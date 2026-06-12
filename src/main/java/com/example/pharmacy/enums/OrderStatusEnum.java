package com.example.pharmacy.enums;

public enum OrderStatusEnum {
    WAIT_PAY("待支付"),
    PAID("已支付"),
    CANCELED("已取消"),
    FINISHED("已完成");

    private final String description;

    OrderStatusEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
