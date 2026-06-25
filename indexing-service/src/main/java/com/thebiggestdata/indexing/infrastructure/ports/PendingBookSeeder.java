package com.thebiggestdata.indexing.infrastructure.ports;

public interface PendingBookSeeder {

    void seedAfter(int maxBookId);

    void reset();
}
