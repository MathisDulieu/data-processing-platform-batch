package com.dataprocessing.batch.reader;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileParserFactory {

    private final CsvFileParser csvFileParser;
    private final JsonFileParser jsonFileParser;

    public FileParser getParser(String mimeType) {
        return switch (mimeType) {
            case "text/csv", "application/csv" -> csvFileParser;
            case "application/json" -> jsonFileParser;
            default -> throw new IllegalArgumentException("Unsupported file type: " + mimeType);
        };
    }
}
