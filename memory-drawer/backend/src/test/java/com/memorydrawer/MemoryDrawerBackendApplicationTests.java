package com.memorydrawer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.memorydrawer.memorydraft.domain.DocumentType;
import com.memorydrawer.memorydraft.domain.DraftStatus;
import com.memorydrawer.memorydraft.domain.MemoryDraft;
import com.memorydrawer.memorydraft.repository.MemoryDraftRepository;

@ActiveProfiles("test")
@SpringBootTest
class MemoryDrawerBackendApplicationTests {

	@Autowired
	private MemoryDraftRepository memoryDraftRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void storesAnalyzedMemoryDraftWithSchema() {
		Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
		MemoryDraft draft = MemoryDraft.analyzed(
			UUID.randomUUID(),
			UUID.randomUUID(),
			"drafts/user/draft/original.jpg",
			"image/jpeg",
			"{\"text\":\"영수증\",\"html\":\"<p>영수증</p>\"}",
			DocumentType.RECEIPT,
			now,
			now.plus(7, ChronoUnit.DAYS)
		);

		MemoryDraft saved = memoryDraftRepository.saveAndFlush(draft);
		MemoryDraft found = memoryDraftRepository.findById(saved.getId()).orElseThrow();

		assertThat(found.getDraftStatus()).isEqualTo(DraftStatus.TYPE_PENDING);
		assertThat(found.getSuggestedDocumentType()).isEqualTo(DocumentType.RECEIPT);
		assertThat(found.getParsedContent()).contains("영수증");
	}
}
