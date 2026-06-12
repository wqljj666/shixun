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
import com.example.pharmacy.vo.OrderDetailVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
/**
 * 订单业务服务。
 *
 * <p>负责订单确认预览、创建订单、订单列表/详情查询和取消订单。创建订单时会检查购物车、
 * 药品状态、处方药限制和库存，随后生成订单主表与明细快照，并扣减库存。取消订单仅允许
 * 待支付状态，并会恢复库存。</p>
 */
public class OrderService {

    private static final BigDecimal DELIVERY_FEE = BigDecimal.ZERO;

    private final CartRepository cartRepository;
    private final MedicineRepository medicineRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final CartService cartService;

    public OrderService(CartRepository cartRepository,
                        MedicineRepository medicineRepository,
                        OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        PaymentRecordRepository paymentRecordRepository,
                        CartService cartService) {
        this.cartRepository = cartRepository;
        this.medicineRepository = medicineRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.cartService = cartService;
    }

    /**
     * 预览订单确认页数据。
     *
     * @return 当前购物车汇总信息
     * @throws BusinessException 当购物车为空时抛出
     */
    public CartSummaryVO previewOrder() {
        CartSummaryVO summary = cartService.getCartSummary();
        if (summary.getItems().isEmpty()) {
            throw new BusinessException("购物车为空，请先选择药品");
        }
        return summary;
    }

