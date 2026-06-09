package com.dataprocessing.batch;

import com.dataprocessing.batch.model.ImportLog;
import com.dataprocessing.batch.model.RejectedTransaction;
import com.dataprocessing.batch.model.Transaction;
import com.dataprocessing.batch.service.BatchProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class BatchIntegrationTests {

    @Autowired
    private BatchProcessingService batchProcessingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE rejected_transactions, import_logs, transactions, uploaded_files RESTART IDENTITY CASCADE");
    }

    @Test
    void shouldProcessCsvFile() {
        // Arrange
        String csv = """
            reference,label,amount,currency,date,category
             ref001 , Amazon purchase ,49.99,eur,2025-01-15, shopping
            REF002,January salary,3000.00,EUR,2025-01-31,INCOME
            REF003,Future date invalid,50.00,EUR,2099-12-31,SHOPPING
            REF004,,100.00,EUR,2025-01-10,SHOPPING
            """;

        jdbcTemplate.update(
            "INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)",
            "test.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8), "PENDING"
        );

        // Act
        int processedFiles = batchProcessingService.run();

        // Assert
        assertThat(processedFiles).isEqualTo(1);

        String fileStatus = jdbcTemplate.queryForObject("SELECT status FROM uploaded_files WHERE filename = 'test.csv'", String.class);
        assertThat(fileStatus).isEqualTo("PROCESSED");

        Integer transactionsCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions", Integer.class);
        assertThat(transactionsCount).isEqualTo(2);

        List<Transaction> transactions = jdbcTemplate.query(
            "SELECT * FROM transactions ORDER BY reference",
            (rs, rowNum) -> getTransaction(rs)
        );

        Transaction expectedTransaction1 = Transaction.builder()
            .reference("REF001")
            .label("Amazon purchase")
            .amount(new BigDecimal("49.99"))
            .currency("EUR")
            .date(LocalDate.of(2025, 1, 15))
            .category("SHOPPING")
            .uploadedFileId(1L)
            .build();

        Transaction expectedTransaction2 = Transaction.builder()
            .reference("REF002")
            .label("January salary")
            .amount(new BigDecimal("3000.00"))
            .currency("EUR")
            .date(LocalDate.of(2025, 1, 31))
            .category("INCOME")
            .uploadedFileId(1L)
            .build();

        assertThat(transactions).isEqualTo(List.of(expectedTransaction1, expectedTransaction2));

        List<RejectedTransaction> rejectedTransactions = jdbcTemplate.query(
            "SELECT * FROM rejected_transactions ORDER BY reference",
            (rs, rowNum) -> getRejectedTransaction(rs)
        );

        RejectedTransaction expectedRejectedTransaction1 = new RejectedTransaction("REF003", "date", "date cannot be in the future");
        RejectedTransaction expectedRejectedTransaction2 = new RejectedTransaction("REF004", "label", "label is required");

        assertThat(rejectedTransactions).isEqualTo(List.of(expectedRejectedTransaction1, expectedRejectedTransaction2));
    }

    @Test
    void shouldMarkCsvFileAsFailed_whenCsvContentIsMalformed() {
        // Arrange
        jdbcTemplate.update(
            "INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)",
            "malformed.csv", "text/csv", new byte[]{(byte) 0xFF, (byte) 0xFE}, "PENDING"
        );

        // Act
        batchProcessingService.run();

        // Assert
        String fileStatus = jdbcTemplate.queryForObject("SELECT status FROM uploaded_files WHERE filename = 'malformed.csv'", String.class);
        assertThat(fileStatus).isEqualTo("FAILED");

        ImportLog importLog = jdbcTemplate.queryForObject(
            "SELECT * FROM import_logs ORDER BY started_at DESC LIMIT 1",
            (rs, rowNum) -> getImportLog(rs)
        );

        assertThat(importLog).isNotNull();
        assertThat(importLog.startedAt()).isNotNull();
        assertThat(importLog.finishedAt()).isNotNull();

        ImportLog expectedImportLog = ImportLog.builder()
            .id(1L)
            .uploadedFileId(1L)
            .status("FAILED")
            .totalRecords(0)
            .validRecords(0)
            .rejectedRecords(0)
            .errorMessage("Missing required CSV column: reference")
            .startedAt(importLog.startedAt())
            .finishedAt(importLog.finishedAt())
            .build();

        assertThat(importLog).isEqualTo(expectedImportLog);
    }

    @Test
    void shouldProcessJsonFile_whenFileContainsValidAndInvalidRecords() {
        // Arrange
        String json = """
            [
              {"reference":"JSN001","label":"Valid transaction","amount":250.00,"currency":"EUR","date":"2025-09-01","category":"SHOPPING"},
              {"reference":"JSN002","label":"Future date","amount":100.00,"currency":"EUR","date":"2099-01-01","category":"SHOPPING"},
              {"reference":"","label":"Empty reference","amount":50.00,"currency":"EUR","date":"2025-09-03","category":"FOOD"},
              {"reference":"JSN004","label":"Valid without category","amount":75.00,"currency":"USD","date":"2025-09-05","category":null}
            ]
            """;

        jdbcTemplate.update(
            "INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)",
            "test.json", "application/json", json.getBytes(StandardCharsets.UTF_8), "PENDING"
        );

        // Act
        int processedFiles = batchProcessingService.run();

        // Assert
        assertThat(processedFiles).isEqualTo(1);

        String fileStatus = jdbcTemplate.queryForObject("SELECT status FROM uploaded_files WHERE filename = 'test.json'", String.class);
        assertThat(fileStatus).isEqualTo("PROCESSED");

        Integer transactionsCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions", Integer.class);
        assertThat(transactionsCount).isEqualTo(2);

        List<Transaction> transactions = jdbcTemplate.query(
            "SELECT * FROM transactions ORDER BY reference",
            (rs, rowNum) -> getTransaction(rs)
        );

        Transaction expectedTransaction1 = Transaction.builder()
            .reference("JSN001")
            .label("Valid transaction")
            .amount(new BigDecimal("250.00"))
            .currency("EUR")
            .date(LocalDate.of(2025, 9, 1))
            .category("SHOPPING")
            .uploadedFileId(1L)
            .build();

        Transaction expectedTransaction2 = Transaction.builder()
            .reference("JSN004")
            .label("Valid without category")
            .amount(new BigDecimal("75.00"))
            .currency("USD")
            .date(LocalDate.of(2025, 9, 5))
            .category("UNCATEGORIZED")
            .uploadedFileId(1L)
            .build();

        assertThat(transactions).containsExactly(expectedTransaction1, expectedTransaction2);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rejected_transactions", Integer.class)).isEqualTo(2);

        List<RejectedTransaction> rejectedTransactions = jdbcTemplate.query(
            "SELECT * FROM rejected_transactions ORDER BY reference",
            (rs, rowNum) -> getRejectedTransaction(rs)
        );

        assertThat(rejectedTransactions).containsExactly(
            new RejectedTransaction("", "reference", "reference is required"),
            new RejectedTransaction("JSN002", "date", "date cannot be in the future")
        );

        ImportLog importLog = jdbcTemplate.queryForObject(
            "SELECT * FROM import_logs ORDER BY started_at DESC LIMIT 1",
            (rs, rowNum) -> getImportLog(rs)
        );

        assertThat(importLog).isNotNull();
        assertThat(importLog.startedAt()).isNotNull();
        assertThat(importLog.finishedAt()).isNotNull();

        assertThat(importLog).isEqualTo(ImportLog.builder()
            .id(1L)
            .uploadedFileId(1L)
            .status("SUCCESS")
            .totalRecords(4)
            .validRecords(2)
            .rejectedRecords(2)
            .errorMessage(null)
            .startedAt(importLog.startedAt())
            .finishedAt(importLog.finishedAt())
            .build()
        );
    }

    @Test
    void shouldMarkJsonFileAsFailed_whenJsonContentIsMalformed() {
        // Arrange
        jdbcTemplate.update(
            "INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)",
            "malformed.json", "application/json", "not valid json".getBytes(StandardCharsets.UTF_8), "PENDING"
        );

        // Act
        batchProcessingService.run();

        // Assert
        String fileStatus = jdbcTemplate.queryForObject("SELECT status FROM uploaded_files WHERE filename = 'malformed.json'", String.class);
        assertThat(fileStatus).isEqualTo("FAILED");

        ImportLog importLog = jdbcTemplate.queryForObject(
            "SELECT * FROM import_logs ORDER BY started_at DESC LIMIT 1",
            (rs, rowNum) -> getImportLog(rs)
        );

        assertThat(importLog).isNotNull();
        assertThat(importLog.startedAt()).isNotNull();
        assertThat(importLog.finishedAt()).isNotNull();

        ImportLog expectedImportLog = ImportLog.builder()
            .id(1L)
            .uploadedFileId(1L)
            .status("FAILED")
            .totalRecords(0)
            .validRecords(0)
            .rejectedRecords(0)
            .errorMessage("Failed to parse JSON content")
            .startedAt(importLog.startedAt())
            .finishedAt(importLog.finishedAt())
            .build();

        assertThat(importLog).isEqualTo(expectedImportLog);
    }

    @Test
    void shouldNotReprocessFile_whenFileIsAlreadyProcessed() {
        // Arrange
        String json = """
            [{"reference":"JSN001","label":"Valid","amount":100.00,"currency":"EUR","date":"2025-01-01","category":"INCOME"}]
            """;

        jdbcTemplate.update(
            "INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)",
            "already-processed.json", "application/json", json.getBytes(StandardCharsets.UTF_8), "PROCESSED"
        );

        // Act
        int processedFiles = batchProcessingService.run();

        // Assert
        assertThat(processedFiles).isZero();

        Integer transactionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions", Integer.class);
        assertThat(transactionCount).isZero();
    }

    private static ImportLog getImportLog(ResultSet rs) throws SQLException {
        return ImportLog.builder()
            .id(rs.getLong("id"))
            .uploadedFileId(rs.getLong("uploaded_file_id"))
            .status(rs.getString("status"))
            .totalRecords(rs.getInt("total_records"))
            .validRecords(rs.getInt("valid_records"))
            .rejectedRecords(rs.getInt("rejected_records"))
            .errorMessage(rs.getString("error_message"))
            .startedAt(rs.getTimestamp("started_at").toLocalDateTime())
            .finishedAt(rs.getTimestamp("finished_at").toLocalDateTime())
            .build();
    }

    private static RejectedTransaction getRejectedTransaction(ResultSet rs) throws SQLException {
        return new RejectedTransaction(
            rs.getString("reference"),
            rs.getString("field"),
            rs.getString("reason")
        );
    }

    private static Transaction getTransaction(ResultSet rs) throws SQLException {
        return Transaction.builder()
            .reference(rs.getString("reference"))
            .label(rs.getString("label"))
            .amount(rs.getBigDecimal("amount"))
            .currency(rs.getString("currency"))
            .date(rs.getDate("date").toLocalDate())
            .category(rs.getString("category"))
            .uploadedFileId(rs.getLong("uploaded_file_id"))
            .build();
    }
}
