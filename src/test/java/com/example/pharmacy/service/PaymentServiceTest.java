package com.example.pharmacy.service;

import com.example.pharmacy.common.BusinessException;
import com.example.pharmacy.entity.OrderEntity;
import com.example.pharmacy.entity.PaymentRecordEntity;
import com.example.pharmacy.enums.OrderStatusEnum;
import com.example.pharmacy.repository.OrderRepository;
import com.example.pharmacy.repository.PaymentRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRecordRepository paymentRecordRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(orderRepository, paymentRecordRepository);
    }

    /**
     * 场景：模拟支付成功后，订单状态应变为 PAID，并生成支付记录。
     */
    @Test
    void simulatePay_withWaitPayOrder_shouldMarkOrderPaidAndSavePayment() {
        OrderEntity order = new OrderEntity();
        order.setId(100L);
        order.setUserId(CartService.DEFAULT_USER_ID);
        order.setStatus(OrderStatusEnum.WAIT_PAY.name());
        order.setPayAmount(new BigDecimal("59.60"));
        when(orderRepository.findByIdAndUserId(100L, CartService.DEFAULT_USER_ID)).thenReturn(Optional.of(order));

        paymentService.simulatePay(100L);

        assertEquals(OrderStatusEnum.PAID.name(), order.getStatus());
        assertNotNull(order.getPaidAt());
        ArgumentCaptor<PaymentRecordEntity> captor = ArgumentCaptor.forClass(PaymentRecordEntity.class);
        verify(paymentRecordRepository).save(captor.capture());
        assertEquals(100L, captor.getValue().getOrderId());
        assertEquals(new BigDecimal("59.60"), captor.getValue().getAmount());
    }

    /**
     * 场景：非待支付订单不能重复模拟支付。
     */
    @Test
    void simulatePay_withPaidOrder_shouldThrowBusinessException() {
        OrderEntity order = new OrderEntity();
        order.setId(100L);
        order.setUserId(CartService.DEFAULT_USER_ID);
        order.setStatus(OrderStatusEnum.PAID.name());
        when(orderRepository.findByIdAndUserId(100L, CartService.DEFAULT_USER_ID)).thenReturn(Optional.of(order));

        assertThrows(BusinessException.class, () -> paymentService.simulatePay(100L));
    }
}
