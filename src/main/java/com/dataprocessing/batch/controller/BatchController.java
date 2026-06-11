package com.dataprocessing.batch.controller;

import com.dataprocessing.batch.model.BatchResponse;
import com.dataprocessing.batch.model.BatchStatusResponse;
import com.dataprocessing.batch.service.BatchProcessingService;
import com.dataprocessing.batch.service.ImportLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
public class BatchController {

    private final BatchProcessingService batchProcessingService;
    private final ImportLogService importLogService;

    @PostMapping("/run")
    public ResponseEntity<BatchResponse> run() {
        log.info("Batch triggered at {}", LocalDateTime.now());
        final int processedFiles = batchProcessingService.run();
        log.info("Batch completed at {} — {} file(s) processed", LocalDateTime.now(), processedFiles);
        final BatchResponse response = new BatchResponse("completed", processedFiles);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<BatchStatusResponse> status() {
        return ResponseEntity.ok(
            importLogService.findLatest()
                .map(BatchStatusResponse::from)
                .orElseGet(BatchStatusResponse::neverRun)
        );
    }
}
