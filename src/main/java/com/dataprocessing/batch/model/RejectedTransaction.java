package com.dataprocessing.batch.model;

public record RejectedTransaction(
    String reference,
    String field,
    String reason
) {}
