package com.dataprocessing.batch.validator;

import com.dataprocessing.batch.model.Transaction;
import com.dataprocessing.batch.model.ValidationError;

import java.util.Optional;

public interface ValidationRule {

    Optional<ValidationError> validate(Transaction transaction);
}