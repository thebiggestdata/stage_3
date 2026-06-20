package com.thebiggestdata.domain.gateway;

public interface BookIngestedNotifier {
    void notifyIngestedBook(int bookId);
}
