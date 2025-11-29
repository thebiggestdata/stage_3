package com.thebiggestdata.ingestion.domain.port;

import com.thebiggestdata.ingestion.domain.model.BookParts;

import java.time.LocalDateTime;

public interface DatalakeWritePort {
    void write(BookParts parts, LocalDateTime timestamp);
}
