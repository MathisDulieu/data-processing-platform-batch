package com.dataprocessing.batch.validator;

import com.dataprocessing.batch.model.Transaction;
import com.dataprocessing.batch.model.ValidationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AmountValidationRuleTest {

    private final AmountValidationRule amountValidationRule = new AmountValidationRule();

    @ParameterizedTest
    @ValueSource(strings = {"0", "100.00", "1000000", "-1000000", "-150.50"})
    void shouldReturnEmpty_whenAmountIsWithinRange(String amount) {
        // Arrange
        Transaction transaction = buildTransaction(new BigDecimal(amount));

        // Act
        Optional<ValidationError> result = amountValidationRule.validate(transaction);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmpty_whenAmountIsNull() {
        // Arrange
        Transaction transaction = buildTransaction(null);

        // Act
        Optional<ValidationError> result = amountValidationRule.validate(transaction);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnError_whenAmountExceedsMaximum() {
        // Arrange
        Transaction transaction = buildTransaction(new BigDecimal("1000000.01"));

        // Act
        Optional<ValidationError> result = amountValidationRule.validate(transaction);

        // Assert
        assertThat(result).contains(new ValidationError("amount", "amount cannot exceed 1000000"));
    }

    @Test
    void shouldReturnError_whenAmountIsBelowMinimum() {
        // Arrange
        Transaction transaction = buildTransaction(new BigDecimal("-1000000.01"));

        // Act
        Optional<ValidationError> result = amountValidationRule.validate(transaction);

        // Assert
        assertThat(result).contains(new ValidationError("amount", "amount cannot be less than -1000000"));
    }

    private static Transaction buildTransaction(BigDecimal amount) {
        return Transaction.builder()
            .reference("REF001")
            .label("Label")
            .amount(amount)
            .currency("EUR")
            .date(LocalDate.of(2025, 1, 1))
            .category("SHOPPING")
            .uploadedFileId(1L)
            .build();
    }
}
