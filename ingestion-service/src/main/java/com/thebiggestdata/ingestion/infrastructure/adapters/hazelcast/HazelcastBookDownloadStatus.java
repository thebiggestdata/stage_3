package com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast;

import com.hazelcast.collection.ISet;
import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.ingestion.infrastructure.ports.BookDownloadStatus;

public final class HazelcastBookDownloadStatus implements BookDownloadStatus {

    private final ISet<Integer> downloadedBooks;

    public HazelcastBookDownloadStatus(HazelcastInstance hazelcast) {
        this.downloadedBooks = hazelcast.getSet(HazelcastNames.DOWNLOADED_BOOKS);
    }

    @Override
    public boolean isDownloaded(int bookId) {
        return downloadedBooks.contains(bookId);
    }

    @Override
    public void markAsDownloaded(int bookId) {
        downloadedBooks.add(bookId);
    }
}
