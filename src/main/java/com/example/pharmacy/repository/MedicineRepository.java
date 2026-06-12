package com.example.pharmacy.repository;

import com.example.pharmacy.entity.MedicineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MedicineRepository extends JpaRepository<MedicineEntity, Long> {

    @Query("""
            select m from MedicineEntity m
            where m.status = 1
              and (:categoryId is null or m.categoryId = :categoryId)
              and (:keyword is null or :keyword = ''
                   or lower(m.name) like lower(concat('%', :keyword, '%'))
                   or lower(m.manufacturer) like lower(concat('%', :keyword, '%'))
                   or lower(m.description) like lower(concat('%', :keyword, '%')))
            order by m.createdAt desc, m.id desc
            """)
    List<MedicineEntity> search(@Param("keyword") String keyword, @Param("categoryId") Long categoryId);
}
