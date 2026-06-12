package com.example.pharmacy.service;

import com.example.pharmacy.entity.AiConsultationEntity;
import com.example.pharmacy.enums.ConsultationRoleEnum;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 咨询风险识别服务。
 *
 * <p>根据课程要求维护高风险关键词列表。风险识别不仅查看当前消息，也会结合当前会话的历史上下文，
 * 用于识别孕妇、儿童、老人、过敏史、处方药、严重症状等需要人工药师或医生确认的场景。</p>
 */
@Service
public class RiskDetectService {

    private static final List<String> HIGH_RISK_KEYWORDS = List.of(
            "孕妇", "儿童", "婴儿", "老人", "青霉素过敏", "过敏史", "处方药", "抗生素",
            "剧烈疼痛", "呼吸困难", "高烧", "慢病", "多药同服", "肝肾功能异常"
    );

    /**
     * 检测当前消息和历史上下文中命中的高风险关键词。
     *
     * @param currentMessage 用户当前输入内容
     * @param contextMessages 当前会话最近的历史消息
     * @return 命中的高风险关键词列表；未命中时返回空列表
     */
    public List<String> detectHighRiskKeywords(String currentMessage, List<AiConsultationEntity> contextMessages) {
        StringBuilder context = new StringBuilder();
        if (contextMessages != null) {
            contextMessages.stream()
                    .filter(message -> ConsultationRoleEnum.USER.name().equals(message.getRole()))
                    .forEach(message -> context.append(message.getContent()).append('\n'));
        }
        if (currentMessage != null) {
            context.append(currentMessage);
        }
        String text = context.toString();
        return HIGH_RISK_KEYWORDS.stream()
                .filter(text::contains)
                .toList();
    }
}
