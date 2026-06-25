package com.thebiggestdata.ingestion.infrastructure.adapters.bookprovider;

import java.io.IOException;
import java.util.concurrent.Callable;

public final class GutenbergRetryPolicy {

    private final int maxRetries;
    private final long baseDelayMillis;

    public GutenbergRetryPolicy(int maxRetries, long baseDelayMillis) {
        this.maxRetries = maxRetries;
        this.baseDelayMillis = baseDelayMillis;
    }

    public String execute(Callable<String> request) {
        int attempt = 0;

        while (true) {
            try {
                return request.call();
            } catch (Exception e) {
                if (!shouldRetry(e, attempt)) {
                    throw new GutenbergAdapterException("Could not fetch book from Gutenberg", e);
                }

                sleep(backoff(attempt));
                attempt++;
            }
        }
    }

    private boolean shouldRetry(Exception exception, int attempt) {
        return attempt < maxRetries && isTransient(exception);
    }

    private boolean isTransient(Exception exception) {
        if (exception instanceof GutenbergHttpException http) {
            return http.statusCode() == 429 || http.statusCode() >= 500;
        }

        return exception instanceof IOException;
    }

    private long backoff(int attempt) {
        return Math.min(baseDelayMillis * (1L << attempt), 30_000L);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(jitter(millis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GutenbergAdapterException("Interrupted while retrying Gutenberg request", e);
        }
    }

    private long jitter(long millis) {
        return (long) (millis * (0.9 + Math.random() * 0.2));
    }
}
