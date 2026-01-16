package com.thebiggestdata.ingestion.infrastructure.adapter.documentprovider;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.ingestion.infrastructure.port.DownloadDocumentStatusProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InMemoryDownloadStatus implements DownloadDocumentStatusProvider {
    private final IMap<Integer, Boolean> downloadedBooks;

    public InMemoryDownloadStatus(HazelcastInstance hazelcast) {
        this.downloadedBooks = hazelcast.getMap("downloaded-books");
    }

    @Override
    public Set<Integer> loadDocuments() {
        return new HashSet<>(downloadedBooks.keySet());
    }

    @Override
    public void saveDocs(int bookId) {
        downloadedBooks.put(bookId, true);
    }

    @Override
    public void registerDownload(int bookId) {
        downloadedBooks.put(bookId, true);
    }

    @Override
    public boolean isDownloaded(int bookId) {
        return downloadedBooks.containsKey(bookId);
    }

    @Override
    public List<Integer> getDownloadedDocs() {
        return new ArrayList<>(downloadedBooks.keySet());
    }
}

