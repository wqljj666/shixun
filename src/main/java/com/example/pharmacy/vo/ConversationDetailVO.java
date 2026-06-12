package com.example.pharmacy.vo;

import java.util.ArrayList;
import java.util.List;

public class ConversationDetailVO {

    private String conversationId;
    private String title;
    private String riskLevel;
    private String riskLevelText;
    private List<ConsultationMessageVO> messages = new ArrayList<>();

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

    public List<ConsultationMessageVO> getMessages() {
        return messages;
    }

    public void setMessages(List<ConsultationMessageVO> messages) {
        this.messages = messages;
    }
}
