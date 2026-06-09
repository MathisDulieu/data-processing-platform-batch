package com.dataprocessing.batch.transformer;

import com.dataprocessing.batch.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionTransformer {

    public Transaction transform(final Transaction transaction) {
        return Transaction.builder()
            .reference(this.normaliseReference(transaction.reference()))
            .currency(this.normaliseCurrency(transaction.currency()))
            .category(this.normaliseCategory(transaction.category()))
            .label(this.normaliseLabel(transaction.label()))
            .amount(transaction.amount())
            .date(transaction.date())
            .uploadedFileId(transaction.uploadedFileId())
            .build();
    }

    private String normaliseReference(final String reference) {
        return reference.trim().toUpperCase();
    }

    private String normaliseLabel(final String label) {
        return label.trim();
    }

    private String normaliseCurrency(final String currency) {
        return currency.trim().toUpperCase();
    }

    private String normaliseCategory(final String category) {
        if (category == null || category.isBlank()) {
            return "UNCATEGORIZED";
        }

        return category.trim().toUpperCase();
    }
}
