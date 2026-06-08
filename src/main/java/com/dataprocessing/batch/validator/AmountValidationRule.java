package com.dataprocessing.batch.validator;

import com.dataprocessing.batch.model.Transaction;
import com.dataprocessing.batch.model.ValidationError;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class AmountValidationRule implements ValidationRule {

    private static final BigDecimal MIN_AMOUNT = new BigDecimal("-1000000");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000");

    @Override
    public Optional<ValidationError> validate(Transaction transaction) {
        if (transaction.amount() == null) {
            return Optional.empty();
        }
        if (isBelowMinimum(transaction.amount())) {
            return Optional.of(new ValidationError("amount", "amount cannot be less than " + MIN_AMOUNT));
        }
        if (isAboveMaximum(transaction.amount())) {
            return Optional.of(new ValidationError("amount", "amount cannot exceed " + MAX_AMOUNT));
        }
        return Optional.empty();
    }

    private boolean isBelowMinimum(BigDecimal amount) {
        return amount.compareTo(MIN_AMOUNT) < 0;
    }

    private boolean isAboveMaximum(BigDecimal amount) {
        return amount.compareTo(MAX_AMOUNT) > 0;
    }
}