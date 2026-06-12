package com.example.pharmacy.service;

import com.example.pharmacy.common.BusinessException;
import com.example.pharmacy.entity.OrderEntity;
import com.example.pharmacy.entity.PaymentRecordEntity;
import com.example.pharmacy.enums.OrderStatusEnum;
import com.example.pharmacy.repository.OrderRepository;
import com.example.pharmacy.repository.PaymentRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
/**
 * 模拟支付业务服务。
 *
 * <p>负责将待支付订单模拟支付为已支付，并生成支付记录。当前模块用于课程演示，不对接真实
 * 第三方支付平台；真实 API Key 或支付凭证不得写入代码。</p>
 */
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRecordRepository paymentRecordRepository;

    public PaymentService(OrderRepository orderRepository, PaymentRecordRepository paymentRecordRepository) {
        this.orderRepository = orderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
    }

    @Transactional
    /**
     * 模拟支付指定订单。
     *
     * @param orderId 订单 ID
     * @throws BusinessException 当订单不存在或订单不是待支付状态时抛出
     */
    public void simulatePay(Long orderId) {
        OrderEntity order = orderRepository.findByIdAndUserId(orderId, CartService.DEFAULT_USER_ID)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if (!OrderStatusEnum.WAIT_PAY.name().equals(order.getStatus())) {
            throw new BusinessException("只有待支付订单可以支付");
        }

        order.setStatus(OrderStatusEnum.PAID.name());
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        PaymentRecordEntity payment = new PaymentRecordEntity();
        payment.setOrderId(order.getId());
        payment.setPaymentNo(generatePaymentNo());
        payment.setAmount(order.getPayAmount());
        payment.setStatus("SUCCESS");
        payment.setPaymentMethod("模拟支付");
        paymentRecordRepository.save(payment);
    }

    private String generatePaymentNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "PAY" + time + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
