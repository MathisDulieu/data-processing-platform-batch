package com.dataprocessing.batch.repository;

import com.dataprocessing.batch.BatchApplication;
import com.dataprocessing.batch.BatchConfigurationTests;
import com.dataprocessing.batch.model.ImportLog;
import com.dataprocessing.batch.model.RejectedTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
    BatchApplication.class,
    BatchConfigurationTests.class
})
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
    void shouldReturnGeneratedId_whenSaveIsCalled() {
        //Arrange

        //Act
        Long logId = repository.save(1L, LocalDateTime.now());

        //Assert
        assertThat(logId).isNotNull().isPositive();
    }

    @Test
    void shouldPersistRunningStatus_whenSaveIsCalled() {
        //Arrange

        //Act
        Long logId = repository.save(1L, LocalDateTime.now());

        //Assert
        String status = jdbcTemplate.queryForObject("SELECT status FROM import_logs WHERE id = ?", String.class, logId);
        assertThat(status).isEqualTo("RUNNING");
    }

    @Test
    void shouldUpdateLog_whenUpdateIsCalled() {
        //Arrange
        Long logId = repository.save(1L, LocalDateTime.now());
        LocalDateTime finishedAt = LocalDateTime.now();
        ImportLog updated = ImportLog.builder()
            .id(logId)
            .uploadedFileId(1L)
            .status("SUCCESS")
            .totalRecords(10)
            .validRecords(8)
            .rejectedRecords(2)
            .finishedAt(finishedAt)
            .build();

        //Act
        repository.update(updated);

        //Assert
        ImportLog saved = jdbcTemplate.queryForObject(
            "SELECT id, uploaded_file_id, status, total_records, valid_records, rejected_records, error_message, started_at, finished_at FROM import_logs WHERE id = ?",
            (rs, rowNum) -> new ImportLog(
                rs.getLong("id"), rs.getLong("uploaded_file_id"), rs.getString("status"),
                rs.getObject("total_records", Integer.class), rs.getObject("valid_records", Integer.class),
                rs.getObject("rejected_records", Integer.class), rs.getString("error_message"),
                null, Optional.ofNullable(rs.getTimestamp("finished_at")).map(ts -> ts.toLocalDateTime()).orElse(null)
            ), logId
        );
        assertThat(saved.status()).isEqualTo("SUCCESS");
        assertThat(saved.totalRecords()).isEqualTo(10);
        assertThat(saved.validRecords()).isEqualTo(8);
        assertThat(saved.rejectedRecords()).isEqualTo(2);
        assertThat(saved.finishedAt()).isNotNull();
    }

    @Test
    void shouldSaveRejectedTransactions_whenSaveRejectedTransactionsIsCalled() {
        //Arrange
        Long logId = repository.save(1L, LocalDateTime.now());
        List<RejectedTransaction> rejected = List.of(
            new RejectedTransaction("REF001", "amount", "amount is required"),
            new RejectedTransaction("REF002", "date", "date cannot be in the future")
        );

        //Act
        repository.saveRejectedTransactions(logId, rejected);

        //Assert
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rejected_transactions WHERE import_log_id = ?", Integer.class, logId);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldReturnLatestLog_whenFindLatestIsCalled() {
        //Arrange
        repository.save(1L, LocalDateTime.now().minusMinutes(5));
        Long latestId = repository.save(1L, LocalDateTime.now());

        //Act
        Optional<ImportLog> result = repository.findLatest();

        //Assert
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(latestId);
    }

    @Test
    void shouldReturnEmpty_whenNoLogsExist() {
        //Act
        Optional<ImportLog> result = repository.findLatest();

        //Assert
        assertThat(result).isEmpty();
    }
}
