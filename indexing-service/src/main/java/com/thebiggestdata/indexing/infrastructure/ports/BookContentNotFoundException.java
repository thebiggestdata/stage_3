package com.thebiggestdata.indexing.infrastructure.ports;

public final class BookContentNotFoundException extends RuntimeException {

    public BookContentNotFoundException(String message) {
        super(message);
    }

    public BookContentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
