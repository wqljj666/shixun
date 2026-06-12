package com.example.pharmacy.vo;

import com.example.pharmacy.entity.CartItemEntity;
import com.example.pharmacy.entity.MedicineEntity;

import java.math.BigDecimal;

public class CartItemVO {

    private CartItemEntity cartItem;
    private MedicineEntity medicine;
    private BigDecimal subtotal;

    public CartItemEntity getCartItem() {
        return cartItem;
    }

    public void setCartItem(CartItemEntity cartItem) {
        this.cartItem = cartItem;
    }

    public MedicineEntity getMedicine() {
        return medicine;
    }

    public void setMedicine(MedicineEntity medicine) {
        this.medicine = medicine;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
