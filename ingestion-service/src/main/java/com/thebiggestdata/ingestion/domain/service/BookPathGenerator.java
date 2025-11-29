package com.thebiggestdata.ingestion.domain.service;

import com.thebiggestdata.ingestion.domain.model.BookParts;
import java.time.LocalDateTime;

public class BookPathGenerator {

    public record GeneratedPaths(String headerPath, String bodyPath) {}

    public GeneratedPaths generate(BookParts parts, LocalDateTime timestamp) {

        DatalakeStructure structure = DatalakeStructure
                .forBook(parts.bookId())
                .at(timestamp);

        return new GeneratedPaths(
                structure.headerPath(),
                structure.bodyPath()
        );
    }
}
