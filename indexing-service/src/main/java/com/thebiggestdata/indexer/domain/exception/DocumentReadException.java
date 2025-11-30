package com.thebiggestdata.indexer.domain.exception;

public class DocumentReadException extends RuntimeException {
    public DocumentReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
