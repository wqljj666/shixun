package com.example.pharmacy.vo;

import java.time.LocalDateTime;

public class ConversationSummaryVO {

    private String conversationId;
    private String title;
    private String riskLevel;
    private String riskLevelText;
    private String lastMessage;
    private LocalDateTime updatedAt;

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
