package com.example.pharmacy.service;

import com.example.pharmacy.enums.RiskLevelEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
/**
 * 大模型调用服务。
 *
 * <p>从配置读取 API 地址、API Key 和模型名称，按 OpenAI Chat Completions 兼容格式调用模型。
 * 如果未配置 API Key 或调用失败，返回本地兜底回复，保证课程项目在离线或未配置密钥时仍可运行。
 * 所有回复都会追加合规免责声明。</p>
 */
public class LargeModelService {

    public static final String DISCLAIMER = "AI 回答仅供参考，不替代医生诊断或执业药师审核。";
    private static final Logger log = LoggerFactory.getLogger(LargeModelService.class);
    private static final String SYSTEM_PROMPT = """
            你是线上购药系统中的 AI 药师咨询助手，只能提供一般性用药知识和风险提示，不能进行疾病诊断，不能开具处方，不能替代医生或执业药师。对于孕妇、儿童、老人、慢病、多药同服、过敏史、处方药、严重症状等高风险情况，应建议用户咨询执业药师或医生。回答必须包含“仅供参考，不替代医生诊断或执业药师审核”。
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
     * 生成智能药师回复。
     *
     * @param question 用户咨询内容
     * @param riskLevel 风险等级；高风险时不直接给出具体用药结论
     * @param riskKeywords 命中的高风险关键词
     * @return 大模型回复或本地兜底回复，均包含免责声明
     */
    public String generateAnswer(String question, RiskLevelEnum riskLevel, List<String> riskKeywords) {
        if (riskLevel == RiskLevelEnum.HIGH) {
            return withDisclaimer("你的问题涉及高风险因素：" + String.join("、", riskKeywords)
                    + "。为避免误用药或延误治疗，建议先联系人工药师或医生进一步确认。本系统不能直接给出具体用药结论，也不能开具处方。");
        }
        if (!StringUtils.hasText(apiKey)) {
            return localFallback(question);
        }
        try {
            String answer = callLargeModel(question);
            if (StringUtils.hasText(answer)) {
                return withDisclaimer(answer);
            }
        } catch (Exception ex) {
            log.warn("Large model call failed, fallback answer will be used", ex);
        }
        return localFallback(question);
    }

    @SuppressWarnings("unchecked")
    /**
     * 调用 OpenAI 兼容的大模型接口。
     *
     * @param question 用户咨询内容
     * @return 模型返回的文本；接口结构异常时返回 null
     */
    private String callLargeModel(String question) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", question)
                ),
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
     * 生成本地兜底回复。
     *
     * @param question 用户咨询内容，当前仅用于保留方法签名和后续扩展
     * @return 不依赖外部 API 的安全提示文本
     */
    private String localFallback(String question) {
        return withDisclaimer("我已收到你的咨询。建议先核对药品说明书中的适应症、用法用量、禁忌和注意事项；如正在使用其他药物，或症状持续、加重，应及时咨询执业药师或医生。你可以补充年龄、既往过敏情况、正在服用的药物和症状持续时间，以便获得更有针对性的风险提示。");
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
