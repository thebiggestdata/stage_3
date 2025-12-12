package com.thebiggestdata.indexing.infrastructure.adapter.api;

import com.thebiggestdata.indexing.infrastructure.port.*;
import com.thebiggestdata.indexing.model.BookContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class IndexBookService implements IndexBookProvider {
    private static final Logger logger = LoggerFactory.getLogger(IndexBookService.class);

    private final DatalakeReader datalakeReader;
    private final Tokenizer tokenizer;
    private final InvertedIndexWriter indexWriter;

    public IndexBookService(
            DatalakeReader datalakeReader,
            Tokenizer tokenizer,
            InvertedIndexWriter indexWriter) {
        this.datalakeReader = datalakeReader;
        this.tokenizer = tokenizer;
        this.indexWriter = indexWriter;
    }

    @Override
    public Map<String, Object> index(int bookId) {
        logger.info("index() - Start indexing bookId={}", bookId);

        try {
            if (indexWriter.isIndexed(bookId)) return alreadyIndexedResponse(bookId);
            BookContent book = datalakeReader.read(bookId);
            if (book == null) return bookNotFoundResponse(bookId);
            List<String> tokens = tokenizer.tokenize(book.body());
            indexWriter.indexBook(bookId, tokens);
            return successResponse(bookId, tokens.size());
        } catch (Exception e) {
            logger.error("index() - Error indexing bookId {}: {}", bookId, e.getMessage(), e);
            return errorResponse(bookId, e.getMessage());
        } finally {
            logger.info("index() - Finished indexing bookId={}", bookId);
        }
    }

    private Map<String, Object> successResponse(int bookId, int tokenCount) {
        logger.info("index() - Book {} indexed successfully with {} tokens", bookId, tokenCount);
        return Map.of(
                "book_id", bookId,
                "status", "indexed",
                "token_count", tokenCount
        );
    }

    private Map<String, Object> alreadyIndexedResponse(int bookId) {
        logger.warn("index() - Book {} is already indexed", bookId);
        return Map.of(
                "book_id", bookId,
                "status", "already_indexed"
        );
    }

    private Map<String, Object> bookNotFoundResponse(int bookId) {
        logger.warn("index() - Book {} not found in datalake", bookId);
        return Map.of(
                "book_id", bookId,
                "status", "not_found",
                "message", "Book not found in datalake"
        );
    }

    private Map<String, Object> errorResponse(int bookId, String message) {
        return Map.of(
                "book_id", bookId,
                "status", "error",
                "message", message
        );
    }
}