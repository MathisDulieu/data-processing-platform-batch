package com.dataprocessing.batch.repository;

import com.dataprocessing.batch.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TransactionRepository {

    private static final String INSERT_TRANSACTION = "INSERT INTO transactions (reference, label, amount, currency, date, category, uploaded_file_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public void saveAll(final List<Transaction> transactions) {
        final List<Object[]> batchArgs = transactions.stream()
                .map(this::toArgs)
                .toList();

        jdbcTemplate.batchUpdate(INSERT_TRANSACTION, batchArgs);
    }

    private Object[] toArgs(final Transaction transaction) {
        return new Object[]{
                transaction.reference(),
                transaction.label(),
                transaction.amount(),
                transaction.currency(),
                transaction.date(),
                transaction.category(),
                transaction.uploadedFileId()
        };
    }
}
