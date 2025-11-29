package com.thebiggestdata.ingestion.domain.exception;

public class InvalidBookFormatException extends RuntimeException {
    public InvalidBookFormatException(String message) {
        super(message);
    }
}
