package com.dataprocessing.batch.transformer;

import com.dataprocessing.batch.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionTransformer {

    public Transaction transform(Transaction transaction) {
        return Transaction.builder()
            .reference(normaliseReference(transaction.reference()))
            .label(normaliseLabel(transaction.label()))
            .amount(transaction.amount())
            .currency(normaliseCurrency(transaction.currency()))
            .date(transaction.date())
            .category(normaliseCategory(transaction.category()))
            .uploadedFileId(transaction.uploadedFileId())
            .build();
    }

    private String normaliseReference(String reference) {
        return reference.trim().toUpperCase();
    }

    private String normaliseLabel(String label) {
        return label.trim();
    }

    private String normaliseCurrency(String currency) {
        return currency.trim().toUpperCase();
    }

    private String normaliseCategory(String category) {
        if (category == null || category.isBlank()) {
            return "UNCATEGORIZED";
        }
        return category.trim().toUpperCase();
    }
}
