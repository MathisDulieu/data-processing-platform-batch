package com.dataprocessing.batch.service;

import com.dataprocessing.batch.model.ImportLog;
import com.dataprocessing.batch.model.RejectedTransaction;
import com.dataprocessing.batch.repository.ImportLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportLogServiceTest {

    @Mock
    private ImportLogRepository importLogRepository;

    @InjectMocks
    private ImportLogService importLogService;

    @Test
    void shouldReturnLogId() {
        // Arrange
        when(importLogRepository.save(any(), any())).thenReturn(42L);

        // Act
        Long logId = importLogService.start(1L);

        // Assert
        verify(importLogRepository).save(eq(1L), any(LocalDateTime.class));
        verifyNoMoreInteractions(importLogRepository);

        assertThat(logId).isEqualTo(42L);
    }

    @Test
    void shouldUpdateLogWithSuccess() {
        // Arrange
        ArgumentCaptor<ImportLog> captor = ArgumentCaptor.forClass(ImportLog.class);

        // Act
        importLogService.success(1L, 2L, 10, 8, 2, emptyList());

        // Assert
        verify(importLogRepository).update(captor.capture());
        verifyNoMoreInteractions(importLogRepository);

        ImportLog savedImportLog = captor.getValue();

        assertThat(savedImportLog.id()).isEqualTo(1L);
        assertThat(savedImportLog.uploadedFileId()).isEqualTo(2L);
        assertThat(savedImportLog.status()).isEqualTo("SUCCESS");
        assertThat(savedImportLog.totalRecords()).isEqualTo(10);
        assertThat(savedImportLog.validRecords()).isEqualTo(8);
        assertThat(savedImportLog.rejectedRecords()).isEqualTo(2);
        assertThat(savedImportLog.finishedAt()).isNotNull();
    }

    @Test
    void shouldSaveRejectedTransactions_whenSuccessHasRejections() {
        // Arrange
        ArgumentCaptor<ImportLog> captor = ArgumentCaptor.forClass(ImportLog.class);
        List<RejectedTransaction> rejectedTransactions = List.of(
            new RejectedTransaction("REF001", "amount", "amount is required")
        );

        // Act
        importLogService.success(1L, 2L, 5, 4, 1, rejectedTransactions);

        // Assert
        InOrder inOrder = inOrder(importLogRepository);
        inOrder.verify(importLogRepository).update(captor.capture());
        inOrder.verify(importLogRepository).saveRejectedTransactions(1L, rejectedTransactions);
        inOrder.verifyNoMoreInteractions();

        ImportLog savedImportLog = captor.getValue();

        assertThat(savedImportLog.id()).isEqualTo(1L);
        assertThat(savedImportLog.uploadedFileId()).isEqualTo(2L);
        assertThat(savedImportLog.status()).isEqualTo("SUCCESS");
        assertThat(savedImportLog.totalRecords()).isEqualTo(5);
        assertThat(savedImportLog.validRecords()).isEqualTo(4);
        assertThat(savedImportLog.rejectedRecords()).isEqualTo(1);
        assertThat(savedImportLog.finishedAt()).isNotNull();
    }

    @Test
    void shouldUpdateLogWithFailed() {
        // Arrange
        ArgumentCaptor<ImportLog> captor = ArgumentCaptor.forClass(ImportLog.class);

        // Act
        importLogService.failure(1L, 2L, "Parsing error");

        // Assert
        verify(importLogRepository).update(captor.capture());
        verifyNoMoreInteractions(importLogRepository);

        ImportLog savedImportLog = captor.getValue();

        assertThat(savedImportLog.id()).isEqualTo(1L);
        assertThat(savedImportLog.uploadedFileId()).isEqualTo(2L);
        assertThat(savedImportLog.status()).isEqualTo("FAILED");
        assertThat(savedImportLog.totalRecords()).isEqualTo(0);
        assertThat(savedImportLog.validRecords()).isEqualTo(0);
        assertThat(savedImportLog.rejectedRecords()).isEqualTo(0);
        assertThat(savedImportLog.finishedAt()).isNotNull();
    }

    @Test
    void shouldReturnLatestLog() {
        // Arrange
        LocalDateTime startedAt = LocalDateTime.of(2025, 8, 15, 17, 45);
        LocalDateTime finishedAt = LocalDateTime.of(2025, 8, 15, 17, 46);

        ImportLog log = ImportLog.builder()
            .id(1L)
            .uploadedFileId(2L)
            .status("SUCCESS")
            .totalRecords(10)
            .validRecords(10)
            .rejectedRecords(0)
            .startedAt(startedAt)
            .finishedAt(finishedAt)
            .build();

        when(importLogRepository.findLatest()).thenReturn(Optional.of(log));

        // Act
        Optional<ImportLog> result = importLogService.findLatest();

        // Assert
        assertThat(result).isPresent().get().isEqualTo(log);
    }

    @Test
    void shouldReturnEmpty_whenNoLogExists() {
        // Arrange
        when(importLogRepository.findLatest()).thenReturn(Optional.empty());

        // Act
        Optional<ImportLog> result = importLogService.findLatest();

        // Assert
        assertThat(result).isEmpty();
    }
}
