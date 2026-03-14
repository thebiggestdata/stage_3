package org.example.shared.model;

import java.io.Serializable;

public class BookMetadata implements Serializable {

    private String bookId;
    private String title;
    private String author;
    private String language;
    private String releaseDate;
    private String storagePath;

    public BookMetadata() {}

    public BookMetadata(String bookId, String title, String author,
                        String language, String releaseDate, String storagePath) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.language = language;
        this.releaseDate = releaseDate;
        this.storagePath = storagePath;
    }

    public String getBookId()      { return bookId; }
    public String getTitle()       { return title; }
    public String getAuthor()      { return author; }
    public String getLanguage()    { return language; }
    public String getReleaseDate() { return releaseDate; }
    public String getStoragePath() { return storagePath; }

    public void setBookId(String bookId)           { this.bookId = bookId; }
    public void setTitle(String title)             { this.title = title; }
    public void setAuthor(String author)           { this.author = author; }
    public void setLanguage(String language)       { this.language = language; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    @Override
    public String toString() {
        return "BookMetadata{bookId='" + bookId + "', title='" + title +
               "', author='" + author + "'}";
    }
}
