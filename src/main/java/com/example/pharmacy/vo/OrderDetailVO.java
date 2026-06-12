package com.example.pharmacy.vo;

import com.example.pharmacy.entity.OrderEntity;
import com.example.pharmacy.entity.OrderItemEntity;
import com.example.pharmacy.entity.PaymentRecordEntity;

import java.util.ArrayList;
import java.util.List;

public class OrderDetailVO {

    private OrderEntity order;
    private List<OrderItemEntity> items = new ArrayList<>();
    private List<PaymentRecordEntity> payments = new ArrayList<>();
    private String statusText;

    public OrderEntity getOrder() {
        return order;
    }

    public void setOrder(OrderEntity order) {
        this.order = order;
    }

    public List<OrderItemEntity> getItems() {
        return items;
    }

    public void setItems(List<OrderItemEntity> items) {
        this.items = items;
    }

    public List<PaymentRecordEntity> getPayments() {
        return payments;
    }

    public void setPayments(List<PaymentRecordEntity> payments) {
        this.payments = payments;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }
}
