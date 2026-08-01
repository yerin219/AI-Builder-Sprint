package com.memorydrawer.card.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.memorydrawer.card.domain.MemoryCard;

public interface MemoryCardRepository extends JpaRepository<MemoryCard, UUID> {

	boolean existsByDraftId(UUID draftId);

	List<MemoryCard> findAllByOwnerId(UUID ownerId);
}
