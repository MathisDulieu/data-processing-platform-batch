package com.dataprocessing.batch.model;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record BatchStatusResponse(
    String status,
    Integer totalRecords,
    Integer validRecords,
    Integer rejectedRecords,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    String message
) {

    public static BatchStatusResponse from(ImportLog log) {
        return BatchStatusResponse.builder()
            .status(log.status())
            .totalRecords(log.totalRecords())
            .validRecords(log.validRecords())
            .rejectedRecords(log.rejectedRecords())
            .startedAt(log.startedAt())
            .finishedAt(log.finishedAt())
            .build();
    }

    public static BatchStatusResponse neverRun() {
        return BatchStatusResponse.builder()
            .status("never_run")
            .message("The batch has never been executed")
            .build();
    }
}
