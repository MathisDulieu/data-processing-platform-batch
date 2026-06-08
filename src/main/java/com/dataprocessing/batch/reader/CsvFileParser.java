package com.dataprocessing.batch.reader;

import com.dataprocessing.batch.model.Transaction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Component
public class CsvFileParser implements FileParser {

    @Override
    public List<Transaction> parse(byte[] content, Long uploadedFileId) {
        try (CSVParser csvParser = buildCsvParser(content)) {
            return parseRecords(csvParser.getRecords(), uploadedFileId);
        } catch (IOException | NullPointerException e) {
            throw new IllegalArgumentException("Failed to parse CSV content", e);
        }
    }

    private CSVParser buildCsvParser(byte[] content) throws IOException {
        InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8);
        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build()
                .parse(reader);
    }

    private List<Transaction> parseRecords(List<CSVRecord> records, Long uploadedFileId) {
        return records.stream()
            .map(record -> mapToTransaction(record, uploadedFileId))
            .toList();
    }

    private Transaction mapToTransaction(CSVRecord record, Long uploadedFileId) {
        return Transaction.builder()
            .reference(record.get("reference"))
            .label(record.get("label"))
            .amount(parseBigDecimal(record.get("amount")))
            .currency(record.get("currency"))
            .date(parseLocalDate(record.get("date")))
            .category(record.get("category"))
            .uploadedFileId(uploadedFileId)
            .build();
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseLocalDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
