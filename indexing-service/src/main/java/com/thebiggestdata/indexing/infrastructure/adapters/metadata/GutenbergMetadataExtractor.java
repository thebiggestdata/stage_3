package com.thebiggestdata.indexing.infrastructure.adapters.metadata;

import com.thebiggestdata.indexing.infrastructure.ports.MetadataExtractor;
import com.thebiggestdata.indexing.model.BookMetadata;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GutenbergMetadataExtractor implements MetadataExtractor {

    private static final Pattern YEAR = Pattern.compile("\\b(1[5-9]\\d{2}|20\\d{2})\\b");

    @Override
    public BookMetadata extract(String header) {
        String title = null;
        String author = null;
        String language = null;
        Integer year = null;

        for (String rawLine : header.split("\\R")) {
            String line = rawLine.trim();
            if (line.startsWith("Title:")) {
                title = valueAfterLabel(line, "Title:");
            } else if (line.startsWith("Author:")) {
                author = valueAfterLabel(line, "Author:");
            } else if (line.startsWith("Language:")) {
                language = valueAfterLabel(line, "Language:");
            } else if (line.startsWith("Release date:")) {
                year = extractYear(line);
            }
        }
        return new BookMetadata(title, author, language, year);
    }

    private String valueAfterLabel(String line, String label) {
        String value = line.substring(label.length()).trim();
        return value.isEmpty() ? null : value;
    }

    private Integer extractYear(String releaseDate) {
        Matcher matcher = YEAR.matcher(releaseDate);
        return matcher.find() ? Integer.parseInt(matcher.group()) : null;
    }
}
