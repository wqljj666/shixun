package com.example.pharmacy.service;

import com.example.pharmacy.entity.AiConsultationEntity;
import com.example.pharmacy.enums.ConsultationRoleEnum;
import com.example.pharmacy.enums.RiskLevelEnum;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LargeModelServiceTest {

    /**
     * 场景：未配置 API Key 时，LargeModelService 必须使用本地模拟回复，不能访问真实外部大模型。
     */
    @Test
    void generateAnswer_withoutApiKey_shouldUseLocalFallbackAndIncludeDisclaimer() {
        LargeModelService service = new LargeModelService(
                "http://localhost/not-used",
                "",
                "test-model",
                RestClient.builder()
        );

        String answer = service.generateAnswer(
                List.of(message("我嗓子疼，可以吃什么药？")),
                "会有什么风险？",
                RiskLevelEnum.LOW,
                List.of()
        );

        assertTrue(answer.contains(LargeModelService.DISCLAIMER));
    }

    /**
     * 场景：高风险问题应返回转人工方向的安全提示，并包含免责声明。
     */
    @Test
    void generateAnswer_withHighRisk_shouldSuggestHumanTransferAndIncludeDisclaimer() {
        LargeModelService service = new LargeModelService(
                "http://localhost/not-used",
                "",
                "test-model",
                RestClient.builder()
        );

        String answer = service.generateAnswer(
                List.of(message("我嗓子疼，可以吃什么药？")),
                "我有青霉素过敏史",
                RiskLevelEnum.HIGH,
                List.of("青霉素过敏", "过敏史")
        );

        assertTrue(answer.contains("青霉素过敏"));
        assertTrue(answer.contains(LargeModelService.DISCLAIMER));
    }

    private AiConsultationEntity message(String content) {
        AiConsultationEntity entity = new AiConsultationEntity();
        entity.setRole(ConsultationRoleEnum.USER.name());
        entity.setContent(content);
        return entity;
    }
}
