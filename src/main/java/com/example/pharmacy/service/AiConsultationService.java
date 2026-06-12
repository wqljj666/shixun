package com.example.pharmacy.service;

import com.example.pharmacy.common.BusinessException;
import com.example.pharmacy.dto.ConsultationRequest;
import com.example.pharmacy.entity.AiConsultationEntity;
import com.example.pharmacy.entity.AiConversationEntity;
import com.example.pharmacy.enums.ConsultationRoleEnum;
import com.example.pharmacy.enums.RiskLevelEnum;
import com.example.pharmacy.repository.AiConsultationRepository;
import com.example.pharmacy.repository.AiConversationRepository;
import com.example.pharmacy.vo.ConsultationMessageVO;
import com.example.pharmacy.vo.ConsultationResponseVO;
import com.example.pharmacy.vo.ConversationDetailVO;
import com.example.pharmacy.vo.ConversationSummaryVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 智能药师连续咨询业务服务。
 *
 * <p>负责创建咨询会话、保存多轮用户/AI 消息、查询历史上下文、识别高风险关键词、
 * 调用大模型服务生成回复，并提供会话历史查询能力。当前课程项目使用默认演示用户。</p>
 */
@Service
public class AiConsultationService {

    private static final Long DEFAULT_USER_ID = 1L;

    private final AiConversationRepository conversationRepository;
    private final AiConsultationRepository consultationRepository;
    private final RiskDetectService riskDetectService;
    private final LargeModelService largeModelService;

    public AiConsultationService(AiConversationRepository conversationRepository,
                                 AiConsultationRepository consultationRepository,
                                 RiskDetectService riskDetectService,
                                 LargeModelService largeModelService) {
        this.conversationRepository = conversationRepository;
        this.consultationRepository = consultationRepository;
        this.riskDetectService = riskDetectService;
        this.largeModelService = largeModelService;
    }

    /**
     * 创建新的咨询会话。
     *
     * @return 新会话详情，包含 conversationId 和空消息列表
     */
    @Transactional
    public ConversationDetailVO createConversation() {
        AiConversationEntity conversation = new AiConversationEntity();
        conversation.setConversationId(generateConversationId());
        conversation.setUserId(DEFAULT_USER_ID);
        conversation.setTitle("新的智能药师咨询");
        conversation.setRiskLevel(RiskLevelEnum.LOW.name());
        conversation.setStatus("ACTIVE");
        conversation.setLastMessage("尚未开始咨询");
        conversation = conversationRepository.save(conversation);
        return toConversationDetail(conversation, List.of());
    }

    /**
     * 按会话 ID 查询完整聊天记录。
     *
     * @param conversationId 会话编号
     * @return 会话详情和全部消息
     * @throws BusinessException 当会话不存在时抛出
     */
    public ConversationDetailVO getConversationDetail(String conversationId) {
        AiConversationEntity conversation = getConversation(conversationId);
        List<AiConsultationEntity> messages = consultationRepository
                .findByConversationIdAndUserIdOrderByCreatedAtAsc(conversationId, DEFAULT_USER_ID);
        return toConversationDetail(conversation, messages);
    }

    /**
     * 查询最近咨询会话列表。
     *
     * @return 按更新时间倒序排列的会话摘要列表
     */
    public List<ConversationSummaryVO> listConversations() {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(DEFAULT_USER_ID).stream()
                .map(this::toConversationSummary)
                .toList();
    }

    /**
     * 发送一条用户消息并生成 AI 回复。
     *
     * @param request 请求对象，包含 conversationId 和 message
     * @return 本轮对话结果，包含用户消息、AI 回复、风险等级和是否建议转人工
     * @throws BusinessException 当消息为空或会话不存在时抛出
     */
    @Transactional
    public ConsultationResponseVO sendMessage(ConsultationRequest request) {
        if (!StringUtils.hasText(request.getMessage())) {
            throw new BusinessException("请输入咨询内容");
        }
        String message = request.getMessage().trim();
        AiConversationEntity conversation = StringUtils.hasText(request.getConversationId())
                ? getConversation(request.getConversationId())
                : getConversation(createConversation().getConversationId());

        AiConsultationEntity userMessage = saveMessage(
                conversation.getConversationId(),
                ConsultationRoleEnum.USER.name(),
                message,
                RiskLevelEnum.LOW,
                List.of(),
                false
        );

        List<AiConsultationEntity> recentContext = consultationRepository
                .findTop10ByConversationIdAndUserIdOrderByCreatedAtDesc(conversation.getConversationId(), DEFAULT_USER_ID)
                .stream()
                .sorted(Comparator.comparing(AiConsultationEntity::getCreatedAt))
                .toList();
        List<String> riskKeywords = riskDetectService.detectHighRiskKeywords(message, recentContext);
        RiskLevelEnum riskLevel = riskKeywords.isEmpty() ? RiskLevelEnum.LOW : RiskLevelEnum.HIGH;
        boolean needHumanTransfer = riskLevel == RiskLevelEnum.HIGH;

        String aiReply = largeModelService.generateAnswer(recentContext, message, riskLevel, riskKeywords);
        saveMessage(
                conversation.getConversationId(),
                ConsultationRoleEnum.AI.name(),
                aiReply,
                riskLevel,
                riskKeywords,
                needHumanTransfer
        );

        updateConversation(conversation, message, riskLevel);
        return toResponse(conversation.getConversationId(), userMessage.getContent(), aiReply, riskLevel, riskKeywords, needHumanTransfer);
    }

