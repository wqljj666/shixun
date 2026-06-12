package com.example.pharmacy.enums;

public enum RiskLevelEnum {
    LOW("低风险"),
    HIGH("高风险");

    private final String description;

    RiskLevelEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
