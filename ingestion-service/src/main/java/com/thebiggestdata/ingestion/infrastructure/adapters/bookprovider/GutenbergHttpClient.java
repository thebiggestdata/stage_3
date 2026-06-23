package com.thebiggestdata.ingestion.infrastructure.adapters.bookprovider;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class GutenbergHttpClient {

    private static final String USER_AGENT = "TheBiggestDataBot/1.0";

    private final Duration timeout;
    private final HttpClient client;

    public GutenbergHttpClient(Duration timeout) {
        this.timeout = timeout;
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public String fetch(String url) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching Gutenberg book", e);
        }

        if (response.statusCode() != 200) {
            throw new GutenbergHttpException(response.statusCode(), url);
        }
        return response.body();
    }
}
