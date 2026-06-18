package com.thebiggestdata.search.model;

public record SearchResult(int id, String title, String author, String language, int year, int frequency) {
}