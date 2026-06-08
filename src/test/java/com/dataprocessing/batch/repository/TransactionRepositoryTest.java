package com.dataprocessing.batch.repository;

import com.dataprocessing.batch.BatchApplication;
import com.dataprocessing.batch.BatchConfigurationTests;
import com.dataprocessing.batch.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
    BatchApplication.class,
    BatchConfigurationTests.class
})
@ActiveProfiles("test")
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE rejected_transactions, import_logs, transactions, uploaded_files RESTART IDENTITY CASCADE");
        jdbcTemplate.update("INSERT INTO uploaded_files (id, filename, mime_type, content, status) VALUES (?, ?, ?, ?, ?)", 1L, "test.csv", "text/csv", "content".getBytes(), "PENDING");
    }

    @Test
    void shouldSaveAllTransactions_whenSaveAllIsCalled() {
        //Arrange
        List<Transaction> transactions = List.of(
            new Transaction("REF001", "Amazon purchase", new BigDecimal("49.99"), "EUR", LocalDate.of(2025, 1, 15), "SHOPPING", 1L),
            new Transaction("REF002", "January salary", new BigDecimal("3000.00"), "EUR", LocalDate.of(2025, 1, 31), "INCOME", 1L)
        );

        //Act
        repository.saveAll(transactions);

        //Assert
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions", Integer.class);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldPersistTransactionFields_whenSaveAllIsCalled() {
        //Arrange
        List<Transaction> transactions = List.of(
            new Transaction("REF001", "Amazon purchase", new BigDecimal("49.99"), "EUR", LocalDate.of(2025, 1, 15), "SHOPPING", 1L)
        );

        //Act
        repository.saveAll(transactions);

        //Assert
        Transaction saved = jdbcTemplate.queryForObject(
            "SELECT reference, label, amount, currency, date, category, uploaded_file_id FROM transactions WHERE reference = 'REF001'",
            (rs, rowNum) -> new Transaction(
                rs.getString("reference"), rs.getString("label"),
                rs.getBigDecimal("amount"), rs.getString("currency"),
                rs.getDate("date").toLocalDate(), rs.getString("category"),
                rs.getLong("uploaded_file_id")
            )
        );
        assertThat(saved).isEqualTo(new Transaction("REF001", "Amazon purchase", new BigDecimal("49.99"), "EUR", LocalDate.of(2025, 1, 15), "SHOPPING", 1L));
    }

    @Test
    void shouldNotInsertAnything_whenTransactionListIsEmpty() {
        //Act
        repository.saveAll(List.of());

        //Assert
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions", Integer.class);
        assertThat(count).isZero();
    }
}
