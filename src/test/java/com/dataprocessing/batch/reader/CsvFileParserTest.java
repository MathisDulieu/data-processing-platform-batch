package com.dataprocessing.batch.reader;

import com.dataprocessing.batch.model.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvFileParserTest {

    private final CsvFileParser csvFileParser = new CsvFileParser();

    @Test
    void shouldReturnTransactions_whenCsvContentIsValid() {
        // Arrange
        String csv = """
            reference,label,amount,currency,date,category
            REF001,Amazon purchase,49.99,EUR,2025-01-15,SHOPPING
            REF002,January salary,3000.00,EUR,2025-01-31,INCOME""";

        byte[] content = csv.getBytes(StandardCharsets.UTF_8);

        // Act
        List<Transaction> result = csvFileParser.parse(content, 1L);

        // Assert
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

        assertThat(result).containsExactly(transaction1, transaction2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"reference", "label", "amount", "currency", "date"})
    void shouldThrowIllegalArgumentException_whenRequiredHeaderIsMissing(String missingHeader) {
        // Arrange
        String headers = "reference,label,amount,currency,date,category"
            .replace(missingHeader + ",", "")
            .replace("," + missingHeader, "");

        String csv = headers + """
            REF001,Amazon purchase,49.99,EUR,2025-01-15,SHOPPING""";

        byte[] content = csv.getBytes(StandardCharsets.UTF_8);

        // Act & Assert
        assertThatThrownBy(() -> csvFileParser.parse(content, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Missing required CSV column: " + missingHeader);
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "NOT_A_NUMBER"})
    void shouldReturnNullAmount_whenAmountIsNotValid(String amount) {
        // Arrange
        String csv = """
        reference,label,amount,currency,date,category
        REF001,Label,%s,EUR,2025-01-15,SHOPPING""".formatted(amount);

        byte[] content = csv.getBytes(StandardCharsets.UTF_8);

        // Act
        List<Transaction> result = csvFileParser.parse(content, 1L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().amount()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "NOT_A_DATE"})
    void shouldReturnNullDate_whenDateIsNotValid(String date) {
        // Arrange
        String csv = """
        reference,label,amount,currency,date,category
        REF001,Label,49.99,EUR,%s,SHOPPING""".formatted(date);

        byte[] content = csv.getBytes(StandardCharsets.UTF_8);

        // Act
        List<Transaction> result = csvFileParser.parse(content, 1L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().date()).isNull();
    }

    @Test
    void shouldThrowIllegalArgumentException_whenCsvContentIsNull() {
        // Arrange

        // Act & Assert
        assertThatThrownBy(() -> csvFileParser.parse(null, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Failed to parse CSV content")
            .hasCauseInstanceOf(NullPointerException.class);
    }
}
