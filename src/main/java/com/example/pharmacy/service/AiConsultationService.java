package com.example.pharmacy.service;

import com.example.pharmacy.common.BusinessException;
import com.example.pharmacy.dto.ConsultationRequest;
import com.example.pharmacy.entity.AiConsultationEntity;
import com.example.pharmacy.enums.RiskLevelEnum;
import com.example.pharmacy.repository.AiConsultationRepository;
import com.example.pharmacy.vo.ConsultationResponseVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Service
/**
 * 智能药师咨询业务服务。
 *
 * <p>负责接收用户咨询、识别高风险关键词、调用大模型服务生成回复，并保存咨询记录。
 * 课程项目当前使用默认演示用户。所有回复均由 LargeModelService 统一追加免责声明。</p>
 */
public class AiConsultationService {

    private static final Long DEFAULT_USER_ID = 1L;

    private final AiConsultationRepository consultationRepository;
    private final RiskDetectService riskDetectService;
    private final LargeModelService largeModelService;

    public AiConsultationService(AiConsultationRepository consultationRepository,
                                 RiskDetectService riskDetectService,
                                 LargeModelService largeModelService) {
        this.consultationRepository = consultationRepository;
        this.riskDetectService = riskDetectService;
        this.largeModelService = largeModelService;
    }

    @Transactional
    /**
     * 提交一次智能药师咨询。
     *
     * @param request 咨询请求，包含用户输入的问题
     * @return 保存后的咨询回复 VO，包含风险等级、命中关键词和是否建议转人工
     * @throws BusinessException 当咨询内容为空时抛出
     */
    public ConsultationResponseVO consult(ConsultationRequest request) {
        if (!StringUtils.hasText(request.getQuestion())) {
            throw new BusinessException("请输入咨询内容");
        }
        String question = request.getQuestion().trim();
        List<String> riskKeywords = riskDetectService.detectHighRiskKeywords(question);
        RiskLevelEnum riskLevel = riskKeywords.isEmpty() ? RiskLevelEnum.LOW : RiskLevelEnum.HIGH;
        String answer = largeModelService.generateAnswer(question, riskLevel, riskKeywords);

        AiConsultationEntity entity = new AiConsultationEntity();
        entity.setUserId(DEFAULT_USER_ID);
        entity.setQuestion(question);
        entity.setAnswer(answer);
        entity.setRiskLevel(riskLevel.name());
        entity.setRiskKeywords(String.join("、", riskKeywords));
        entity.setManualTransferSuggested(riskLevel == RiskLevelEnum.HIGH);
        entity = consultationRepository.save(entity);
        return toVO(entity);
    }

    /**
     * 查询默认演示用户的历史咨询记录。
     *
     * @return 按咨询时间倒序排列的历史记录
     */
    public List<ConsultationResponseVO> history() {
        return consultationRepository.findByUserIdOrderByCreatedAtDesc(DEFAULT_USER_ID).stream()
                .map(this::toVO)
                .toList();
    }

    private ConsultationResponseVO toVO(AiConsultationEntity entity) {
        RiskLevelEnum riskLevel = RiskLevelEnum.valueOf(entity.getRiskLevel());
        ConsultationResponseVO vo = new ConsultationResponseVO();
        vo.setId(entity.getId());
        vo.setQuestion(entity.getQuestion());
        vo.setAnswer(entity.getAnswer());
        vo.setRiskLevel(entity.getRiskLevel());
        vo.setRiskLevelText(riskLevel.getDescription());
        vo.setRiskKeywords(splitKeywords(entity.getRiskKeywords()));
        vo.setManualTransferSuggested(entity.getManualTransferSuggested());
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
}
