package com.dataprocessing.batch.validator;

import com.dataprocessing.batch.model.Transaction;
import com.dataprocessing.batch.model.ValidationError;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
public class DateValidationRule implements ValidationRule {

    @Override
    public Optional<ValidationError> validate(final Transaction transaction) {
        if (transaction.date() == null) {
            return Optional.empty();
        }
        if (this.isFutureDate(transaction.date())) {
            return Optional.of(new ValidationError("date", "date cannot be in the future"));
        }

        return Optional.empty();
    }

    private boolean isFutureDate(final LocalDate date) {
        return date.isAfter(LocalDate.now());
    }
}
