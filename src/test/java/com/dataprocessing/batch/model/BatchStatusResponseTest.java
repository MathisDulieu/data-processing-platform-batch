package com.dataprocessing.batch.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BatchStatusResponseTest {

    @Test
    void shouldBuildResponseFromImportLog_whenLogIsProvided() {
        //Arrange
        LocalDateTime startedAt = LocalDateTime.of(2025, 1, 15, 10, 0);
        LocalDateTime finishedAt = LocalDateTime.of(2025, 1, 15, 10, 5);
        ImportLog log = new ImportLog(1L, 2L, "SUCCESS", 10, 8, 2, null, startedAt, finishedAt);

        //Act
        BatchStatusResponse response = BatchStatusResponse.from(log);

        //Assert
        BatchStatusResponse expectedBatchStatusResponse = BatchStatusResponse.builder()
            .status("SUCCESS")
            .totalRecords(10)
            .validRecords(8)
            .rejectedRecords(2)
            .startedAt(startedAt)
            .finishedAt(finishedAt)
            .build();

        assertThat(response).isEqualTo(expectedBatchStatusResponse);
    }

    @Test
    void shouldBuildNeverRunResponse_whenNoLogExists() {
        //Arrange

        //Act
        BatchStatusResponse batchStatusResponse = BatchStatusResponse.neverRun();

        //Assert
        BatchStatusResponse expectedBatchStatusResponse = BatchStatusResponse.builder()
            .status("never_run")
            .message("The batch has never been executed")
            .build();

        assertThat(batchStatusResponse).isEqualTo(expectedBatchStatusResponse);
    }
}
