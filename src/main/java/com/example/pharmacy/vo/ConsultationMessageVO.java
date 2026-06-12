package com.example.pharmacy.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ConsultationMessageVO {

    private String role;
    private String content;
    private String riskLevel;
    private String riskLevelText;
    private List<String> riskKeywords = new ArrayList<>();
    private Boolean needHumanTransfer;
    private LocalDateTime createdAt;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public Boolean getNeedHumanTransfer() {
        return needHumanTransfer;
    }

    public void setNeedHumanTransfer(Boolean needHumanTransfer) {
        this.needHumanTransfer = needHumanTransfer;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
