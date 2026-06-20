package com.thebiggestdata.infrastructure.adapter.filesystem;

import com.thebiggestdata.domain.gateway.PathBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateTimePathBuilder implements PathBuilder {
    private final String datalakePath;

    public DateTimePathBuilder(String datalakePath) {
        this.datalakePath = datalakePath;
    }

    @Override
    public Path generatePath() throws IOException {
        Instant instant = Instant.now();
        ZoneId zone = ZoneId.of("GMT");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH");
        String dateDirectory = instant.atZone(zone).format(dateFormatter);
        String timeDirectory = instant.atZone(zone).format(timeFormatter);

        Path date = Paths.get(this.datalakePath).resolve(Paths.get(dateDirectory));
        Path time = date.resolve(timeDirectory);

        Files.createDirectories(time);

        return time;
    }
}
