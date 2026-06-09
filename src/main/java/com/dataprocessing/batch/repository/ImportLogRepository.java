package com.dataprocessing.batch.repository;

import com.dataprocessing.batch.model.ImportLog;
import com.dataprocessing.batch.model.RejectedTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Repository
@RequiredArgsConstructor
public class ImportLogRepository {

    private static final String INSERT_LOG = "INSERT INTO import_logs (uploaded_file_id, status, started_at) VALUES (?, ?, ?)";
    private static final String UPDATE_LOG = "UPDATE import_logs SET status = ?, total_records = ?, valid_records = ?, rejected_records = ?, error_message = ?, finished_at = ? WHERE id = ?";
    private static final String INSERT_REJECTED = "INSERT INTO rejected_transactions (import_log_id, reference, field, reason) VALUES (?, ?, ?, ?)";
    private static final String SELECT_LATEST_LOG = "SELECT id, uploaded_file_id, status, total_records, valid_records, rejected_records, error_message, started_at, finished_at FROM import_logs ORDER BY started_at DESC LIMIT 1";

    private final JdbcTemplate jdbcTemplate;

    public Long save(final Long uploadedFileId, final LocalDateTime startedAt) {
        final KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(INSERT_LOG, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setLong(1, uploadedFileId);
            preparedStatement.setString(2, "RUNNING");
            preparedStatement.setTimestamp(3, Timestamp.valueOf(startedAt));
            return preparedStatement;
        }, keyHolder);

        return (Long) requireNonNull(keyHolder.getKeys()).get("id");
    }

    public void update(final ImportLog importLog) {
        final Timestamp finishedAt = Optional.ofNullable(importLog.finishedAt())
            .map(Timestamp::valueOf)
            .orElse(null);

        jdbcTemplate.update(UPDATE_LOG,
            importLog.status(),
            importLog.totalRecords(),
            importLog.validRecords(),
            importLog.rejectedRecords(),
            importLog.errorMessage(),
            finishedAt,
            importLog.id()
        );
    }

    public void saveRejectedTransactions(final Long logId, final List<RejectedTransaction> rejectedTransactions) {
        final List<Object[]> batchArgs = rejectedTransactions.stream()
            .map(rejected -> new Object[]{logId, rejected.reference(), rejected.field(), rejected.reason()})
            .toList();

        jdbcTemplate.batchUpdate(INSERT_REJECTED, batchArgs);
    }

    public Optional<ImportLog> findLatest() {
        return jdbcTemplate.query(SELECT_LATEST_LOG, rs ->
            rs.next() ? Optional.of(mapImportLog(rs)) : Optional.empty()
        );
    }

    private ImportLog mapImportLog(final ResultSet rs) throws SQLException {
        return ImportLog.builder()
            .id(rs.getLong("id"))
            .uploadedFileId(rs.getLong("uploaded_file_id"))
            .status(rs.getString("status"))
            .totalRecords(rs.getObject("total_records", Integer.class))
            .validRecords(rs.getObject("valid_records", Integer.class))
            .rejectedRecords(rs.getObject("rejected_records", Integer.class))
            .errorMessage(rs.getString("error_message"))
            .startedAt(this.toLocalDateTime(rs.getTimestamp("started_at")))
            .finishedAt(this.toLocalDateTime(rs.getTimestamp("finished_at")))
            .build();
    }

    private LocalDateTime toLocalDateTime(final Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}
