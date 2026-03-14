package org.example.shared.model;

import java.util.List;

public class SearchResult {

    private String bookId;
    private String title;
    private String author;
    private List<String> matchedTerms;
    private int relevanceScore;

    public SearchResult() {}

    public SearchResult(String bookId, String title, String author,
                        List<String> matchedTerms, int relevanceScore) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.matchedTerms = matchedTerms;
        this.relevanceScore = relevanceScore;
    }

    public String getBookId()            { return bookId; }
    public String getTitle()             { return title; }
    public String getAuthor()            { return author; }
    public List<String> getMatchedTerms(){ return matchedTerms; }
    public int getRelevanceScore()       { return relevanceScore; }

    public void setBookId(String bookId)                  { this.bookId = bookId; }
    public void setTitle(String title)                    { this.title = title; }
    public void setAuthor(String author)                  { this.author = author; }
    public void setMatchedTerms(List<String> matchedTerms){ this.matchedTerms = matchedTerms; }
    public void setRelevanceScore(int relevanceScore)     { this.relevanceScore = relevanceScore; }
}
