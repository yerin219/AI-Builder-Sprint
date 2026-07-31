package com.memorydrawer.memorydraft.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class MemoryDraftSchemaInitializerTests {

	@Test
	void addsApi4ColumnsToExistingApi3TableOnlyOnce() throws Exception {
		String databaseName = "schema_" + UUID.randomUUID().toString().replace("-", "");
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
			"jdbc:h2:mem:" + databaseName + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
			"sa",
			""
		);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute("""
			CREATE TABLE memory_drafts (
			    id CHAR(36) NOT NULL,
			    owner_id CHAR(36) NOT NULL,
			    PRIMARY KEY (id)
			)
			""");

		MemoryDraftSchemaInitializer initializer = new MemoryDraftSchemaInitializer(dataSource);
		initializer.initialize();
		initializer.initialize();

		Integer documentTypeCount = jdbcTemplate.queryForObject(
			"""
				SELECT COUNT(*)
				FROM INFORMATION_SCHEMA.COLUMNS
				WHERE TABLE_NAME = 'MEMORY_DRAFTS'
				  AND COLUMN_NAME = 'DOCUMENT_TYPE'
				""",
			Integer.class
		);
		Integer frontCandidateCount = jdbcTemplate.queryForObject(
			"""
				SELECT COUNT(*)
				FROM INFORMATION_SCHEMA.COLUMNS
				WHERE TABLE_NAME = 'MEMORY_DRAFTS'
				  AND COLUMN_NAME = 'FRONT_CANDIDATE'
				""",
			Integer.class
		);

		assertThat(documentTypeCount).isEqualTo(1);
		assertThat(frontCandidateCount).isEqualTo(1);
	}
}
