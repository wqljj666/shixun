package com.example.pharmacy.service;

import com.example.pharmacy.entity.AiConsultationEntity;
import com.example.pharmacy.enums.ConsultationRoleEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskDetectServiceTest {

    private final RiskDetectService riskDetectService = new RiskDetectService();

    /**
     * 场景：当前消息包含青霉素过敏史时，应识别为高风险关键词。
     */
    @Test
    void detectHighRiskKeywords_withAllergyMessage_shouldReturnKeywords() {
        List<String> keywords = riskDetectService.detectHighRiskKeywords("我有青霉素过敏史", List.of());

        assertTrue(keywords.contains("青霉素过敏"));
        assertTrue(keywords.contains("过敏史"));
    }

    /**
     * 场景：风险识别应结合用户历史消息，但不应把 AI 风险说明误判为用户风险。
     */
    @Test
    void detectHighRiskKeywords_shouldUseUserContextAndIgnoreAiMessages() {
        AiConsultationEntity userHistory = message(ConsultationRoleEnum.USER.name(), "我嗓子疼，可以吃什么药？");
        AiConsultationEntity aiHistory = message(ConsultationRoleEnum.AI.name(), "如果有高烧、呼吸困难，需要就医。");

        List<String> keywords = riskDetectService.detectHighRiskKeywords("会有什么风险？", List.of(userHistory, aiHistory));

        assertTrue(keywords.isEmpty());
    }

    /**
     * 场景：高风险关键词出现在用户历史上下文中时，也应被识别。
     */
    @Test
    void detectHighRiskKeywords_withHighRiskUserHistory_shouldReturnKeywords() {
        AiConsultationEntity userHistory = message(ConsultationRoleEnum.USER.name(), "儿童高烧，可以吃什么？");

        List<String> keywords = riskDetectService.detectHighRiskKeywords("会有什么风险？", List.of(userHistory));

        assertFalse(keywords.isEmpty());
        assertTrue(keywords.contains("儿童"));
        assertTrue(keywords.contains("高烧"));
    }

    private AiConsultationEntity message(String role, String content) {
        AiConsultationEntity entity = new AiConsultationEntity();
        entity.setRole(role);
        entity.setContent(content);
        return entity;
    }
}
