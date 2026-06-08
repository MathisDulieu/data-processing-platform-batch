package com.dataprocessing.batch.service;

import com.dataprocessing.batch.model.UploadedFile;
import com.dataprocessing.batch.repository.UploadedFileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchProcessingServiceTest {

    @Mock
    private UploadedFileRepository uploadedFileRepository;

    @Mock
    private FileProcessingService fileProcessingService;

    @InjectMocks
    private BatchProcessingService batchProcessingService;

    @Test
    void shouldReturnZero_whenNoPendingFilesExist() {
        //Arrange
        when(uploadedFileRepository.findAllProcessable()).thenReturn(List.of());

        //Act
        int result = batchProcessingService.run();

        //Assert
        assertThat(result).isZero();
        verify(fileProcessingService, never()).process(any());
    }

    @Test
    void shouldProcessAllFilesAndReturnCount_whenPendingFilesExist() {
        //Arrange
        UploadedFile file1 = new UploadedFile(1L, "file1.csv", "text/csv", new byte[0]);
        UploadedFile file2 = new UploadedFile(2L, "file2.json", "application/json", new byte[0]);
        when(uploadedFileRepository.findAllProcessable()).thenReturn(List.of(file1, file2));

        //Act
        int result = batchProcessingService.run();

        //Assert
        assertThat(result).isEqualTo(2);
        verify(fileProcessingService).process(file1);
        verify(fileProcessingService).process(file2);
    }
}
