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

    public void process(UploadedFile uploadedFile) {
        Long logId = importLogService.start(uploadedFile.id());
        try {
            List<Transaction> parsed = parseFile(uploadedFile);
            List<Transaction> valid = filterValid(parsed);
            List<RejectedTransaction> rejected = buildRejectedList(parsed);
            List<Transaction> transformed = transformAll(valid);
            transactionRepository.saveAll(transformed);
            uploadedFileRepository.updateStatus(uploadedFile.id(), "PROCESSED");
            importLogService.success(logId, uploadedFile.id(), parsed.size(), valid.size(), rejected.size(), rejected);
        } catch (Exception e) {
            uploadedFileRepository.updateStatus(uploadedFile.id(), "FAILED");
            importLogService.failure(logId, uploadedFile.id(), e.getMessage());
        }
    }

    private List<Transaction> parseFile(UploadedFile uploadedFile) {
        FileParser parser = fileParserFactory.getParser(uploadedFile.mimeType());
        return parser.parse(uploadedFile.content(), uploadedFile.id());
    }

    private List<Transaction> filterValid(List<Transaction> transactions) {
        return transactions.stream()
            .filter(this::isValid)
            .toList();
    }

    private List<RejectedTransaction> buildRejectedList(List<Transaction> transactions) {
        return transactions.stream()
            .flatMap(transaction -> findFirstError(transaction)
                .map(error -> new RejectedTransaction(transaction.reference(), error.field(), error.message()))
                .stream())
            .toList();
    }

    private boolean isValid(Transaction transaction) {
        return findFirstError(transaction).isEmpty();
    }

    private Optional<ValidationError> findFirstError(Transaction transaction) {
        return validationRules.stream()
            .map(rule -> rule.validate(transaction))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

    private List<Transaction> transformAll(List<Transaction> transactions) {
        return transactions.stream()
            .map(transactionTransformer::transform)
            .toList();
    }
}
