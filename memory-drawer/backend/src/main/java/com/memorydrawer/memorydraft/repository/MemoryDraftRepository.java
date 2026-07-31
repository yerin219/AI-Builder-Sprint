package com.memorydrawer.memorydraft.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.memorydrawer.memorydraft.domain.DraftStatus;
import com.memorydrawer.memorydraft.domain.MemoryDraft;

public interface MemoryDraftRepository extends JpaRepository<MemoryDraft, UUID> {

	List<MemoryDraft> findAllByExpiresAtBeforeAndDraftStatusNot(Instant expiresAt, DraftStatus draftStatus);

	Optional<MemoryDraft> findByIdAndOwnerId(UUID id, UUID ownerId);
}
