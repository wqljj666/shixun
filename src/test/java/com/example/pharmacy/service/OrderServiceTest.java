package com.example.pharmacy.service;

import com.example.pharmacy.common.BusinessException;
import com.example.pharmacy.dto.CreateOrderRequest;
import com.example.pharmacy.entity.CartItemEntity;
import com.example.pharmacy.entity.MedicineEntity;
import com.example.pharmacy.entity.OrderEntity;
import com.example.pharmacy.entity.OrderItemEntity;
import com.example.pharmacy.enums.OrderStatusEnum;
import com.example.pharmacy.repository.CartRepository;
import com.example.pharmacy.repository.MedicineRepository;
import com.example.pharmacy.repository.OrderItemRepository;
import com.example.pharmacy.repository.OrderRepository;
import com.example.pharmacy.repository.PaymentRecordRepository;
import com.example.pharmacy.vo.CartSummaryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private MedicineRepository medicineRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private PaymentRecordRepository paymentRecordRepository;
    @Mock
    private CartService cartService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(cartRepository, medicineRepository, orderRepository,
                orderItemRepository, paymentRecordRepository, cartService);
    }

    /**
     * 场景：创建订单成功后，订单状态应为 WAIT_PAY，订单金额应等于药品单价乘数量。
     */
    @Test
    void createOrder_shouldCreateWaitPayOrderAndCalculateAmount() {
        CartItemEntity cartItem = cartItem(1L, 2);
        MedicineEntity medicine = medicine(1L, false, 10);
        when(cartRepository.findByUserIdOrderByUpdatedAtDesc(CartService.DEFAULT_USER_ID)).thenReturn(List.of(cartItem));
        when(medicineRepository.findAllById(List.of(1L))).thenReturn(List.of(medicine));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });

        OrderEntity order = orderService.createOrder(request());

        assertEquals(OrderStatusEnum.WAIT_PAY.name(), order.getStatus());
        assertEquals(new BigDecimal("59.60"), order.getPayAmount());
        assertEquals(8, medicine.getStock());
        verify(orderItemRepository).save(any(OrderItemEntity.class));
        verify(cartRepository).deleteByUserId(CartService.DEFAULT_USER_ID);
    }

    /**
     * 场景：购买数量大于库存时，创建订单应失败。
     */
    @Test
    void createOrder_whenStockNotEnough_shouldThrowBusinessException() {
        when(cartRepository.findByUserIdOrderByUpdatedAtDesc(CartService.DEFAULT_USER_ID)).thenReturn(List.of(cartItem(1L, 20)));
        when(medicineRepository.findAllById(List.of(1L))).thenReturn(List.of(medicine(1L, false, 3)));

        assertThrows(BusinessException.class, () -> orderService.createOrder(request()));
    }

    /**
     * 场景：购物车中包含处方药时，不允许直接创建订单。
     */
    @Test
    void createOrder_withPrescriptionMedicine_shouldThrowBusinessException() {
        when(cartRepository.findByUserIdOrderByUpdatedAtDesc(CartService.DEFAULT_USER_ID)).thenReturn(List.of(cartItem(8L, 1)));
        when(medicineRepository.findAllById(List.of(8L))).thenReturn(List.of(medicine(8L, true, 10)));

        assertThrows(BusinessException.class, () -> orderService.createOrder(request()));
    }

    /**
     * 场景：取消 WAIT_PAY 订单应成功，并将库存恢复。
     */
    @Test
    void cancelOrder_withWaitPayOrder_shouldCancelAndRestoreStock() {
        OrderEntity order = order(100L, OrderStatusEnum.WAIT_PAY);
        OrderItemEntity item = orderItem(1L, 2);
        MedicineEntity medicine = medicine(1L, false, 8);
        when(orderRepository.findByIdAndUserId(100L, CartService.DEFAULT_USER_ID)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of(item));
        when(medicineRepository.findAllById(List.of(1L))).thenReturn(List.of(medicine));

        orderService.cancelOrder(100L);

        assertEquals(OrderStatusEnum.CANCELED.name(), order.getStatus());
        assertEquals(10, medicine.getStock());
        verify(orderRepository).save(order);
    }

    /**
     * 场景：已支付订单不能取消。
     */
    @Test
    void cancelOrder_withPaidOrder_shouldThrowBusinessException() {
        when(orderRepository.findByIdAndUserId(100L, CartService.DEFAULT_USER_ID))
                .thenReturn(Optional.of(order(100L, OrderStatusEnum.PAID)));

        assertThrows(BusinessException.class, () -> orderService.cancelOrder(100L));
    }

    private CreateOrderRequest request() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setReceiverName("测试用户");
        request.setReceiverPhone("13800000000");
        request.setReceiverAddress("测试地址");
        request.setDeliveryMethod("标准配送");
        return request;
    }

    private CartItemEntity cartItem(Long medicineId, int quantity) {
        CartItemEntity item = new CartItemEntity();
        item.setMedicineId(medicineId);
        item.setQuantity(quantity);
        item.setUserId(CartService.DEFAULT_USER_ID);
        return item;
    }

    private MedicineEntity medicine(Long id, boolean prescriptionRequired, int stock) {
        MedicineEntity medicine = new MedicineEntity();
        medicine.setId(id);
        medicine.setName("测试药品");
        medicine.setSpecification("10片/盒");
        medicine.setManufacturer("测试药业");
        medicine.setImageUrl("/img/medicine-default.svg");
        medicine.setPrice(new BigDecimal("29.80"));
        medicine.setStock(stock);
        medicine.setStatus(1);
        medicine.setPrescriptionRequired(prescriptionRequired);
        return medicine;
    }

    private OrderEntity order(Long id, OrderStatusEnum status) {
        OrderEntity order = new OrderEntity();
        order.setId(id);
        order.setUserId(CartService.DEFAULT_USER_ID);
        order.setStatus(status.name());
        return order;
    }

    private OrderItemEntity orderItem(Long medicineId, int quantity) {
        OrderItemEntity item = new OrderItemEntity();
        item.setMedicineId(medicineId);
        item.setQuantity(quantity);
        return item;
    }
}
