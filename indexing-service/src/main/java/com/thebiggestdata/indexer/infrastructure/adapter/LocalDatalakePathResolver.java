package com.thebiggestdata.indexer.infrastructure.adapter;

import com.thebiggestdata.indexer.domain.port.DatalakePathResolverPort;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDatalakePathResolver implements DatalakePathResolverPort {
    private final String basePath;

    public LocalDatalakePathResolver() {
        this.basePath = "/app/datalake" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    @Override
    public String resolve(int bookId) {
        return basePath + "/" + bookId + "_body.txt";
    }
}
