package com.example.pharmacy.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
/**
 * 咨询风险识别服务。
 *
 * <p>根据课程要求维护高风险关键词列表，用于识别孕妇、儿童、老人、过敏史、处方药、
 * 严重症状等需要人工药师或医生确认的场景。</p>
 */
public class RiskDetectService {

    private static final List<String> HIGH_RISK_KEYWORDS = List.of(
            "孕妇", "儿童", "婴儿", "老人", "青霉素过敏", "过敏史", "处方药", "抗生素",
            "剧烈疼痛", "呼吸困难", "高烧", "慢病", "多药同服", "肝肾功能异常"
    );

    /**
     * 检测用户咨询内容中命中的高风险关键词。
     *
     * @param question 用户输入的症状、用药问题或健康咨询内容
     * @return 命中的高风险关键词列表；未命中时返回空列表
     */
    public List<String> detectHighRiskKeywords(String question) {
        if (question == null || question.isBlank()) {
            return List.of();
        }
        return HIGH_RISK_KEYWORDS.stream()
                .filter(question::contains)
                .toList();
    }
}
