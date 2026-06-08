package com.dataprocessing.batch.reader;

import com.dataprocessing.batch.model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvFileParserTest {

    private final CsvFileParser parser = new CsvFileParser();

    @Test
    void shouldReturnTransactions_whenCsvContentIsValid() {
        //Arrange
        String csv = """
            reference,label,amount,currency,date,category
            REF001,Amazon purchase,49.99,EUR,2025-01-15,SHOPPING
            REF002,January salary,3000.00,EUR,2025-01-31,INCOME""";

        byte[] content = csv.getBytes(StandardCharsets.UTF_8);

        //Act
        List<Transaction> result = parser.parse(content, 1L);

        //Assert
        assertThat(result).containsExactly(
            new Transaction("REF001", "Amazon purchase", new BigDecimal("49.99"), "EUR", LocalDate.of(2025, 1, 15), "SHOPPING", 1L),
            new Transaction("REF002", "January salary", new BigDecimal("3000.00"), "EUR", LocalDate.of(2025, 1, 31), "INCOME", 1L)
        );
    }

    @Test
    void shouldReturnEmptyList_whenCsvHasOnlyHeader() {
        //Arrange
        String csv = """
            reference,label,amount,currency,date,category""";

        byte[] content = csv.getBytes(StandardCharsets.UTF_8);

        //Act
        List<Transaction> result = parser.parse(content, 1L);

        //Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnTransactionWithNullFields_whenCsvFieldsAreMissing() {
        //Arrange
        String csv = """
            reference,label,amount,currency,date,category
            REF001,Label,,EUR,,SHOPPING""";

        byte[] content = csv.getBytes(StandardCharsets.UTF_8);

        //Act
        List<Transaction> result = parser.parse(content, 1L);

        //Assert
        assertThat(result).containsExactly(
            new Transaction("REF001", "Label", null, "EUR", null, "SHOPPING", 1L)
        );
    }

    @Test
    void shouldThrowIllegalArgumentException_whenCsvContentIsNull() {
        //Act & Assert
        assertThatThrownBy(() -> parser.parse(null, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Failed to parse CSV content");
    }
}
