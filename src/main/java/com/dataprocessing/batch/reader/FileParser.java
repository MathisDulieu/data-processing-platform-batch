package com.dataprocessing.batch.reader;

import com.dataprocessing.batch.model.Transaction;

import java.util.List;

public interface FileParser {

    List<Transaction> parse(byte[] content, Long uploadedFileId);
}