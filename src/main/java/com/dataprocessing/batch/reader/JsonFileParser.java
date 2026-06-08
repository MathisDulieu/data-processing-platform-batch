package com.dataprocessing.batch.reader;

import com.dataprocessing.batch.model.Transaction;
import com.dataprocessing.batch.model.TransactionRaw;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JsonFileParser implements FileParser {

    private final ObjectMapper objectMapper;

    @Override
    public List<Transaction> parse(byte[] content, Long uploadedFileId) {
        try {
            List<Map<String, Object>> rawRecords = objectMapper.readValue(content, new TypeReference<>() {});
            return rawRecords.stream()
                .map(raw -> mapToTransaction(raw, uploadedFileId))
                .toList();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse JSON content", e);
        }
    }

    private Transaction mapToTransaction(Map<String, Object> raw, Long uploadedFileId) {
        TransactionRaw transactionRaw = objectMapper.convertValue(raw, TransactionRaw.class);
        return Transaction.builder()
            .reference(transactionRaw.reference())
            .label(transactionRaw.label())
            .amount(transactionRaw.amount())
            .currency(transactionRaw.currency())
            .date(transactionRaw.date())
            .category(transactionRaw.category())
            .uploadedFileId(uploadedFileId)
            .build();
    }
}
