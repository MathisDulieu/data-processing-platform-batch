package com.dataprocessing.batch.service;

import com.dataprocessing.batch.model.UploadedFile;
import com.dataprocessing.batch.repository.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BatchProcessingService {

    private final UploadedFileRepository uploadedFileRepository;
    private final FileProcessingService fileProcessingService;

    public int run() {
        List<UploadedFile> processableFiles = uploadedFileRepository.findAllProcessable();
        processableFiles.forEach(fileProcessingService::process);
        return processableFiles.size();
    }
}
