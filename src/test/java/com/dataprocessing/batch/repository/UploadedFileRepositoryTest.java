package com.dataprocessing.batch.repository;

import com.dataprocessing.batch.model.UploadedFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UploadedFileRepositoryTest {

    @Autowired
    private UploadedFileRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE uploaded_files RESTART IDENTITY CASCADE");
    }

    @Test
    void shouldReturnProcessableFiles() {
        // Arrange
        jdbcTemplate.update("INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)", "pending.csv", "text/csv", "content".getBytes(), "PENDING");
        jdbcTemplate.update("INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)", "retry.csv", "text/csv", "content".getBytes(), "RETRY");
        jdbcTemplate.update("INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)", "processed.csv", "text/csv", "content".getBytes(), "PROCESSED");
        jdbcTemplate.update("INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)", "failed.csv", "text/csv", "content".getBytes(), "FAILED");

        // Act
        List<UploadedFile> result = repository.findAllProcessable();

        // Assert
        UploadedFile uploadedFile1 = UploadedFile.builder()
            .id(1L)
            .filename("pending.csv")
            .mimeType("text/csv")
            .content("content".getBytes())
            .build();

        UploadedFile uploadedFile2 = UploadedFile.builder()
            .id(2L)
            .filename("retry.csv")
            .mimeType("text/csv")
            .content("content".getBytes())
            .build();

        assertThat(result)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(uploadedFile1, uploadedFile2);
    }

    @Test
    void shouldUpdateFileStatus() {
        // Arrange
        jdbcTemplate.update("INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)", "file.csv", "text/csv", "content".getBytes(), "PENDING");
        Long fileId = jdbcTemplate.queryForObject("SELECT id FROM uploaded_files WHERE filename = 'file.csv'", Long.class);

        // Act
        repository.updateStatus(fileId, "PROCESSED");

        // Assert
        String status = jdbcTemplate.queryForObject("SELECT status FROM uploaded_files WHERE id = ?", String.class, fileId);
        assertThat(status).isEqualTo("PROCESSED");
    }
}
