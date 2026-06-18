package com.thebiggestdata.infrastructure.ports;

public interface BookIngestedNotifier {
    void notifyIngestedBook(int bookId);
}
