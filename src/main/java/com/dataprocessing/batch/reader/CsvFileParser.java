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

    private static final List<String> REQUIRED_HEADERS = List.of("reference", "label", "amount", "currency", "date");

    @Override
    public List<Transaction> parse(final byte[] content, final Long uploadedFileId) {
        try (final CSVParser csvParser = this.buildCsvParser(content)) {
            this.validateHeaders(csvParser.getHeaderNames());
            return this.parseRecords(csvParser.getRecords(), uploadedFileId);
        } catch (IOException | NullPointerException e) {
            throw new IllegalArgumentException("Failed to parse CSV content", e);
        }
    }

    private void validateHeaders(final List<String> headers) {
        REQUIRED_HEADERS.stream()
            .filter(required -> !headers.contains(required))
            .findFirst()
            .ifPresent(required -> {
                throw new IllegalArgumentException("Missing required CSV column: " + required);
            });
    }

    private CSVParser buildCsvParser(final byte[] content) throws IOException {
        final InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8);
        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build()
                .parse(reader);
    }

    private List<Transaction> parseRecords(final List<CSVRecord> records, final Long uploadedFileId) {
        return records.stream()
            .map(record -> this.mapToTransaction(record, uploadedFileId))
            .toList();
    }

    private Transaction mapToTransaction(final CSVRecord record, final Long uploadedFileId) {
        return Transaction.builder()
            .reference(record.get("reference"))
            .label(record.get("label"))
            .amount(this.parseBigDecimal(record.get("amount")))
            .currency(record.get("currency"))
            .date(this.parseLocalDate(record.get("date")))
            .category(record.get("category"))
            .uploadedFileId(uploadedFileId)
            .build();
    }

    private BigDecimal parseBigDecimal(final String value) {
        if (value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseLocalDate(final String value) {
        if (value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
