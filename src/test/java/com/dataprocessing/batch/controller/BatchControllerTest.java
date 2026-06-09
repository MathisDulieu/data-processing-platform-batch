package com.dataprocessing.batch.controller;

import com.dataprocessing.batch.model.ImportLog;
import com.dataprocessing.batch.service.BatchProcessingService;
import com.dataprocessing.batch.service.ImportLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BatchController.class)
class BatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BatchProcessingService batchProcessingService;

    @MockitoBean
    private ImportLogService importLogService;

    @Test
    void shouldReturnCompletedResponse_whenBatchRunIsTriggered() throws Exception {
        // Arrange
        when(batchProcessingService.run()).thenReturn(3);

        // Act & Assert
        String expectedJsonResponse = """
            {
              "status": "completed",
              "processedFiles": 3
            }
            """;

        mockMvc.perform(post("/batch/run").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(expectedJsonResponse));
    }

    @Test
    void shouldReturnNeverRunResponse_whenNoLogExists() throws Exception {
        // Arrange
        when(importLogService.findLatest()).thenReturn(Optional.empty());

        // Act & Assert
        String expectedJsonResponse = """
            {
              "status": "never_run",
              "message": "The batch has never been executed"
            }
            """;

        mockMvc.perform(get("/batch/status"))
            .andExpect(status().isOk())
            .andExpect(content().json(expectedJsonResponse));
    }

    @Test
    void shouldReturnLatestLogStatus_whenLogExists() throws Exception {
        // Arrange
        ImportLog log = ImportLog.builder()
            .id(1L)
            .uploadedFileId(2L)
            .status("SUCCESS")
            .totalRecords(10)
            .validRecords(8)
            .rejectedRecords(2)
            .startedAt(LocalDateTime.of(2025, 1, 15, 10, 0))
            .finishedAt(LocalDateTime.of(2025, 1, 15, 10, 5))
            .build();

        when(importLogService.findLatest()).thenReturn(Optional.of(log));

        // Act & Assert
        String expectedJsonResponse = """
            {
              "status": "SUCCESS",
              "totalRecords": 10,
              "validRecords": 8,
              "rejectedRecords": 2
            }
            """;

        mockMvc.perform(get("/batch/status"))
            .andExpect(status().isOk())
            .andExpect(content().json(expectedJsonResponse));
    }
}
