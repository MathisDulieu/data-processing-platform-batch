package com.dataprocessing.batch.controller;

import com.dataprocessing.batch.model.BatchResponse;
import com.dataprocessing.batch.model.BatchStatusResponse;
import com.dataprocessing.batch.service.BatchProcessingService;
import com.dataprocessing.batch.service.ImportLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
public class BatchController {

    private final BatchProcessingService batchProcessingService;
    private final ImportLogService importLogService;

    @PostMapping("/run")
    public ResponseEntity<BatchResponse> run() {
        int processedFiles = batchProcessingService.run();
        return ResponseEntity.ok(new BatchResponse("completed", processedFiles));
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
