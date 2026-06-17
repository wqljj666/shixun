package com.example.pharmacy.service;

import com.example.pharmacy.common.BusinessException;
import com.example.pharmacy.entity.CartItemEntity;
import com.example.pharmacy.entity.MedicineEntity;
import com.example.pharmacy.repository.CartRepository;
import com.example.pharmacy.repository.MedicineRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private MedicineRepository medicineRepository;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, medicineRepository);
    }

    /**
     * 场景：普通 OTC 药品加入购物车，应该保存新的购物车条目。
     */
    @Test
    void addToCart_withOtcMedicine_shouldSaveCartItem() {
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(medicine(1L, false, 10)));
        when(cartRepository.findByUserIdAndMedicineId(CartService.DEFAULT_USER_ID, 1L)).thenReturn(Optional.empty());

        cartService.addToCart(1L, 2);

        ArgumentCaptor<CartItemEntity> captor = ArgumentCaptor.forClass(CartItemEntity.class);
        verify(cartRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getMedicineId());
        assertEquals(2, captor.getValue().getQuantity());
    }

    /**
     * 场景：修改购物车数量为 2 后，购物车汇总金额应等于单价乘数量。
     */
    @Test
    void updateQuantityAndSummary_shouldCalculateTotalAmount() {
        CartItemEntity item = cartItem(11L, 1L, 1);
        when(cartRepository.findByIdAndUserId(11L, CartService.DEFAULT_USER_ID)).thenReturn(Optional.of(item));
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(medicine(1L, false, 10)));

        cartService.updateQuantity(11L, 2);

        assertEquals(2, item.getQuantity());

        when(cartRepository.findByUserIdOrderByUpdatedAtDesc(CartService.DEFAULT_USER_ID)).thenReturn(List.of(item));
        when(medicineRepository.findAllById(List.of(1L))).thenReturn(List.of(medicine(1L, false, 10)));

        CartSummaryVO summary = cartService.getCartSummary();

        assertEquals(2, summary.getTotalQuantity());
        assertEquals(new BigDecimal("59.60"), summary.getTotalAmount());
    }

    /**
     * 场景：删除购物车商品，应该调用仓储删除该条目。
     */
    @Test
    void removeItem_shouldDeleteCartItem() {
        CartItemEntity item = cartItem(11L, 1L, 1);
        when(cartRepository.findByIdAndUserId(11L, CartService.DEFAULT_USER_ID)).thenReturn(Optional.of(item));

        cartService.removeItem(11L);

        verify(cartRepository).delete(item);
    }

    /**
     * 场景：处方药不能直接加入购物车。
     */
    @Test
    void addToCart_withPrescriptionMedicine_shouldThrowBusinessException() {
        when(medicineRepository.findById(8L)).thenReturn(Optional.of(medicine(8L, true, 10)));

        assertThrows(BusinessException.class, () -> cartService.addToCart(8L, 1));
    }

    private MedicineEntity medicine(Long id, boolean prescriptionRequired, int stock) {
        MedicineEntity medicine = new MedicineEntity();
        medicine.setId(id);
        medicine.setName("测试药品");
        medicine.setPrice(new BigDecimal("29.80"));
        medicine.setStock(stock);
        medicine.setStatus(1);
        medicine.setPrescriptionRequired(prescriptionRequired);
        medicine.setOtcFlag(!prescriptionRequired);
        return medicine;
    }

    private CartItemEntity cartItem(Long id, Long medicineId, int quantity) {
        CartItemEntity item = new CartItemEntity();
        item.setId(id);
        item.setUserId(CartService.DEFAULT_USER_ID);
        item.setMedicineId(medicineId);
        item.setQuantity(quantity);
        return item;
    }
}
