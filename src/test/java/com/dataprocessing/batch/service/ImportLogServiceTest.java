package com.dataprocessing.batch.service;

import com.dataprocessing.batch.model.ImportLog;
import com.dataprocessing.batch.model.RejectedTransaction;
import com.dataprocessing.batch.repository.ImportLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportLogServiceTest {

    @Mock
    private ImportLogRepository importLogRepository;

    @InjectMocks
    private ImportLogService importLogService;

    @Test
    void shouldReturnLogId_whenStartIsCalled() {
        //Arrange
        when(importLogRepository.save(eq(1L), any(LocalDateTime.class))).thenReturn(42L);

        //Act
        Long logId = importLogService.start(1L);

        //Assert
        assertThat(logId).isEqualTo(42L);
    }

    @Test
    void shouldUpdateLogWithSuccess_whenSuccessIsCalled() {
        //Arrange
        ArgumentCaptor<ImportLog> captor = ArgumentCaptor.forClass(ImportLog.class);

        //Act
        importLogService.success(1L, 2L, 10, 8, 2, List.of());

        //Assert
        verify(importLogRepository).update(captor.capture());
        ImportLog saved = captor.getValue();
        assertThat(saved.status()).isEqualTo("SUCCESS");
        assertThat(saved.totalRecords()).isEqualTo(10);
        assertThat(saved.validRecords()).isEqualTo(8);
        assertThat(saved.rejectedRecords()).isEqualTo(2);
        assertThat(saved.finishedAt()).isNotNull();
    }

    @Test
    void shouldSaveRejectedTransactions_whenSuccessHasRejections() {
        //Arrange
        List<RejectedTransaction> rejected = List.of(new RejectedTransaction("REF001", "amount", "amount is required"));

        //Act
        importLogService.success(1L, 2L, 5, 4, 1, rejected);

        //Assert
        verify(importLogRepository).saveRejectedTransactions(1L, rejected);
    }

    @Test
    void shouldNotSaveRejectedTransactions_whenSuccessHasNoRejections() {
        //Act
        importLogService.success(1L, 2L, 5, 5, 0, List.of());

        //Assert
        verify(importLogRepository, never()).saveRejectedTransactions(any(), any());
    }

    @Test
    void shouldUpdateLogWithFailed_whenFailureIsCalled() {
        //Arrange
        ArgumentCaptor<ImportLog> captor = ArgumentCaptor.forClass(ImportLog.class);

        //Act
        importLogService.failure(1L, 2L, "Parsing error");

        //Assert
        verify(importLogRepository).update(captor.capture());
        ImportLog saved = captor.getValue();
        assertThat(saved.status()).isEqualTo("FAILED");
        assertThat(saved.errorMessage()).isEqualTo("Parsing error");
        assertThat(saved.finishedAt()).isNotNull();
    }

    @Test
    void shouldReturnLatestLog_whenOneExists() {
        //Arrange
        ImportLog log = new ImportLog(1L, 2L, "SUCCESS", 10, 10, 0, null, LocalDateTime.now(), LocalDateTime.now());
        when(importLogRepository.findLatest()).thenReturn(Optional.of(log));

        //Act
        Optional<ImportLog> result = importLogService.findLatest();

        //Assert
        assertThat(result).contains(log);
    }

    @Test
    void shouldReturnEmpty_whenNoLogExists() {
        //Arrange
        when(importLogRepository.findLatest()).thenReturn(Optional.empty());

        //Act
        Optional<ImportLog> result = importLogService.findLatest();

        //Assert
        assertThat(result).isEmpty();
    }
}
