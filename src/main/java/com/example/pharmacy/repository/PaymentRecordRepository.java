package com.example.pharmacy.repository;

import com.example.pharmacy.entity.PaymentRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecordEntity, Long> {

    List<PaymentRecordEntity> findByOrderIdOrderByPaidAtDesc(Long orderId);
}
