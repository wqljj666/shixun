package com.example.pharmacy.repository;

import com.example.pharmacy.entity.MedicineCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicineCategoryRepository extends JpaRepository<MedicineCategoryEntity, Long> {

    List<MedicineCategoryEntity> findByStatusOrderBySortOrderAscIdAsc(Integer status);
}
