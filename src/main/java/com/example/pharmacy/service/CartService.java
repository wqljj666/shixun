package com.example.pharmacy.service;

import com.example.pharmacy.common.BusinessException;
import com.example.pharmacy.entity.CartItemEntity;
import com.example.pharmacy.entity.MedicineEntity;
import com.example.pharmacy.repository.CartRepository;
import com.example.pharmacy.repository.MedicineRepository;
import com.example.pharmacy.vo.CartItemVO;
import com.example.pharmacy.vo.CartSummaryVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
/**
 * 购物车业务服务。
 *
 * <p>负责加入购物车、修改数量、删除商品和购物车汇总计算。当前课程项目未接入登录模块，
 * 因此使用默认演示用户。核心规则包括：药品必须存在且上架、数量必须大于 0、不可超过库存、
 * 处方药不能直接加入购物车。</p>
 */
public class CartService {

    public static final Long DEFAULT_USER_ID = 1L;

    private final CartRepository cartRepository;
    private final MedicineRepository medicineRepository;

    public CartService(CartRepository cartRepository, MedicineRepository medicineRepository) {
        this.cartRepository = cartRepository;
        this.medicineRepository = medicineRepository;
    }

    @Transactional
    /**
     * 将药品加入购物车。
     *
     * @param medicineId 药品 ID
     * @param quantity 加入数量，必须大于 0
     * @throws BusinessException 当药品不存在、处方药直接购买、数量非法或库存不足时抛出
     */
    public void addToCart(Long medicineId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("加入购物车数量必须大于 0");
        }
        MedicineEntity medicine = getAvailableMedicine(medicineId);
        if (Boolean.TRUE.equals(medicine.getPrescriptionRequired())) {
            throw new BusinessException("处方药需审核后购买，暂不支持直接加入购物车");
        }
        if (medicine.getStock() < quantity) {
            throw new BusinessException("库存不足，当前库存为 " + medicine.getStock());
        }

        CartItemEntity cartItem = cartRepository.findByUserIdAndMedicineId(DEFAULT_USER_ID, medicineId)
                .orElseGet(CartItemEntity::new);
        if (cartItem.getId() == null) {
            cartItem.setUserId(DEFAULT_USER_ID);
            cartItem.setMedicineId(medicineId);
            cartItem.setQuantity(quantity);
        } else {
            int newQuantity = cartItem.getQuantity() + quantity;
            if (newQuantity > medicine.getStock()) {
                throw new BusinessException("购物车数量超过当前库存");
            }
            cartItem.setQuantity(newQuantity);
        }
        cartRepository.save(cartItem);
    }

    @Transactional
    /**
     * 修改购物车中某个商品的数量。
     *
     * @param cartItemId 购物车条目 ID
     * @param quantity 新数量，必须大于 0 且不能超过库存
     * @throws BusinessException 当购物车条目不存在、数量非法或库存不足时抛出
     */
    public void updateQuantity(Long cartItemId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("购物车数量必须大于 0");
        }
        CartItemEntity cartItem = cartRepository.findByIdAndUserId(cartItemId, DEFAULT_USER_ID)
                .orElseThrow(() -> new BusinessException("购物车商品不存在"));
        MedicineEntity medicine = getAvailableMedicine(cartItem.getMedicineId());
        if (quantity > medicine.getStock()) {
            throw new BusinessException("库存不足，当前库存为 " + medicine.getStock());
        }
        cartItem.setQuantity(quantity);
        cartRepository.save(cartItem);
    }

    @Transactional
    /**
     * 删除购物车商品。
     *
     * @param cartItemId 购物车条目 ID
     * @throws BusinessException 当购物车条目不存在时抛出
     */
    public void removeItem(Long cartItemId) {
        CartItemEntity cartItem = cartRepository.findByIdAndUserId(cartItemId, DEFAULT_USER_ID)
                .orElseThrow(() -> new BusinessException("购物车商品不存在"));
        cartRepository.delete(cartItem);
    }

    /**
     * 获取购物车汇总信息。
     *
     * @return 包含购物车商品、总数量和总金额的汇总对象
     */
    public CartSummaryVO getCartSummary() {
        List<CartItemEntity> cartItems = cartRepository.findByUserIdOrderByUpdatedAtDesc(DEFAULT_USER_ID);
        Map<Long, MedicineEntity> medicineMap = medicineRepository.findAllById(
                        cartItems.stream().map(CartItemEntity::getMedicineId).toList())
                .stream()
                .collect(Collectors.toMap(MedicineEntity::getId, Function.identity()));

        CartSummaryVO summary = new CartSummaryVO();
        List<CartItemVO> items = cartItems.stream()
                .filter(item -> medicineMap.containsKey(item.getMedicineId()))
                .map(item -> toCartItemVO(item, medicineMap.get(item.getMedicineId())))
                .toList();
        summary.setItems(items);
        summary.setTotalQuantity(items.stream().mapToInt(item -> item.getCartItem().getQuantity()).sum());
        summary.setTotalAmount(items.stream()
                .map(CartItemVO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return summary;
    }

    private CartItemVO toCartItemVO(CartItemEntity cartItem, MedicineEntity medicine) {
        CartItemVO vo = new CartItemVO();
        vo.setCartItem(cartItem);
        vo.setMedicine(medicine);
        vo.setSubtotal(medicine.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        return vo;
    }

    private MedicineEntity getAvailableMedicine(Long medicineId) {
        return medicineRepository.findById(medicineId)
                .filter(medicine -> medicine.getStatus() != null && medicine.getStatus() == 1)
                .orElseThrow(() -> new BusinessException("药品不存在或已下架"));
    }
}
