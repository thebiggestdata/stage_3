package com.thebiggestdata.ingestion.infrastructure.adapter.documentprovider;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FetchGutenbergBook {
    public String fetch(int bookId) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        String url = "https://www.gutenberg.org/cache/epub/" + bookId + "/pg" + bookId + ".txt";
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("Failed to fetch document: " + response.statusCode());
        return response.body();
    }
}
