package com.thebiggestdata.ingestion.domain.service;

import java.time.LocalDateTime;

public class DatalakeStructure {

    private final int bookId;
    private LocalDateTime timestamp;

    private DatalakeStructure(int bookId) {
        this.bookId = bookId;
    }

    public static DatalakeStructure forBook(int bookId) {
        return new DatalakeStructure(bookId);
    }

    public DatalakeStructure at(LocalDateTime time) {
        this.timestamp = time;
        return this;
    }

    public String headerPath() {
        return basePath() + "/" + bookId + "_header.txt";
    }

    public String bodyPath() {
        return basePath() + "/" + bookId + "_body.txt";
    }

    private String basePath() {
        int year = timestamp.getYear();
        int month = timestamp.getMonthValue();
        int day = timestamp.getDayOfMonth();
        int hour = timestamp.getHour();

        return "datalake/" +
                String.format("%04d%02d%02d", year, month, day) + "/" +
                String.format("%02d", hour);
    }
}
