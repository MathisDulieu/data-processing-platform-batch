package com.dataprocessing.batch.repository;

import com.dataprocessing.batch.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
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
    void shouldSaveAllTransactions() {
        // Arrange
        Transaction transaction1 = Transaction.builder()
            .reference("REF001")
            .label("Amazon purchase")
            .amount(new BigDecimal("49.99"))
            .currency("EUR")
            .date(LocalDate.of(2025, 1, 15))
            .category("SHOPPING")
            .uploadedFileId(1L)
            .build();

        Transaction transaction2 = Transaction.builder()
            .reference("REF002")
            .label("January salary")
            .amount(new BigDecimal("3000.00"))
            .currency("EUR")
            .date(LocalDate.of(2025, 1, 31))
            .category("INCOME")
            .uploadedFileId(1L)
            .build();

        List<Transaction> transactions = List.of(transaction1, transaction2);

        // Act
        repository.saveAll(transactions);

        // Assert
        List<Transaction> savedTransactions = jdbcTemplate.query(
            "SELECT reference, label, amount, currency, date, category, uploaded_file_id FROM transactions ORDER BY id",
            (rs, rowNum) -> getTransaction(rs)
        );

        assertThat(savedTransactions).containsExactly(transaction1, transaction2);
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
