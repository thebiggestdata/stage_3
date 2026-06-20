package com.thebiggestdata.domain.gateway;

public interface BookIngestedPublisher {
    void notifyIngestedBook(int bookId);
}
