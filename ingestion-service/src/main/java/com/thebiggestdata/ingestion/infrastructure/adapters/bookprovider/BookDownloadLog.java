package com.thebiggestdata.ingestion.infrastructure.adapters.bookprovider;

import com.hazelcast.collection.ISet;
import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.ingestion.infrastructure.ports.BookDownloadStatusStore;

import java.util.ArrayList;
import java.util.List;

public class BookDownloadLog implements BookDownloadStatusStore {
    private final ISet<Integer> downloadedBooks;

    public BookDownloadLog(HazelcastInstance hazelcastInstance, String setName) {
        this.downloadedBooks = hazelcastInstance.getSet(setName);
    }

    @Override
    public void registerBookDownload(int bookId) {
        downloadedBooks.add(bookId);
    }

    @Override
    public boolean isDownloaded(int bookId) {
        return downloadedBooks.contains(bookId);
    }

    @Override
    public List<Integer> getAllDownloadedBooks() {
        return new ArrayList<>(downloadedBooks);
    }
}