    private AiConversationEntity getConversation(String conversationId) {
        return conversationRepository.findByConversationIdAndUserId(conversationId, DEFAULT_USER_ID)
                .orElseThrow(() -> new BusinessException(404, "咨询会话不存在"));
    }

    private AiConsultationEntity saveMessage(String conversationId,
                                             String role,
                                             String content,
                                             RiskLevelEnum riskLevel,
                                             List<String> riskKeywords,
                                             boolean needHumanTransfer) {
        AiConsultationEntity entity = new AiConsultationEntity();
        entity.setConversationId(conversationId);
        entity.setUserId(DEFAULT_USER_ID);
        entity.setRole(role);
        entity.setContent(content);
        entity.setRiskLevel(riskLevel.name());
        entity.setRiskKeywords(String.join("、", riskKeywords));
        entity.setNeedHumanTransfer(needHumanTransfer);
        return consultationRepository.save(entity);
    }

    private void updateConversation(AiConversationEntity conversation, String message, RiskLevelEnum riskLevel) {
        if ("新的智能药师咨询".equals(conversation.getTitle())) {
            conversation.setTitle(abbreviate(message, 28));
        }
        if (riskLevel == RiskLevelEnum.HIGH) {
            conversation.setRiskLevel(RiskLevelEnum.HIGH.name());
        }
        conversation.setLastMessage(abbreviate(message, 80));
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    private ConsultationResponseVO toResponse(String conversationId,
                                              String userMessage,
                                              String aiReply,
                                              RiskLevelEnum riskLevel,
                                              List<String> riskKeywords,
                                              boolean needHumanTransfer) {
        ConsultationResponseVO vo = new ConsultationResponseVO();
        vo.setConversationId(conversationId);
        vo.setUserMessage(userMessage);
        vo.setAiReply(aiReply);
        vo.setRiskLevel(riskLevel.name());
        vo.setRiskLevelText(riskLevel.getDescription());
        vo.setRiskKeywords(riskKeywords);
        vo.setNeedHumanTransfer(needHumanTransfer);
        return vo;
    }

    private ConversationDetailVO toConversationDetail(AiConversationEntity conversation, List<AiConsultationEntity> messages) {
        RiskLevelEnum riskLevel = RiskLevelEnum.valueOf(conversation.getRiskLevel());
        ConversationDetailVO vo = new ConversationDetailVO();
        vo.setConversationId(conversation.getConversationId());
        vo.setTitle(conversation.getTitle());
        vo.setRiskLevel(conversation.getRiskLevel());
        vo.setRiskLevelText(riskLevel.getDescription());
        vo.setMessages(messages.stream().map(this::toMessageVO).toList());
        return vo;
    }

    private ConversationSummaryVO toConversationSummary(AiConversationEntity conversation) {
        RiskLevelEnum riskLevel = RiskLevelEnum.valueOf(conversation.getRiskLevel());
        ConversationSummaryVO vo = new ConversationSummaryVO();
        vo.setConversationId(conversation.getConversationId());
        vo.setTitle(conversation.getTitle());
        vo.setRiskLevel(conversation.getRiskLevel());
        vo.setRiskLevelText(riskLevel.getDescription());
        vo.setLastMessage(conversation.getLastMessage());
        vo.setUpdatedAt(conversation.getUpdatedAt());
        return vo;
    }

    private ConsultationMessageVO toMessageVO(AiConsultationEntity entity) {
        RiskLevelEnum riskLevel = RiskLevelEnum.valueOf(entity.getRiskLevel());
        ConsultationMessageVO vo = new ConsultationMessageVO();
        vo.setRole(entity.getRole());
        vo.setContent(entity.getContent());
        vo.setRiskLevel(entity.getRiskLevel());
        vo.setRiskLevelText(riskLevel.getDescription());
        vo.setRiskKeywords(splitKeywords(entity.getRiskKeywords()));
        vo.setNeedHumanTransfer(entity.getNeedHumanTransfer());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    private List<String> splitKeywords(String riskKeywords) {
        if (!StringUtils.hasText(riskKeywords)) {
            return List.of();
        }
        return Arrays.stream(riskKeywords.split("、"))
                .filter(StringUtils::hasText)
                .toList();
    }

    private String generateConversationId() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "CONV" + time + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 1) + "…";
    }
}
