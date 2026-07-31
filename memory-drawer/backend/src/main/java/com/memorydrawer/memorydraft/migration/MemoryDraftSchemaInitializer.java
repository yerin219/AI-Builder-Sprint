package com.memorydrawer.memorydraft.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class MemoryDraftSchemaInitializer implements ApplicationRunner {

	private static final String TABLE_NAME = "memory_drafts";

	private final DataSource dataSource;

	public MemoryDraftSchemaInitializer(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public void run(ApplicationArguments args) throws SQLException {
		initialize();
	}

	void initialize() throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			Set<String> columns = findColumns(connection);
			if (columns.isEmpty()) {
				return;
			}
			if (!columns.contains("document_type")) {
				addColumn(connection, "document_type VARCHAR(32)");
			}
			if (!columns.contains("front_candidate")) {
				addColumn(connection, "front_candidate LONGTEXT");
			}
		}
	}

	private Set<String> findColumns(Connection connection) throws SQLException {
		DatabaseMetaData metadata = connection.getMetaData();
		Set<String> columns = new HashSet<>();
		try (ResultSet resultSet = metadata.getColumns(
			connection.getCatalog(),
			null,
			null,
			null
		)) {
			while (resultSet.next()) {
				String tableName = resultSet.getString("TABLE_NAME");
				if (TABLE_NAME.equalsIgnoreCase(tableName)) {
					columns.add(
						resultSet.getString("COLUMN_NAME").toLowerCase(Locale.ROOT)
					);
				}
			}
		}
		return columns;
	}

	private void addColumn(Connection connection, String definition) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(
				"ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + definition
			);
		}
	}
}
