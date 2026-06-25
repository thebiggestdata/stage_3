package com.thebiggestdata.ingestion.infrastructure.adapters.filesystem;

public final class FilesystemAdapterException extends RuntimeException {

    public FilesystemAdapterException(String message, Throwable cause) {
        super(message, cause);
    }
}