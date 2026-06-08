package com.dataprocessing.batch.validator;

import com.dataprocessing.batch.model.Transaction;
import com.dataprocessing.batch.model.ValidationError;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RequiredFieldsValidationRule implements ValidationRule {

    @Override
    public Optional<ValidationError> validate(Transaction transaction) {
        if (isMissing(transaction.reference())) {
            return error("reference", "reference is required");
        }
        if (isMissing(transaction.label())) {
            return error("label", "label is required");
        }
        if (transaction.amount() == null) {
            return error("amount", "amount is required");
        }
        if (isMissing(transaction.currency())) {
            return error("currency", "currency is required");
        }
        if (transaction.date() == null) {
            return error("date", "date is required");
        }
        return Optional.empty();
    }

    private boolean isMissing(String value) {
        return value == null || value.isBlank();
    }

    private Optional<ValidationError> error(String field, String message) {
        return Optional.of(new ValidationError(field, message));
    }
}