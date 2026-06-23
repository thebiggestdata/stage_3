package com.thebiggestdata.indexing.infrastructure.ports;

import com.thebiggestdata.indexing.model.BookMetadata;

public interface MetadataExtractor {

    BookMetadata extract(String header);
}
