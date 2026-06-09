package com.dataprocessing.batch.transformer;

import com.dataprocessing.batch.model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionTransformerTest {

    private final TransactionTransformer transactionTransformer = new TransactionTransformer();

    @Test
    void shouldNormaliseTransaction() {
        // Arrange
        Transaction transaction = Transaction.builder()
            .reference(" ref001 ")
            .label(" Amazon purchase ")
            .amount(BigDecimal.TEN)
            .currency(" eur ")
            .date(LocalDate.of(2025, 1, 1))
            .category(" shopping ")
            .uploadedFileId(1L)
            .build();

        // Act
        Transaction result = transactionTransformer.transform(transaction);

        // Assert
        Transaction expectedTransaction = Transaction.builder()
            .reference("REF001")
            .label("Amazon purchase")
            .amount(BigDecimal.TEN)
            .currency("EUR")
            .date(LocalDate.of(2025, 1, 1))
            .category("SHOPPING")
            .uploadedFileId(1L)
            .build();

        assertThat(result).isEqualTo(expectedTransaction);
    }

    @Test
    void shouldSetUncategorized_whenCategoryIsBlank() {
        // Arrange
        Transaction transaction = Transaction.builder()
            .reference("REF001")
            .label("Amazon purchase")
            .amount(BigDecimal.TEN)
            .currency("EUR")
            .date(LocalDate.of(2025, 1, 1))
            .category(" ")
            .uploadedFileId(1L)
            .build();

        // Act
        Transaction result = transactionTransformer.transform(transaction);

        // Assert
        Transaction expectedTransaction = Transaction.builder()
            .reference("REF001")
            .label("Amazon purchase")
            .amount(BigDecimal.TEN)
            .currency("EUR")
            .date(LocalDate.of(2025, 1, 1))
            .category("UNCATEGORIZED")
            .uploadedFileId(1L)
            .build();

        assertThat(result).isEqualTo(expectedTransaction);
    }

    @Test
    void shouldSetUncategorized_whenCategoryIsNull() {
        // Arrange
        Transaction transaction = Transaction.builder()
            .reference("REF001")
            .label("Amazon purchase")
            .amount(BigDecimal.TEN)
            .currency("EUR")
            .date(LocalDate.of(2025, 1, 1))
            .category(null)
            .uploadedFileId(1L)
            .build();

        // Act
        Transaction result = transactionTransformer.transform(transaction);

        // Assert
        Transaction expectedTransaction = Transaction.builder()
            .reference("REF001")
            .label("Amazon purchase")
            .amount(BigDecimal.TEN)
            .currency("EUR")
            .date(LocalDate.of(2025, 1, 1))
            .category("UNCATEGORIZED")
            .uploadedFileId(1L)
            .build();

        assertThat(result).isEqualTo(expectedTransaction);
    }
}
