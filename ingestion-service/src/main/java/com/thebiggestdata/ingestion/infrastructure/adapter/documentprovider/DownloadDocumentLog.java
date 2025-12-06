package com.thebiggestdata.ingestion.infrastructure.adapter.documentprovider;

import com.thebiggestdata.ingestion.infrastructure.port.DownloadDocumentStatusProvider;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.nio.file.Files;
import java.io.IOException;

public class DownloadDocumentLog implements DownloadDocumentStatusProvider {
    private final String downloadedDocumentPath;

    public DownloadDocumentLog(String downloadedDocumentPath) {
        this.downloadedDocumentPath = downloadedDocumentPath;
    }

    @Override
    public void registerDownload(int bookId) throws IOException {
        Set<Integer> documents = loadDocuments();
        documents.add(bookId);
        saveDocs(bookId);
    }

    @Override
    public boolean isDownloaded(int bookId) throws IOException {
        return loadDocuments().contains(bookId);
    }

    @Override
    public List<Integer> getDownloadedDocs() throws IOException {
        return new ArrayList<>(loadDocuments());
    }

    @Override
    public Set<Integer> loadDocuments() throws IOException {
        Path docPath = Paths.get(downloadedDocumentPath);
        if (!Files.exists(docPath)) return new HashSet<>();
        List<String> lines = Files.readAllLines(docPath);
        Set<Integer> docs = new HashSet<>();
        for (String line : lines) if (!line.trim().isEmpty()) docs.add(Integer.parseInt(line.trim()));
        return docs;
    }

    @Override
    public void saveDocs(int bookId) throws IOException {
        Path docPath = Paths.get(downloadedDocumentPath);
        if (docPath.getParent() != null && !Files.exists(docPath.getParent())) Files.createDirectories(docPath.getParent());
        String line = bookId + System.lineSeparator();
        Files.writeString(docPath, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

}
