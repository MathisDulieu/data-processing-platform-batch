package com.dataprocessing.batch.model;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ImportLog(
        Long id,
        Long uploadedFileId,
        String status,
        Integer totalRecords,
        Integer validRecords,
        Integer rejectedRecords,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {}
