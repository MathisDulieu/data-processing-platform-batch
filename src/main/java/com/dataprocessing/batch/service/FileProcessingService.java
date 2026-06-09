package com.dataprocessing.batch.service;

import com.dataprocessing.batch.model.RejectedTransaction;
import com.dataprocessing.batch.model.Transaction;
import com.dataprocessing.batch.model.UploadedFile;
import com.dataprocessing.batch.model.ValidationError;
import com.dataprocessing.batch.reader.FileParser;
import com.dataprocessing.batch.reader.FileParserFactory;
import com.dataprocessing.batch.repository.TransactionRepository;
import com.dataprocessing.batch.repository.UploadedFileRepository;
import com.dataprocessing.batch.transformer.TransactionTransformer;
import com.dataprocessing.batch.validator.ValidationRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileProcessingService {

    private final FileParserFactory fileParserFactory;
    private final List<ValidationRule> validationRules;
    private final TransactionTransformer transactionTransformer;
    private final TransactionRepository transactionRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final ImportLogService importLogService;

    public void process(final UploadedFile uploadedFile) {
        final Long logId = importLogService.start(uploadedFile.id());
        try {
            final List<Transaction> parsed = this.parseFile(uploadedFile);
            final List<Transaction> valid = this.filterValid(parsed);
            final List<RejectedTransaction> rejected = this.buildRejectedList(parsed);
            final List<Transaction> transformed = this.transformAll(valid);
            transactionRepository.saveAll(transformed);
            uploadedFileRepository.updateStatus(uploadedFile.id(), "PROCESSED");
            importLogService.success(logId, uploadedFile.id(), parsed.size(), valid.size(), rejected.size(), rejected);
        } catch (Exception e) {
            uploadedFileRepository.updateStatus(uploadedFile.id(), "FAILED");
            importLogService.failure(logId, uploadedFile.id(), e.getMessage());
        }
    }

    private List<Transaction> parseFile(final UploadedFile uploadedFile) {
        final FileParser parser = fileParserFactory.getParser(uploadedFile.mimeType());
        return parser.parse(uploadedFile.content(), uploadedFile.id());
    }

    private List<Transaction> filterValid(final List<Transaction> transactions) {
        return transactions.stream()
            .filter(this::isValid)
            .toList();
    }

    private List<RejectedTransaction> buildRejectedList(final List<Transaction> transactions) {
        return transactions.stream()
            .flatMap(transaction -> this.findFirstError(transaction)
                .map(error -> new RejectedTransaction(transaction.reference(), error.field(), error.message()))
                .stream())
            .toList();
    }

    private boolean isValid(final Transaction transaction) {
        return this.findFirstError(transaction).isEmpty();
    }

    private Optional<ValidationError> findFirstError(final Transaction transaction) {
        return validationRules.stream()
            .map(rule -> rule.validate(transaction))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

    private List<Transaction> transformAll(final List<Transaction> transactions) {
        return transactions.stream()
            .map(transactionTransformer::transform)
            .toList();
    }
}
