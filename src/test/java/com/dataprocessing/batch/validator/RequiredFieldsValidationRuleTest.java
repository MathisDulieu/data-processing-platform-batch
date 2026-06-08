package com.dataprocessing.batch.validator;

import com.dataprocessing.batch.model.Transaction;
import com.dataprocessing.batch.model.ValidationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RequiredFieldsValidationRuleTest {

    private final RequiredFieldsValidationRule rule = new RequiredFieldsValidationRule();

    @Test
    void shouldReturnEmpty_whenAllFieldsArePresent() {
        //Arrange
        Transaction transaction = new Transaction("REF001", "Label", BigDecimal.TEN, "EUR", LocalDate.of(2025, 1, 1), "SHOPPING", 1L);

        //Act
        Optional<ValidationError> result = rule.validate(transaction);

        //Assert
        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("missingFieldCases")
    void shouldReturnError_whenRequiredFieldIsMissing(Transaction transaction, ValidationError expectedError) {
        //Act
        Optional<ValidationError> result = rule.validate(transaction);

        //Assert
        assertThat(result).contains(expectedError);
    }

    private static Stream<Arguments> missingFieldCases() {
        return Stream.of(
            Arguments.of(
                new Transaction(null, "Label", BigDecimal.TEN, "EUR", LocalDate.of(2025, 1, 1), "SHOPPING", 1L),
                new ValidationError("reference", "reference is required")
            ),
            Arguments.of(
                new Transaction("  ", "Label", BigDecimal.TEN, "EUR", LocalDate.of(2025, 1, 1), "SHOPPING", 1L),
                new ValidationError("reference", "reference is required")
            ),
            Arguments.of(
                new Transaction("REF001", null, BigDecimal.TEN, "EUR", LocalDate.of(2025, 1, 1), "SHOPPING", 1L),
                new ValidationError("label", "label is required")
            ),
            Arguments.of(
                new Transaction("REF001", "Label", null, "EUR", LocalDate.of(2025, 1, 1), "SHOPPING", 1L),
                new ValidationError("amount", "amount is required")
            ),
            Arguments.of(
                new Transaction("REF001", "Label", BigDecimal.TEN, null, LocalDate.of(2025, 1, 1), "SHOPPING", 1L),
                new ValidationError("currency", "currency is required")
            ),
            Arguments.of(
                new Transaction("REF001", "Label", BigDecimal.TEN, "EUR", null, "SHOPPING", 1L),
                new ValidationError("date", "date is required")
            )
        );
    }
}
