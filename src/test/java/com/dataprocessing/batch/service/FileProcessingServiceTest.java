package com.dataprocessing.batch.service;

import com.dataprocessing.batch.model.RejectedTransaction;
import com.dataprocessing.batch.model.Transaction;
import com.dataprocessing.batch.model.UploadedFile;
import com.dataprocessing.batch.model.ValidationError;
import com.dataprocessing.batch.reader.FileParser;
import com.dataprocessing.batch.reader.FileParserFactory;
import com.dataprocessing.batch.repository.TransactionRepository;
import com.dataprocessing.batch.repository.UploadedFileRepository;
import com.dataprocessing.batch.transformer.TransactionTransformer;
import com.dataprocessing.batch.validator.ValidationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileProcessingServiceTest {

    @Mock private FileParserFactory fileParserFactory;
    @Mock private ValidationRule validationRule;
    @Mock private TransactionTransformer transactionTransformer;
    @Mock private TransactionRepository transactionRepository;
    @Mock private UploadedFileRepository uploadedFileRepository;
    @Mock private ImportLogService importLogService;
    @Mock private FileParser fileParser;

    private FileProcessingService fileProcessingService;

    private final Transaction validTransaction = new Transaction("REF001", "Label", BigDecimal.TEN, "EUR", LocalDate.of(2025, 1, 1), "SHOPPING", 1L);
    private final UploadedFile uploadedFile = new UploadedFile(1L, "test.csv", "text/csv", new byte[0]);

    @BeforeEach
    void setUp() {
        fileProcessingService = new FileProcessingService(
            fileParserFactory,
            List.of(validationRule),
            transactionTransformer,
            transactionRepository,
            uploadedFileRepository,
            importLogService
        );
    }

    @Test
    void shouldProcessFileAndMarkAsProcessed_whenAllTransactionsAreValid() {
        //Arrange
        when(importLogService.start(1L)).thenReturn(10L);
        when(fileParserFactory.getParser("text/csv")).thenReturn(fileParser);
        when(fileParser.parse(any(), eq(1L))).thenReturn(List.of(validTransaction));
        when(validationRule.validate(validTransaction)).thenReturn(Optional.empty());
        when(transactionTransformer.transform(validTransaction)).thenReturn(validTransaction);

        //Act
        fileProcessingService.process(uploadedFile);

        //Assert
        verify(transactionRepository).saveAll(List.of(validTransaction));
        verify(uploadedFileRepository).updateStatus(1L, "PROCESSED");
        verify(importLogService).success(10L, 1L, 1, 1, 0, List.of());
    }

    @Test
    void shouldMarkFileAsProcessedWithRejections_whenSomeTransactionsAreInvalid() {
        //Arrange
        Transaction invalidTransaction = new Transaction("REF002", "Label", null, "EUR", LocalDate.of(2025, 1, 1), "SHOPPING", 1L);
        ValidationError error = new ValidationError("amount", "amount is required");
        when(importLogService.start(1L)).thenReturn(10L);
        when(fileParserFactory.getParser("text/csv")).thenReturn(fileParser);
        when(fileParser.parse(any(), eq(1L))).thenReturn(List.of(validTransaction, invalidTransaction));
        when(validationRule.validate(validTransaction)).thenReturn(Optional.empty());
        when(validationRule.validate(invalidTransaction)).thenReturn(Optional.of(error));
        when(transactionTransformer.transform(validTransaction)).thenReturn(validTransaction);

        //Act
        fileProcessingService.process(uploadedFile);

        //Assert
        verify(transactionRepository).saveAll(List.of(validTransaction));
        verify(uploadedFileRepository).updateStatus(1L, "PROCESSED");
        verify(importLogService).success(10L, 1L, 2, 1, 1,
            List.of(new RejectedTransaction("REF002", "amount", "amount is required")));
    }

    @Test
    void shouldMarkFileAsFailed_whenParsingThrowsException() {
        //Arrange
        when(importLogService.start(1L)).thenReturn(10L);
        when(fileParserFactory.getParser("text/csv")).thenReturn(fileParser);
        when(fileParser.parse(any(), eq(1L))).thenThrow(new IllegalArgumentException("Failed to parse CSV content"));

        //Act
        fileProcessingService.process(uploadedFile);

        //Assert
        verify(uploadedFileRepository).updateStatus(1L, "FAILED");
        verify(importLogService).failure(10L, 1L, "Failed to parse CSV content");
        verify(transactionRepository, never()).saveAll(any());
    }
}
