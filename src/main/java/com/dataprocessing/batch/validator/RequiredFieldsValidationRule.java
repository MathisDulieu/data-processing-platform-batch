package com.dataprocessing.batch.validator;

import com.dataprocessing.batch.model.Transaction;
import com.dataprocessing.batch.model.ValidationError;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RequiredFieldsValidationRule implements ValidationRule {

    @Override
    public Optional<ValidationError> validate(final Transaction transaction) {
        if (this.isMissing(transaction.reference())) {
            return error("reference", "reference is required");
        }
        if (this.isMissing(transaction.label())) {
            return error("label", "label is required");
        }
        if (transaction.amount() == null) {
            return error("amount", "amount is required");
        }
        if (this.isMissing(transaction.currency())) {
            return error("currency", "currency is required");
        }
        if (transaction.date() == null) {
            return error("date", "date is required");
        }

        return Optional.empty();
    }

    private boolean isMissing(final String value) {
        return value == null || value.isBlank();
    }

    private Optional<ValidationError> error(final String field, final String message) {
        return Optional.of(new ValidationError(field, message));
    }
}
