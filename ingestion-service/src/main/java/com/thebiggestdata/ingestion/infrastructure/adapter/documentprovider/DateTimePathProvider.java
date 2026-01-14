package com.thebiggestdata.ingestion.infrastructure.adapter.documentprovider;

import com.thebiggestdata.ingestion.infrastructure.port.PathProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateTimePathProvider implements PathProvider {
    private String basePath;

    public DateTimePathProvider(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public Path provide() {
        Instant instant = Instant.now();
        ZoneId zone = ZoneId.of("GMT");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH");
        String dateDirectory = instant.atZone(zone).format(dateFormatter);
        String timeDirectory = instant.atZone(zone).format(timeFormatter);
        Path date = Paths.get(this.basePath).resolve(Paths.get(dateDirectory));
        Path time = date.resolve(timeDirectory);
        try {Files.createDirectories(time);}
        catch (IOException e) {throw new RuntimeException(e);}
        return time;
    }
}
