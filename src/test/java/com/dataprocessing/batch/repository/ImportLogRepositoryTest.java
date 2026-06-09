package com.dataprocessing.batch.repository;

import com.dataprocessing.batch.model.ImportLog;
import com.dataprocessing.batch.model.RejectedTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ImportLogRepositoryTest {

    @Autowired
    private ImportLogRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE rejected_transactions, import_logs, uploaded_files RESTART IDENTITY CASCADE");
        jdbcTemplate.update("INSERT INTO uploaded_files (id, filename, mime_type, content, status) VALUES (?, ?, ?, ?, ?)", 1L, "test.csv", "text/csv", "content".getBytes(), "PENDING");
    }

    @Test
    void shouldSaveImportLog() {
        // Arrange
        LocalDateTime startedAt = LocalDateTime.of(2025, 1, 15, 10, 30);

        // Act
        Long logId = repository.save(1L, startedAt);

        // Assert
        ImportLog savedImportLog = jdbcTemplate.queryForObject(
            "SELECT id, uploaded_file_id, status, total_records, valid_records, rejected_records, error_message, started_at, finished_at FROM import_logs WHERE id = ?",
            (rs, rowNum) -> getImportLog(rs),
            logId
        );

        ImportLog expectedImportLog = ImportLog.builder()
            .id(logId)
            .uploadedFileId(1L)
            .status("RUNNING")
            .startedAt(startedAt)
            .build();

        assertThat(savedImportLog).isEqualTo(expectedImportLog);
    }

    @Test
    void shouldUpdateLog() {
        // Arrange
        LocalDateTime startedAt = LocalDateTime.of(2025, 1, 15, 10, 30);
        LocalDateTime finishedAt = LocalDateTime.of(2025, 1, 15, 10, 31);

        Long logId = repository.save(1L, startedAt);
        ImportLog updatedImportLog = ImportLog.builder()
            .id(logId)
            .uploadedFileId(1L)
            .status("SUCCESS")
            .totalRecords(10)
            .validRecords(8)
            .rejectedRecords(2)
            .finishedAt(finishedAt)
            .build();

        // Act
        repository.update(updatedImportLog);

        // Assert
        ImportLog savedImportLog = jdbcTemplate.queryForObject(
            "SELECT id, uploaded_file_id, status, total_records, valid_records, rejected_records, error_message, started_at, finished_at FROM import_logs WHERE id = ?",
            (rs, rowNum) -> getImportLog(rs),
            logId
        );

        ImportLog expectedImportLog = ImportLog.builder()
            .id(logId)
            .uploadedFileId(1L)
            .status("SUCCESS")
            .totalRecords(10)
            .validRecords(8)
            .rejectedRecords(2)
            .startedAt(startedAt)
            .finishedAt(finishedAt)
            .build();

        assertThat(savedImportLog).isEqualTo(expectedImportLog);
    }

    @Test
    void shouldSaveRejectedTransactions() {
        // Arrange
        Long logId = repository.save(1L, LocalDateTime.now());
        List<RejectedTransaction> rejectedTransactions = List.of(
            new RejectedTransaction("REF001", "amount", "amount is required"),
            new RejectedTransaction("REF002", "date", "date cannot be in the future")
        );

        // Act
        repository.saveRejectedTransactions(logId, rejectedTransactions);

        // Assert
        List<RejectedTransaction> savedRejectedTransactions = jdbcTemplate.query(
            "SELECT reference, field, reason FROM rejected_transactions WHERE import_log_id = ? ORDER BY id",
            (rs, rowNum) -> getRejectedTransaction(rs),
            logId
        );

        assertThat(savedRejectedTransactions).containsExactlyElementsOf(rejectedTransactions);
    }

    @Test
    void shouldReturnLatestLog() {
        // Arrange
        LocalDateTime firstStartedAt = LocalDateTime.of(2025, 1, 15, 10, 30);
        LocalDateTime secondStartedAt = LocalDateTime.of(2025, 1, 15, 10, 31);

        repository.save(1L, firstStartedAt);
        Long latestId = repository.save(1L, secondStartedAt);

        // Act
        Optional<ImportLog> importLogFound = repository.findLatest();

        // Assert
        ImportLog expectedImportLog = ImportLog.builder()
            .id(latestId)
            .uploadedFileId(1L)
            .status("RUNNING")
            .startedAt(secondStartedAt)
            .build();

        assertThat(importLogFound).isPresent().get().isEqualTo(expectedImportLog);
    }

    @Test
    void shouldReturnEmpty_whenNoLogsExist() {
        // Arrange

        // Act
        Optional<ImportLog> result = repository.findLatest();

        // Assert
        assertThat(result).isEmpty();
    }

    private static RejectedTransaction getRejectedTransaction(ResultSet rs) throws SQLException {
        return new RejectedTransaction(
            rs.getString("reference"),
            rs.getString("field"),
            rs.getString("reason")
        );
    }

    private static ImportLog getImportLog(ResultSet rs) throws SQLException {
        return ImportLog.builder()
            .id(rs.getLong("id"))
            .uploadedFileId(rs.getLong("uploaded_file_id"))
            .status(rs.getString("status"))
            .totalRecords(rs.getObject("total_records", Integer.class))
            .validRecords(rs.getObject("valid_records", Integer.class))
            .rejectedRecords(rs.getObject("rejected_records", Integer.class))
            .errorMessage(rs.getString("error_message"))
            .startedAt(rs.getTimestamp("started_at").toLocalDateTime())
            .finishedAt(rs.getTimestamp("finished_at") == null ? null : rs.getTimestamp("finished_at").toLocalDateTime())
            .build();
    }
}
