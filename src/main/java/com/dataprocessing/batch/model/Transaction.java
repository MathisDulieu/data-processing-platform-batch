package com.dataprocessing.batch.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record Transaction(
        String reference,
        String label,
        BigDecimal amount,
        String currency,
        LocalDate date,
        String category,
        Long uploadedFileId
) {}
