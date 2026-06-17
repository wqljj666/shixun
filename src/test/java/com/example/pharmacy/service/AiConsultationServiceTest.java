package com.example.pharmacy.service;

import com.example.pharmacy.common.BusinessException;
import com.example.pharmacy.dto.ConsultationRequest;
import com.example.pharmacy.entity.AiConsultationEntity;
import com.example.pharmacy.entity.AiConversationEntity;
import com.example.pharmacy.enums.ConsultationRoleEnum;
import com.example.pharmacy.enums.RiskLevelEnum;
import com.example.pharmacy.repository.AiConsultationRepository;
import com.example.pharmacy.repository.AiConversationRepository;
import com.example.pharmacy.vo.ConsultationResponseVO;
import com.example.pharmacy.vo.ConversationDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiConsultationServiceTest {

    @Mock
    private AiConversationRepository conversationRepository;
    @Mock
    private AiConsultationRepository consultationRepository;
    @Mock
    private RiskDetectService riskDetectService;
    @Mock
    private LargeModelService largeModelService;

    private AiConsultationService consultationService;

    @BeforeEach
    void setUp() {
        consultationService = new AiConsultationService(
                conversationRepository,
                consultationRepository,
                riskDetectService,
                largeModelService
        );
    }

    /**
     * 场景：普通咨询应保存用户消息和 AI 回复，并返回 LOW 风险结果。
     */
    @Test
    void sendMessage_withNormalQuestion_shouldSaveTwoMessagesAndReturnLowRisk() {
        AiConversationEntity conversation = conversation("CONV001", RiskLevelEnum.LOW);
        ConsultationRequest request = request("CONV001", "我嗓子疼，可以吃什么药？");
        AiConsultationEntity context = message(ConsultationRoleEnum.USER.name(), request.getMessage(), RiskLevelEnum.LOW, false);

        when(conversationRepository.findByConversationIdAndUserId("CONV001", CartService.DEFAULT_USER_ID))
                .thenReturn(Optional.of(conversation));
        when(consultationRepository.save(any(AiConsultationEntity.class))).thenAnswer(invocation -> {
            AiConsultationEntity entity = invocation.getArgument(0);
            entity.setCreatedAt(LocalDateTime.now());
            return entity;
        });
        when(consultationRepository.findTop10ByConversationIdAndUserIdOrderByCreatedAtDesc("CONV001", CartService.DEFAULT_USER_ID))
                .thenReturn(List.of(context));
        when(riskDetectService.detectHighRiskKeywords(request.getMessage(), List.of(context))).thenReturn(List.of());
        when(largeModelService.generateAnswer(List.of(context), request.getMessage(), RiskLevelEnum.LOW, List.of()))
                .thenReturn("普通回复。" + LargeModelService.DISCLAIMER);

        ConsultationResponseVO response = consultationService.sendMessage(request);

        assertEquals("CONV001", response.getConversationId());
        assertEquals(RiskLevelEnum.LOW.name(), response.getRiskLevel());
        assertFalse(response.getNeedHumanTransfer());
        verify(consultationRepository, times(2)).save(any(AiConsultationEntity.class));
        verify(conversationRepository).save(conversation);
    }

    /**
     * 场景：多轮咨询中补充青霉素过敏史时，应返回 HIGH 风险并建议转人工。
     */
    @Test
    void sendMessage_withHighRiskContext_shouldReturnHighRiskAndNeedHumanTransfer() {
        AiConversationEntity conversation = conversation("CONV002", RiskLevelEnum.LOW);
        ConsultationRequest request = request("CONV002", "我有青霉素过敏史");
        AiConsultationEntity previous = message(ConsultationRoleEnum.USER.name(), "我嗓子疼，可以吃什么药？", RiskLevelEnum.LOW, false);

        when(conversationRepository.findByConversationIdAndUserId("CONV002", CartService.DEFAULT_USER_ID))
                .thenReturn(Optional.of(conversation));
        when(consultationRepository.save(any(AiConsultationEntity.class))).thenAnswer(invocation -> {
            AiConsultationEntity entity = invocation.getArgument(0);
            entity.setCreatedAt(LocalDateTime.now());
            return entity;
        });
        when(consultationRepository.findTop10ByConversationIdAndUserIdOrderByCreatedAtDesc("CONV002", CartService.DEFAULT_USER_ID))
                .thenReturn(List.of(previous));
        when(riskDetectService.detectHighRiskKeywords(request.getMessage(), List.of(previous)))
                .thenReturn(List.of("青霉素过敏", "过敏史"));
        when(largeModelService.generateAnswer(List.of(previous), request.getMessage(), RiskLevelEnum.HIGH, List.of("青霉素过敏", "过敏史")))
                .thenReturn("高风险回复。" + LargeModelService.DISCLAIMER);

        ConsultationResponseVO response = consultationService.sendMessage(request);

        assertEquals(RiskLevelEnum.HIGH.name(), response.getRiskLevel());
        assertTrue(response.getNeedHumanTransfer());
        assertTrue(response.getRiskKeywords().contains("青霉素过敏"));
        assertEquals(RiskLevelEnum.HIGH.name(), conversation.getRiskLevel());
    }

    /**
     * 场景：查询会话详情时，应返回该 conversationId 下的完整消息历史。
     */
    @Test
    void getConversationDetail_shouldReturnAllMessages() {
        AiConversationEntity conversation = conversation("CONV003", RiskLevelEnum.LOW);
        when(conversationRepository.findByConversationIdAndUserId("CONV003", CartService.DEFAULT_USER_ID))
                .thenReturn(Optional.of(conversation));
        when(consultationRepository.findByConversationIdAndUserIdOrderByCreatedAtAsc("CONV003", CartService.DEFAULT_USER_ID))
                .thenReturn(List.of(
                        message(ConsultationRoleEnum.USER.name(), "第一句", RiskLevelEnum.LOW, false),
                        message(ConsultationRoleEnum.AI.name(), "第一句回复", RiskLevelEnum.LOW, false)
                ));

        ConversationDetailVO detail = consultationService.getConversationDetail("CONV003");

        assertEquals("CONV003", detail.getConversationId());
        assertEquals(2, detail.getMessages().size());
        assertEquals(ConsultationRoleEnum.USER.name(), detail.getMessages().get(0).getRole());
    }

    /**
     * 场景：空消息不允许发送。
     */
    @Test
    void sendMessage_withBlankMessage_shouldThrowBusinessException() {
        assertThrows(BusinessException.class, () -> consultationService.sendMessage(request("CONV004", " ")));
    }

    private ConsultationRequest request(String conversationId, String message) {
        ConsultationRequest request = new ConsultationRequest();
        request.setConversationId(conversationId);
        request.setMessage(message);
        return request;
    }

    private AiConversationEntity conversation(String conversationId, RiskLevelEnum riskLevel) {
        AiConversationEntity conversation = new AiConversationEntity();
        conversation.setConversationId(conversationId);
        conversation.setUserId(CartService.DEFAULT_USER_ID);
        conversation.setTitle("测试会话");
        conversation.setRiskLevel(riskLevel.name());
        conversation.setStatus("ACTIVE");
        conversation.setLastMessage("测试");
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        return conversation;
    }

    private AiConsultationEntity message(String role, String content, RiskLevelEnum riskLevel, boolean needHumanTransfer) {
        AiConsultationEntity entity = new AiConsultationEntity();
        entity.setConversationId("CONV");
        entity.setUserId(CartService.DEFAULT_USER_ID);
        entity.setRole(role);
        entity.setContent(content);
        entity.setRiskLevel(riskLevel.name());
        entity.setRiskKeywords("");
        entity.setNeedHumanTransfer(needHumanTransfer);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}
