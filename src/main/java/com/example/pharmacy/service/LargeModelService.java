package com.example.pharmacy.service;

import com.example.pharmacy.entity.AiConsultationEntity;
import com.example.pharmacy.enums.ConsultationRoleEnum;
import com.example.pharmacy.enums.RiskLevelEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 大模型调用服务。
 *
 * <p>从配置读取 API 地址、API Key 和模型名称，按 OpenAI Chat Completions 兼容格式调用模型。
 * 调用时会传入 system prompt 和最近多轮上下文。如果未配置 API Key 或调用失败，返回本地兜底回复，
 * 保证课程项目在离线或未配置密钥时仍可运行。所有回复都会追加合规免责声明。</p>
 */
@Service
public class LargeModelService {

    public static final String DISCLAIMER = "AI 回答仅供参考，不替代医生诊断或执业药师审核。";
    private static final Logger log = LoggerFactory.getLogger(LargeModelService.class);
    private static final String SYSTEM_PROMPT = """
            你是线上购药系统中的 AI 药师咨询助手，只能提供一般性用药知识和风险提示，不能进行疾病诊断，不能开具处方，不能替代医生或执业药师。对于孕妇、儿童、老人、慢病、多药同服、过敏史、处方药、严重症状等高风险情况，应建议用户咨询执业药师或医生。回答必须包含“AI 回答仅供参考，不替代医生诊断或执业药师审核。”
            """;

    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final RestClient restClient;

    public LargeModelService(@Value("${llm.api-url}") String apiUrl,
                             @Value("${llm.api-key:}") String apiKey,
                             @Value("${llm.model:deepseek-v4-flash}") String model,
                             RestClient.Builder restClientBuilder) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = restClientBuilder.build();
    }

    /**
     * 基于多轮上下文生成智能药师回复。
     *
     * @param contextMessages 最近多轮上下文，包含用户消息和 AI 消息
     * @param currentMessage 用户当前消息
     * @param riskLevel 风险等级；高风险时不直接给出具体用药结论
     * @param riskKeywords 命中的高风险关键词
     * @return 大模型回复或本地兜底回复，均包含免责声明
     */
    public String generateAnswer(List<AiConsultationEntity> contextMessages,
                                 String currentMessage,
                                 RiskLevelEnum riskLevel,
                                 List<String> riskKeywords) {
        if (riskLevel == RiskLevelEnum.HIGH) {
            return withDisclaimer("感谢你补充信息。结合本次对话上下文，你的咨询命中了高风险因素："
                    + String.join("、", riskKeywords)
                    + "。该场景不适合直接给出具体用药结论，建议转人工药师或医生进一步确认，尤其不要自行使用处方药或抗生素。");
        }
        if (!StringUtils.hasText(apiKey)) {
            return localFallback(contextMessages, currentMessage, riskLevel, riskKeywords);
        }
        try {
            String answer = callLargeModel(contextMessages);
            if (StringUtils.hasText(answer)) {
                return withDisclaimer(answer);
            }
        } catch (Exception ex) {
            log.warn("Large model call failed, fallback answer will be used", ex);
        }
        return localFallback(contextMessages, currentMessage, riskLevel, riskKeywords);
    }

    /**
     * 调用 OpenAI 兼容的大模型接口。
     *
     * @param contextMessages 最近多轮上下文
     * @return 模型返回的文本；接口结构异常时返回 null
     */
    @SuppressWarnings("unchecked")
    private String callLargeModel(List<AiConsultationEntity> contextMessages) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        for (AiConsultationEntity message : contextMessages) {
            String role = ConsultationRoleEnum.USER.name().equals(message.getRole()) ? "user" : "assistant";
            messages.add(Map.of("role", role, "content", message.getContent()));
        }
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", messages,
                "temperature", 0.2
        );
        Map<String, Object> response = restClient.post()
                .uri(apiUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(requestBody)
                .retrieve()
                .body(Map.class);
        if (response == null) {
            return null;
        }
        Object choicesValue = response.get("choices");
        if (!(choicesValue instanceof List<?> choices) || choices.isEmpty()) {
            return null;
        }
        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choiceMap)) {
            return null;
        }
        Object messageValue = choiceMap.get("message");
        if (!(messageValue instanceof Map<?, ?> messageMap)) {
            return null;
        }
        Object content = messageMap.get("content");
        return content == null ? null : content.toString();
    }

    /**
     * 生成支持多轮上下文的本地兜底回复。
     *
     * @param contextMessages 最近多轮上下文
     * @param currentMessage 用户当前消息
     * @param riskLevel 风险等级
     * @param riskKeywords 命中的风险关键词
     * @return 不依赖外部 API 的安全提示文本
     */
    private String localFallback(List<AiConsultationEntity> contextMessages,
                                 String currentMessage,
                                 RiskLevelEnum riskLevel,
                                 List<String> riskKeywords) {
        String contextText = buildContextText(contextMessages);
        if (riskLevel == RiskLevelEnum.HIGH) {
            return withDisclaimer("结合本次连续咨询，你之前提到的症状或用药问题，以及当前补充的信息，已命中高风险因素："
                    + String.join("、", riskKeywords)
                    + "。建议不要自行判断具体用药，尤其涉及抗生素、处方药或过敏史时，应转人工药师或医生确认。");
        }
        if (contextText.contains("嗓子疼") || contextText.contains("咽痛")) {
            return withDisclaimer("结合你前面提到的嗓子疼，本次咨询可以先关注是否伴随发热、咳嗽、吞咽困难、过敏史或正在服用其他药物。一般购药前建议查看药品说明书中的适应症、禁忌和注意事项；如果症状持续加重或出现高烧、呼吸困难等情况，应及时就医。");
        }
        return withDisclaimer("我已结合本次会话上下文收到你的咨询。建议先核对药品说明书中的适应症、用法用量、禁忌和注意事项；如正在使用其他药物，或症状持续、加重，应及时咨询执业药师或医生。你可以继续补充年龄、既往过敏情况、正在服用的药物和症状持续时间。");
    }

    private String buildContextText(List<AiConsultationEntity> contextMessages) {
        StringBuilder builder = new StringBuilder();
        for (AiConsultationEntity message : contextMessages) {
            builder.append(message.getContent()).append('\n');
        }
        return builder.toString();
    }

    /**
     * 确保回复包含统一免责声明。
     *
     * @param answer 原始回复
     * @return 包含免责声明的回复文本
     */
    private String withDisclaimer(String answer) {
        if (answer.contains(DISCLAIMER)) {
            return answer;
        }
        return answer + "\n\n" + DISCLAIMER;
    }
}
