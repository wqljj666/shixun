package com.example.pharmacy.repository;

import com.example.pharmacy.entity.AiConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversationEntity, Long> {

    Optional<AiConversationEntity> findByConversationIdAndUserId(String conversationId, Long userId);

    List<AiConversationEntity> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
