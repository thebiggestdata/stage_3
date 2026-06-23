package com.thebiggestdata.ingestion.infrastructure.adapters.bookprovider;

import java.io.IOException;

public final class GutenbergHttpException extends IOException {

    private final int statusCode;

    public GutenbergHttpException(int statusCode, String url) {
        super("Gutenberg returned HTTP %d for %s".formatted(statusCode, url));
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
