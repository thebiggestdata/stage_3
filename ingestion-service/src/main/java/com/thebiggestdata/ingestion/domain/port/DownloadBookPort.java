package com.thebiggestdata.ingestion.domain.port;

import com.thebiggestdata.ingestion.domain.model.BookContent;

public interface DownloadBookPort {
    BookContent download(int bookId) throws Exception;
}
