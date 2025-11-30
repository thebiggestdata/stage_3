package com.thebiggestdata.ingestion.infrastructure.adapter;

import com.thebiggestdata.ingestion.domain.model.BookParts;
import com.thebiggestdata.ingestion.domain.port.DatalakeWritePort;
import com.thebiggestdata.ingestion.domain.service.BookPathGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class FileSystemDatalakeAdapter implements DatalakeWritePort {

    private final BookPathGenerator pathGen = new BookPathGenerator();

    @Override
    public void write(BookParts parts, LocalDateTime timestamp) {

        var paths = pathGen.generate(parts, timestamp);

        try {
            Path headerPath = Path.of(paths.headerPath());
            Path bodyPath = Path.of(paths.bodyPath());

            Files.createDirectories(headerPath.getParent());

            Files.writeString(headerPath, parts.header());
            Files.writeString(bodyPath, parts.body());

        } catch (IOException e) {
            throw new RuntimeException("Failed to write book " + parts.bookId(), e);
        }
    }
}
