package com.dataprocessing.batch.repository;

import com.dataprocessing.batch.BatchApplication;
import com.dataprocessing.batch.BatchConfigurationTests;
import com.dataprocessing.batch.model.UploadedFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
    BatchApplication.class,
    BatchConfigurationTests.class
})
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
    void shouldReturnPendingAndRetryFiles_whenFindAllProcessableIsCalled() {
        //Arrange
        jdbcTemplate.update("INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)", "pending.csv", "text/csv", "content".getBytes(), "PENDING");
        jdbcTemplate.update("INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)", "retry.csv", "text/csv", "content".getBytes(), "RETRY");
        jdbcTemplate.update("INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)", "processed.csv", "text/csv", "content".getBytes(), "PROCESSED");
        jdbcTemplate.update("INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)", "failed.csv", "text/csv", "content".getBytes(), "FAILED");

        //Act
        List<UploadedFile> result = repository.findAllProcessable();

        //Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(UploadedFile::filename).containsExactlyInAnyOrder("pending.csv", "retry.csv");
    }

    @Test
    void shouldReturnEmptyList_whenNoProcessableFilesExist() {
        //Arrange
        jdbcTemplate.update("INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)", "processed.csv", "text/csv", "content".getBytes(), "PROCESSED");

        //Act
        List<UploadedFile> result = repository.findAllProcessable();

        //Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldUpdateFileStatus_whenUpdateStatusIsCalled() {
        //Arrange
        jdbcTemplate.update("INSERT INTO uploaded_files (filename, mime_type, content, status) VALUES (?, ?, ?, ?)", "file.csv", "text/csv", "content".getBytes(), "PENDING");
        Long fileId = jdbcTemplate.queryForObject("SELECT id FROM uploaded_files WHERE filename = 'file.csv'", Long.class);

        //Act
        repository.updateStatus(fileId, "PROCESSED");

        //Assert
        String status = jdbcTemplate.queryForObject("SELECT status FROM uploaded_files WHERE id = ?", String.class, fileId);
        assertThat(status).isEqualTo("PROCESSED");
    }
}
