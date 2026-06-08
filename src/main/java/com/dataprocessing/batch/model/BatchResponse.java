package com.dataprocessing.batch.model;

public record BatchResponse(
    String status,
    int processedFiles
) {}
