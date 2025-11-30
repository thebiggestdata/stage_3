package com.thebiggestdata.ingestion.infrastructure.adapter;

import com.thebiggestdata.ingestion.domain.model.BookContent;
import com.thebiggestdata.ingestion.domain.port.DownloadBookPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpBookFetcherAdapter implements DownloadBookPort {

    private final HttpClient client = HttpClient.newHttpClient();

    @Override
    public BookContent download(int bookId) throws Exception {

        String url = "https://www.gutenberg.org/cache/epub/" + bookId + "/pg" + bookId + ".txt";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to download book " + bookId);
        }

        return new BookContent(bookId, response.body());
    }
}
