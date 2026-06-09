package com.dataprocessing.batch.service;

import com.dataprocessing.batch.model.ImportLog;
import com.dataprocessing.batch.model.RejectedTransaction;
import com.dataprocessing.batch.repository.ImportLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ImportLogService {

    private final ImportLogRepository importLogRepository;

    public Long start(final Long uploadedFileId) {
        return importLogRepository.save(uploadedFileId, LocalDateTime.now());
    }

    public void success(final Long logId, final Long uploadedFileId, final int totalRecords, final int validRecords,
                        final int rejectedRecords, final List<RejectedTransaction> rejectedTransactions) {

        final ImportLog importLog = ImportLog.builder()
            .id(logId)
            .uploadedFileId(uploadedFileId)
            .status("SUCCESS")
            .totalRecords(totalRecords)
            .validRecords(validRecords)
            .rejectedRecords(rejectedRecords)
            .errorMessage(null)
            .startedAt(null)
            .finishedAt(LocalDateTime.now())
            .build();

        importLogRepository.update(importLog);

        if (!rejectedTransactions.isEmpty()) {
            importLogRepository.saveRejectedTransactions(logId, rejectedTransactions);
        }
    }

    public void failure(final Long logId, final Long uploadedFileId, final String errorMessage) {
        final ImportLog importLog = ImportLog.builder()
            .id(logId)
            .uploadedFileId(uploadedFileId)
            .status("FAILED")
            .totalRecords(0)
            .validRecords(0)
            .rejectedRecords(0)
            .errorMessage(errorMessage)
            .startedAt(null)
            .finishedAt(LocalDateTime.now())
            .build();

        importLogRepository.update(importLog);
    }

    public Optional<ImportLog> findLatest() {
        return importLogRepository.findLatest();
    }

}
