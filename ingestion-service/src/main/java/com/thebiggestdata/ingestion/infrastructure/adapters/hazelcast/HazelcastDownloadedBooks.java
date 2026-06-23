package com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast;

import com.hazelcast.collection.ISet;
import com.hazelcast.core.HazelcastInstance;
import com.thebiggestdata.ingestion.infrastructure.ports.DownloadedBooks;

import java.util.Comparator;
import java.util.List;

public final class HazelcastDownloadedBooks implements DownloadedBooks {

    private final ISet<Integer> downloadedBooks;

    public HazelcastDownloadedBooks(HazelcastInstance hazelcast) {
        this.downloadedBooks = hazelcast.getSet(HazelcastNames.DOWNLOADED_BOOKS);
    }

    @Override
    public List<Integer> findAll() {
        return downloadedBooks.stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
