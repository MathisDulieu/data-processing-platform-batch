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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileProcessingServiceTest {

    @Mock
    private FileParserFactory fileParserFactory;

    @Mock
    private ValidationRule validationRule;

    @Mock
    private TransactionTransformer transactionTransformer;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UploadedFileRepository uploadedFileRepository;

    @Mock
    private ImportLogService importLogService;

    @Mock
    private FileParser fileParser;

    private final UploadedFile uploadedFile = getUploadedFile();

    private FileProcessingService fileProcessingService;

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
    void shouldProcessFileAndMarkAsProcessed() {
        // Arrange
        Transaction validTransaction = Transaction.builder()
            .reference("REF001")
            .label("Label")
            .amount(BigDecimal.TEN)
            .currency("EUR")
            .date(LocalDate.of(2025, 1, 1))
            .category("SHOPPING")
            .uploadedFileId(1L)
            .build();

        Transaction invalidTransaction = Transaction.builder()
            .reference("REF002")
            .label("Label")
            .currency("EUR")
            .date(LocalDate.of(2025, 1, 1))
            .category("SHOPPING")
            .uploadedFileId(1L)
            .build();

        ValidationError validationError = new ValidationError("amount", "amount is required");

        when(importLogService.start(any())).thenReturn(10L);
        when(fileParserFactory.getParser(anyString())).thenReturn(fileParser);
        when(fileParser.parse(any(), any())).thenReturn(List.of(validTransaction, invalidTransaction));
        when(validationRule.validate(validTransaction)).thenReturn(Optional.empty());
        when(validationRule.validate(invalidTransaction)).thenReturn(Optional.of(validationError));
        when(transactionTransformer.transform(validTransaction)).thenReturn(validTransaction);

        // Act
        fileProcessingService.process(uploadedFile);

        // Assert
        RejectedTransaction expectedRejectedTransaction = new RejectedTransaction("REF002", "amount", "amount is required");

        InOrder inOrder = inOrder(importLogService, fileParserFactory, fileParser, validationRule, transactionTransformer, transactionRepository, uploadedFileRepository);
        inOrder.verify(importLogService).start(1L);
        inOrder.verify(fileParserFactory).getParser("text/csv");
        inOrder.verify(fileParser).parse(uploadedFile.content(), 1L);
        inOrder.verify(validationRule, times(2)).validate(validTransaction);
        inOrder.verify(validationRule).validate(invalidTransaction);
        inOrder.verify(transactionTransformer).transform(validTransaction);
        inOrder.verify(transactionRepository).saveAll(List.of(validTransaction));
        inOrder.verify(uploadedFileRepository).updateStatus(1L, "PROCESSED");
        inOrder.verify(importLogService).success(10L, 1L, 2, 1, 1, List.of(expectedRejectedTransaction));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void shouldMarkFileAsFailed_whenExceptionIsThrown() {
        // Arrange
        when(importLogService.start(any())).thenReturn(10L);
        when(fileParserFactory.getParser(anyString())).thenReturn(fileParser);
        when(fileParser.parse(any(), any())).thenThrow(new IllegalArgumentException("Failed to parse CSV content"));

        // Act
        fileProcessingService.process(uploadedFile);

        // Assert
        InOrder inOrder = inOrder(importLogService, fileParserFactory, fileParser, uploadedFileRepository, importLogService);
        inOrder.verify(importLogService).start(1L);
        inOrder.verify(fileParserFactory).getParser("text/csv");
        inOrder.verify(fileParser).parse(uploadedFile.content(), 1L);
        inOrder.verify(uploadedFileRepository).updateStatus(1L, "FAILED");
        inOrder.verify(importLogService).failure(10L, 1L, "Failed to parse CSV content");
        inOrder.verifyNoMoreInteractions();

        verifyNoInteractions(transactionRepository);
    }

    private static UploadedFile getUploadedFile() {
        return UploadedFile.builder()
            .id(1L)
            .filename("test.csv")
            .mimeType("text/csv")
            .content(new byte[0])
            .build();
    }
}
