package com.thebiggestdata.indexing.model;

public record IndexedTerm(String term, String documentId, long frequency) {
}
