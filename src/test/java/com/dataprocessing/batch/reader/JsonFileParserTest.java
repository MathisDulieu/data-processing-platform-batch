package com.dataprocessing.batch.reader;

import com.dataprocessing.batch.BatchConfigurationTests;
import com.dataprocessing.batch.model.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(BatchConfigurationTests.class)
class JsonFileParserTest {

    private final JsonFileParser parser;

    JsonFileParserTest(@Autowired ObjectMapper objectMapper) {
        this.parser = new JsonFileParser(objectMapper);
    }

    @Test
    void shouldReturnTransactions_whenJsonContentIsValid() {
        //Arrange
        String jsonTransactions = """
            [
              {
                "reference": "JSN001",
                "label": "Wire transfer",
                "amount": 500.00,
                "currency": "EUR",
                "date": "2025-02-10",
                "category": "INCOME"
              },
              {
                "reference": "JSN002",
                "label": "Electricity bill",
                "amount": 112.50,
                "currency": "EUR",
                "date": "2025-02-14",
                "category": "UTILITIES"
              }
            ]
            """;

        byte[] content = jsonTransactions.getBytes(StandardCharsets.UTF_8);

        //Act
        List<Transaction> result = parser.parse(content, 1L);

        //Assert
        Transaction expectedTransaction1 = Transaction.builder()
            .reference("JSN001")
            .label("Wire transfer")
            .amount(new BigDecimal("500.0"))
            .currency("EUR")
            .date(LocalDate.of(2025, 2, 10))
            .category("INCOME")
            .uploadedFileId(1L)
            .build();

        Transaction expectedTransaction2 = Transaction.builder()
            .reference("JSN002")
            .label("Electricity bill")
            .amount(new BigDecimal("112.5"))
            .currency("EUR")
            .date(LocalDate.of(2025, 2, 14))
            .category("UTILITIES")
            .uploadedFileId(1L)
            .build();

        assertThat(result).containsExactly(expectedTransaction1, expectedTransaction2);
    }

    @Test
    void shouldReturnEmptyList_whenJsonArrayIsEmpty() {
        //Arrange
        String jsonTransactions = "[]";

        byte[] content = jsonTransactions.getBytes(StandardCharsets.UTF_8);

        //Act
        List<Transaction> result = parser.parse(content, 1L);

        //Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnTransactionWithNullFields_whenJsonFieldsAreMissing() {
        //Arrange
        String jsonTransactions = """
            [
              {
                "reference": "JSN001",
                "label": "Label",
                "currency": "EUR",
                "date": "2025-02-10"
              }
            ]
            """;

        byte[] content = jsonTransactions.getBytes(StandardCharsets.UTF_8);

        //Act
        List<Transaction> result = parser.parse(content, 1L);

        //Assert
        Transaction expectedTransaction = Transaction.builder()
            .reference("JSN001")
            .label("Label")
            .currency("EUR")
            .date(LocalDate.of(2025, 2, 10))
            .uploadedFileId(1L)
            .build();

        assertThat(result).containsExactly(expectedTransaction);
    }

    @Test
    void shouldThrowIllegalArgumentException_whenJsonContentIsMalformed() {
        //Arrange
        String jsonTransactions = "not valid json";

        byte[] content = jsonTransactions.getBytes(StandardCharsets.UTF_8);

        //Act & Assert
        assertThatThrownBy(() -> parser.parse(content, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Failed to parse JSON content");
    }
}
