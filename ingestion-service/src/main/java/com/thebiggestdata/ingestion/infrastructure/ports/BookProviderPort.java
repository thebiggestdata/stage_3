package com.thebiggestdata.ingestion.infrastructure.ports;

import com.thebiggestdata.ingestion.model.BookContent;

public interface BookProviderPort {
    BookContent getBookContent(int bookId);
}
