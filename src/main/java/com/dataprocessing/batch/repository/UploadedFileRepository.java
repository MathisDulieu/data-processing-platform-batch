package com.dataprocessing.batch.repository;

import com.dataprocessing.batch.model.UploadedFile;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UploadedFileRepository {

    private static final String SELECT_PROCESSABLE = "SELECT id, filename, mime_type, content FROM uploaded_files WHERE status IN ('PENDING', 'RETRY')";
    private static final String UPDATE_STATUS = "UPDATE uploaded_files SET status = ? WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;

    public List<UploadedFile> findAllProcessable() {
        return jdbcTemplate.query(SELECT_PROCESSABLE, (rs, rowNum) -> UploadedFile.builder()
            .id(rs.getLong("id"))
            .filename(rs.getString("filename"))
            .mimeType(rs.getString("mime_type"))
            .content(rs.getBytes("content"))
            .build()
        );
    }

    public void updateStatus(final Long fileId, final String status) {
        jdbcTemplate.update(UPDATE_STATUS, status, fileId);
    }
}
