package com.dataprocessing.batch.validator;

import com.dataprocessing.batch.model.Transaction;
import com.dataprocessing.batch.model.ValidationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DateValidationRuleTest {

    private final DateValidationRule dateValidationRule = new DateValidationRule();

    static Stream<LocalDate> validDates() {
        return Stream.of(
            LocalDate.of(2020, 6, 15),
            LocalDate.of(2019, 1, 1),
            LocalDate.of(2023, 12, 31),
            LocalDate.now()
        );
    }

    @ParameterizedTest
    @MethodSource("validDates")
    void shouldReturnEmpty_whenDateIsValid(LocalDate date) {
        // Arrange
        Transaction transaction = buildTransaction(date);

        // Act
        Optional<ValidationError> result = dateValidationRule.validate(transaction);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmpty_whenDateIsNull() {
        // Arrange
        Transaction transaction = buildTransaction(null);

        // Act
        Optional<ValidationError> result = dateValidationRule.validate(transaction);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnError_whenDateIsInTheFuture() {
        // Arrange
        Transaction transaction = buildTransaction(LocalDate.now().plusDays(1));

        // Act
        Optional<ValidationError> result = dateValidationRule.validate(transaction);

        // Assert
        assertThat(result).contains(new ValidationError("date", "date cannot be in the future"));
    }

    private static Transaction buildTransaction(LocalDate date) {
        return Transaction.builder()
            .reference("REF001")
            .label("Label")
            .amount(BigDecimal.TEN)
            .currency("EUR")
            .date(date)
            .category("SHOPPING")
            .uploadedFileId(1L)
            .build();
    }
}
