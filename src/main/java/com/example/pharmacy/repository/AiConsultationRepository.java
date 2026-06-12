package com.example.pharmacy.repository;

import com.example.pharmacy.entity.AiConsultationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiConsultationRepository extends JpaRepository<AiConsultationEntity, Long> {

    List<AiConsultationEntity> findByConversationIdAndUserIdOrderByCreatedAtAsc(String conversationId, Long userId);

    List<AiConsultationEntity> findTop10ByConversationIdAndUserIdOrderByCreatedAtDesc(String conversationId, Long userId);
}
