package com.dataprocessing.batch.transformer;

import com.dataprocessing.batch.model.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionTransformerTest {

    private final TransactionTransformer transformer = new TransactionTransformer();

    @Test
    void shouldNormaliseTransaction_whenAllFieldsArePresent() {
        //Arrange
        Transaction transaction = new Transaction("ref001", "  Amazon purchase  ", BigDecimal.TEN, "eur", LocalDate.of(2025, 1, 1), "shopping", 1L);

        //Act
        Transaction result = transformer.transform(transaction);

        //Assert
        assertThat(result).isEqualTo(new Transaction("REF001", "Amazon purchase", BigDecimal.TEN, "EUR", LocalDate.of(2025, 1, 1), "SHOPPING", 1L));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void shouldSetUncategorized_whenCategoryIsBlankOrNull(String category) {
        //Arrange
        Transaction transaction = new Transaction("REF001", "Label", BigDecimal.TEN, "EUR", LocalDate.of(2025, 1, 1), category, 1L);

        //Act
        Transaction result = transformer.transform(transaction);

        //Assert
        assertThat(result.category()).isEqualTo("UNCATEGORIZED");
    }
}
