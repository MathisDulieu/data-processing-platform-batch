package com.dataprocessing.batch.model;

public record ValidationError(
        String field,
        String message
) {}