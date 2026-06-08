package com.dataprocessing.batch.model;

import lombok.Builder;

@Builder
public record UploadedFile(
        Long id,
        String filename,
        String mimeType,
        byte[] content
) {}
