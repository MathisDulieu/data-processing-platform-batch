package com.dataprocessing.batch;

import com.dataprocessing.batch.service.BatchProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(BatchConfigurationTests.class)
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
    void shouldProcessCsvFile_whenFileContainsValidAndInvalidRecords() {
        //Arrange
        String csv = "reference,label,amount,currency,date,category\n" +
            "REF001,Amazon purchase,49.99,EUR,2025-01-15,SHOPPING\n" +
            "REF002,January salary,3000.00,EUR,2025-01-31,INCOME\n" +
            "REF003,Future date invalid,50.00,EUR,2099-12-31,SHOPPING\n" +
            "REF004,,100.00,EUR,2025-01-10,SHOPPING";
        jdbcTemplate.update(
            "INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)",
            "test.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8), "PENDING"
        );

        //Act
        int processedFiles = batchProcessingService.run();

        //Assert
        assertThat(processedFiles).isEqualTo(1);

        String fileStatus = jdbcTemplate.queryForObject("SELECT status FROM uploaded_files WHERE filename = 'test.csv'", String.class);
        assertThat(fileStatus).isEqualTo("PROCESSED");

        Integer transactionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions", Integer.class);
        assertThat(transactionCount).isEqualTo(2);

        Integer rejectedCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rejected_transactions", Integer.class);
        assertThat(rejectedCount).isEqualTo(2);

        String logStatus = jdbcTemplate.queryForObject("SELECT status FROM import_logs ORDER BY started_at DESC LIMIT 1", String.class);
        assertThat(logStatus).isEqualTo("SUCCESS");

        Integer totalRecords = jdbcTemplate.queryForObject("SELECT total_records FROM import_logs ORDER BY started_at DESC LIMIT 1", Integer.class);
        assertThat(totalRecords).isEqualTo(4);
    }

    @Test
    void shouldNormaliseCsvTransactions_whenCsvFileIsProcessed() {
        //Arrange
        String csv = "reference,label,amount,currency,date,category\n" +
            "ref001,  Purchase with spaces  ,25.00,eur,2025-06-01,shopping";
        jdbcTemplate.update(
            "INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)",
            "normalisation.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8), "PENDING"
        );

        //Act
        batchProcessingService.run();

        //Assert
        String reference = jdbcTemplate.queryForObject("SELECT reference FROM transactions WHERE reference = 'REF001'", String.class);
        assertThat(reference).isEqualTo("REF001");

        String currency = jdbcTemplate.queryForObject("SELECT currency FROM transactions WHERE reference = 'REF001'", String.class);
        assertThat(currency).isEqualTo("EUR");

        String label = jdbcTemplate.queryForObject("SELECT label FROM transactions WHERE reference = 'REF001'", String.class);
        assertThat(label).isEqualTo("Purchase with spaces");
    }

    @Test
    void shouldMarkCsvFileAsFailed_whenCsvContentIsMalformed() {
        //Arrange
        jdbcTemplate.update(
            "INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)",
            "malformed.csv", "text/csv", new byte[]{(byte) 0xFF, (byte) 0xFE}, "PENDING"
        );

        //Act
        batchProcessingService.run();

        //Assert
        String fileStatus = jdbcTemplate.queryForObject("SELECT status FROM uploaded_files WHERE filename = 'malformed.csv'", String.class);
        assertThat(fileStatus).isEqualTo("FAILED");

        String logStatus = jdbcTemplate.queryForObject("SELECT status FROM import_logs ORDER BY started_at DESC LIMIT 1", String.class);
        assertThat(logStatus).isEqualTo("FAILED");
    }

    @Test
    void shouldProcessJsonFile_whenFileContainsValidAndInvalidRecords() {
        //Arrange
        String json = "[" +
            "{\"reference\":\"JSN001\",\"label\":\"Valid transaction\",\"amount\":250.00,\"currency\":\"EUR\",\"date\":\"2025-09-01\",\"category\":\"SHOPPING\"}," +
            "{\"reference\":\"JSN002\",\"label\":\"Future date\",\"amount\":100.00,\"currency\":\"EUR\",\"date\":\"2099-01-01\",\"category\":\"SHOPPING\"}," +
            "{\"reference\":\"\",\"label\":\"Empty reference\",\"amount\":50.00,\"currency\":\"EUR\",\"date\":\"2025-09-03\",\"category\":\"FOOD\"}," +
            "{\"reference\":\"JSN004\",\"label\":\"Valid without category\",\"amount\":75.00,\"currency\":\"USD\",\"date\":\"2025-09-05\",\"category\":null}" +
            "]";
        jdbcTemplate.update(
            "INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)",
            "test.json", "application/json", json.getBytes(StandardCharsets.UTF_8), "PENDING"
        );

        //Act
        int processedFiles = batchProcessingService.run();

        //Assert
        assertThat(processedFiles).isEqualTo(1);

        String fileStatus = jdbcTemplate.queryForObject("SELECT status FROM uploaded_files WHERE filename = 'test.json'", String.class);
        assertThat(fileStatus).isEqualTo("PROCESSED");

        Integer transactionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions", Integer.class);
        assertThat(transactionCount).isEqualTo(2);

        Integer rejectedCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rejected_transactions", Integer.class);
        assertThat(rejectedCount).isEqualTo(2);

        String logStatus = jdbcTemplate.queryForObject("SELECT status FROM import_logs ORDER BY started_at DESC LIMIT 1", String.class);
        assertThat(logStatus).isEqualTo("SUCCESS");
    }

    @Test
    void shouldSetUncategorized_whenJsonCategoryIsNull() {
        //Arrange
        String json = "[{\"reference\":\"JSN001\",\"label\":\"No category\",\"amount\":75.00,\"currency\":\"USD\",\"date\":\"2025-09-05\",\"category\":null}]";
        jdbcTemplate.update(
            "INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)",
            "no-category.json", "application/json", json.getBytes(StandardCharsets.UTF_8), "PENDING"
        );

        //Act
        batchProcessingService.run();

        //Assert
        String category = jdbcTemplate.queryForObject("SELECT category FROM transactions WHERE reference = 'JSN001'", String.class);
        assertThat(category).isEqualTo("UNCATEGORIZED");
    }

    @Test
    void shouldMarkJsonFileAsFailed_whenJsonContentIsMalformed() {
        //Arrange
        jdbcTemplate.update(
            "INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)",
            "malformed.json", "application/json", "not valid json".getBytes(StandardCharsets.UTF_8), "PENDING"
        );

        //Act
        batchProcessingService.run();

        //Assert
        String fileStatus = jdbcTemplate.queryForObject("SELECT status FROM uploaded_files WHERE filename = 'malformed.json'", String.class);
        assertThat(fileStatus).isEqualTo("FAILED");

        String logStatus = jdbcTemplate.queryForObject("SELECT status FROM import_logs ORDER BY started_at DESC LIMIT 1", String.class);
        assertThat(logStatus).isEqualTo("FAILED");
    }

    @Test
    void shouldNotReprocessFile_whenFileIsAlreadyProcessed() {
        //Arrange
        String json = "[{\"reference\":\"JSN001\",\"label\":\"Valid\",\"amount\":100.00,\"currency\":\"EUR\",\"date\":\"2025-01-01\",\"category\":\"INCOME\"}]";
        jdbcTemplate.update(
            "INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)",
            "already-processed.json", "application/json", json.getBytes(StandardCharsets.UTF_8), "PROCESSED"
        );

        //Act
        int processedFiles = batchProcessingService.run();

        //Assert
        assertThat(processedFiles).isZero();
        Integer transactionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions", Integer.class);
        assertThat(transactionCount).isZero();
    }
}
