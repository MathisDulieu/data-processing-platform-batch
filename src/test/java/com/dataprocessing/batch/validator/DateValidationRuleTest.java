package com.dataprocessing.batch.validator;

import com.dataprocessing.batch.model.Transaction;
import com.dataprocessing.batch.model.ValidationError;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DateValidationRuleTest {

    private final DateValidationRule rule = new DateValidationRule();

    @Test
    void shouldReturnEmpty_whenDateIsInThePast() {
        //Arrange
        Transaction transaction = buildTransaction(LocalDate.of(2020, 6, 15));

        //Act
        Optional<ValidationError> result = rule.validate(transaction);

        //Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmpty_whenDateIsToday() {
        //Arrange
        Transaction transaction = buildTransaction(LocalDate.now());

        //Act
        Optional<ValidationError> result = rule.validate(transaction);

        //Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmpty_whenDateIsNull() {
        //Arrange
        Transaction transaction = buildTransaction(null);

        //Act
        Optional<ValidationError> result = rule.validate(transaction);

        //Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnError_whenDateIsInTheFuture() {
        //Arrange
        Transaction transaction = buildTransaction(LocalDate.now().plusDays(1));

        //Act
        Optional<ValidationError> result = rule.validate(transaction);

        //Assert
        assertThat(result).contains(new ValidationError("date", "date cannot be in the future"));
    }

    private Transaction buildTransaction(LocalDate date) {
        return new Transaction("REF001", "Label", BigDecimal.TEN, "EUR", date, "SHOPPING", 1L);
    }
}
