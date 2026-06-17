package com.example.pharmacy.controller;

import com.example.pharmacy.dto.ConsultationRequest;
import com.example.pharmacy.enums.RiskLevelEnum;
import com.example.pharmacy.service.AiConsultationService;
import com.example.pharmacy.service.LargeModelService;
import com.example.pharmacy.vo.ConsultationResponseVO;
import com.example.pharmacy.vo.ConversationDetailVO;
import com.example.pharmacy.vo.ConversationSummaryVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.AbstractView;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class AiConsultationControllerTest {

    @Mock
    private AiConsultationService consultationService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AiConsultationController(consultationService))
                .setViewResolvers(new NoOpViewResolver())
                .build();
    }

    /**
     * 场景：进入智能药师页面时，如果没有 conversationId，应创建新会话并返回页面模型。
     */
    @Test
    void index_withoutConversationId_shouldCreateConversation() throws Exception {
        when(consultationService.createConversation()).thenReturn(conversationDetail("CONV001"));
        when(consultationService.listConversations()).thenReturn(List.of(conversationSummary("CONV001")));

        mockMvc.perform(get("/ai-consult"))
                .andExpect(status().isOk())
                .andExpect(view().name("ai-consult/index"))
                .andExpect(model().attributeExists("conversation"))
                .andExpect(model().attributeExists("conversations"));
    }

    /**
     * 场景：发送高风险咨询消息时，JSON 响应应包含 HIGH 和 needHumanTransfer=true。
     */
    @Test
    void send_withHighRiskMessage_shouldReturnJsonResponse() throws Exception {
        ConsultationResponseVO response = new ConsultationResponseVO();
        response.setConversationId("CONV001");
        response.setUserMessage("我有青霉素过敏史");
        response.setAiReply("高风险回复。" + LargeModelService.DISCLAIMER);
        response.setRiskLevel(RiskLevelEnum.HIGH.name());
        response.setRiskLevelText(RiskLevelEnum.HIGH.getDescription());
        response.setRiskKeywords(List.of("青霉素过敏", "过敏史"));
        response.setNeedHumanTransfer(true);
        when(consultationService.sendMessage(any(ConsultationRequest.class))).thenReturn(response);

        ConsultationRequest request = new ConsultationRequest();
        request.setConversationId("CONV001");
        request.setMessage("我有青霉素过敏史");

        mockMvc.perform(post("/ai-consult/send")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId", is("CONV001")))
                .andExpect(jsonPath("$.riskLevel", is("HIGH")))
                .andExpect(jsonPath("$.needHumanTransfer", is(true)));
    }

    /**
     * 场景：按 conversationId 查询历史会话，应返回完整会话 JSON。
     */
    @Test
    void conversation_shouldReturnConversationDetailJson() throws Exception {
        when(consultationService.getConversationDetail("CONV001")).thenReturn(conversationDetail("CONV001"));

        mockMvc.perform(get("/ai-consult/conversations/CONV001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId", is("CONV001")));
    }

    private ConversationDetailVO conversationDetail(String conversationId) {
        ConversationDetailVO detail = new ConversationDetailVO();
        detail.setConversationId(conversationId);
        detail.setTitle("测试会话");
        detail.setRiskLevel(RiskLevelEnum.LOW.name());
        detail.setRiskLevelText(RiskLevelEnum.LOW.getDescription());
        return detail;
    }

    private ConversationSummaryVO conversationSummary(String conversationId) {
        ConversationSummaryVO summary = new ConversationSummaryVO();
        summary.setConversationId(conversationId);
        summary.setTitle("测试会话");
        summary.setRiskLevel(RiskLevelEnum.LOW.name());
        summary.setRiskLevelText(RiskLevelEnum.LOW.getDescription());
        summary.setLastMessage("测试消息");
        return summary;
    }

    private static class NoOpViewResolver implements ViewResolver {
        @Override
        public View resolveViewName(String viewName, Locale locale) {
            return new AbstractView() {
                @Override
                protected void renderMergedOutputModel(Map<String, Object> model,
                                                       HttpServletRequest request,
                                                       HttpServletResponse response) {
                    response.setStatus(HttpStatus.OK.value());
                }
            };
        }
    }
}