    @Transactional
    /**
     * 根据购物车创建订单。
     *
     * @param request 收货人、联系电话、地址和配送方式
     * @return 创建成功后的订单主表实体
     * @throws BusinessException 当地址信息不完整、购物车为空、处方药直接下单或库存不足时抛出
     */
    public OrderEntity createOrder(CreateOrderRequest request) {
        validateAddress(request);
        List<CartItemEntity> cartItems = cartRepository.findByUserIdOrderByUpdatedAtDesc(CartService.DEFAULT_USER_ID);
        if (cartItems.isEmpty()) {
            throw new BusinessException("购物车为空，无法提交订单");
        }

        Map<Long, MedicineEntity> medicineMap = medicineRepository.findAllById(
                        cartItems.stream().map(CartItemEntity::getMedicineId).toList())
                .stream()
                .collect(Collectors.toMap(MedicineEntity::getId, Function.identity()));

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItemEntity cartItem : cartItems) {
            MedicineEntity medicine = medicineMap.get(cartItem.getMedicineId());
            if (medicine == null || medicine.getStatus() == null || medicine.getStatus() != 1) {
                throw new BusinessException("购物车中存在已下架药品");
            }
            if (Boolean.TRUE.equals(medicine.getPrescriptionRequired())) {
                throw new BusinessException("处方药需审核后购买，暂不支持直接下单");
            }
            if (cartItem.getQuantity() <= 0) {
                throw new BusinessException("购物车数量异常");
            }
            if (medicine.getStock() < cartItem.getQuantity()) {
                throw new BusinessException(medicine.getName() + " 库存不足");
            }
            totalAmount = totalAmount.add(medicine.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        OrderEntity order = new OrderEntity();
        order.setOrderNo(generateOrderNo());
        order.setUserId(CartService.DEFAULT_USER_ID);
        order.setTotalAmount(totalAmount);
        order.setDeliveryFee(DELIVERY_FEE);
        order.setPayAmount(totalAmount.add(DELIVERY_FEE));
        order.setStatus(OrderStatusEnum.WAIT_PAY.name());
        order.setReceiverName(request.getReceiverName().trim());
        order.setReceiverPhone(request.getReceiverPhone().trim());
        order.setReceiverAddress(request.getReceiverAddress().trim());
        order.setDeliveryMethod(StringUtils.hasText(request.getDeliveryMethod()) ? request.getDeliveryMethod() : "标准配送");
        order = orderRepository.save(order);

        for (CartItemEntity cartItem : cartItems) {
            MedicineEntity medicine = medicineMap.get(cartItem.getMedicineId());
            medicine.setStock(medicine.getStock() - cartItem.getQuantity());
            medicineRepository.save(medicine);
            orderItemRepository.save(toOrderItem(order.getId(), cartItem, medicine));
        }

        cartRepository.deleteByUserId(CartService.DEFAULT_USER_ID);
        return order;
    }

    /**
     * 查询默认演示用户的订单列表。
     *
     * @return 按下单时间倒序排列的订单列表
     */
    public List<OrderEntity> listOrders() {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(CartService.DEFAULT_USER_ID);
    }

    /**
     * 查询订单详情。
     *
     * @param orderId 订单 ID
     * @return 包含订单主表、明细和支付记录的详情 VO
     * @throws BusinessException 当订单不存在时抛出
     */
    public OrderDetailVO getOrderDetail(Long orderId) {
        OrderEntity order = getUserOrder(orderId);
        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrder(order);
        vo.setItems(orderItemRepository.findByOrderId(order.getId()));
        vo.setPayments(paymentRecordRepository.findByOrderIdOrderByPaidAtDesc(order.getId()));
        vo.setStatusText(statusText(order.getStatus()));
        return vo;
    }

    @Transactional
    /**
     * 取消待支付订单。
     *
     * @param orderId 订单 ID
     * @throws BusinessException 当订单不存在或订单状态不是待支付时抛出
     */
    public void cancelOrder(Long orderId) {
        OrderEntity order = getUserOrder(orderId);
        if (!OrderStatusEnum.WAIT_PAY.name().equals(order.getStatus())) {
            throw new BusinessException("只有待支付订单可以取消");
        }
        List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getId());
        Map<Long, MedicineEntity> medicineMap = medicineRepository.findAllById(
                        items.stream().map(OrderItemEntity::getMedicineId).toList())
                .stream()
                .collect(Collectors.toMap(MedicineEntity::getId, Function.identity()));
        for (OrderItemEntity item : items) {
            MedicineEntity medicine = medicineMap.get(item.getMedicineId());
            if (medicine != null) {
                medicine.setStock(medicine.getStock() + item.getQuantity());
                medicineRepository.save(medicine);
            }
        }
        order.setStatus(OrderStatusEnum.CANCELED.name());
        order.setCanceledAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    /**
     * 将订单状态枚举值转换为中文展示文本。
     *
     * @param status 订单状态枚举名称
     * @return 中文状态文本；无法识别时返回原始状态值
     */
    public String statusText(String status) {
        try {
            return OrderStatusEnum.valueOf(status).getDescription();
        } catch (IllegalArgumentException ex) {
            return status;
        }
    }

    private OrderEntity getUserOrder(Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, CartService.DEFAULT_USER_ID)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
    }

    private void validateAddress(CreateOrderRequest request) {
        if (!StringUtils.hasText(request.getReceiverName())) {
            throw new BusinessException("请填写收货人姓名");
        }
        if (!StringUtils.hasText(request.getReceiverPhone())) {
            throw new BusinessException("请填写联系电话");
        }
        if (!StringUtils.hasText(request.getReceiverAddress())) {
            throw new BusinessException("请填写收货地址");
        }
    }

    private OrderItemEntity toOrderItem(Long orderId, CartItemEntity cartItem, MedicineEntity medicine) {
        OrderItemEntity item = new OrderItemEntity();
        item.setOrderId(orderId);
        item.setMedicineId(medicine.getId());
        item.setMedicineName(medicine.getName());
        item.setSpecification(medicine.getSpecification());
        item.setManufacturer(medicine.getManufacturer());
        item.setImageUrl(medicine.getImageUrl());
        item.setPrice(medicine.getPrice());
        item.setQuantity(cartItem.getQuantity());
        item.setSubtotal(medicine.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        return item;
    }

    private String generateOrderNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "KN" + time + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
