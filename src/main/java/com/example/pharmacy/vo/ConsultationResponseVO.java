package com.example.pharmacy.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ConsultationResponseVO {

    private Long id;
    private String question;
    private String answer;
    private String riskLevel;
    private String riskLevelText;
    private List<String> riskKeywords = new ArrayList<>();
    private Boolean manualTransferSuggested;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getRiskLevelText() {
        return riskLevelText;
    }

    public void setRiskLevelText(String riskLevelText) {
        this.riskLevelText = riskLevelText;
    }

    public List<String> getRiskKeywords() {
        return riskKeywords;
    }

    public void setRiskKeywords(List<String> riskKeywords) {
        this.riskKeywords = riskKeywords;
    }

    public Boolean getManualTransferSuggested() {
        return manualTransferSuggested;
    }

    public void setManualTransferSuggested(Boolean manualTransferSuggested) {
        this.manualTransferSuggested = manualTransferSuggested;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
