package com.example.pharmacy.repository;

import com.example.pharmacy.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<CartItemEntity, Long> {

    List<CartItemEntity> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<CartItemEntity> findByUserIdAndMedicineId(Long userId, Long medicineId);

    Optional<CartItemEntity> findByIdAndUserId(Long id, Long userId);

    void deleteByUserId(Long userId);
}
