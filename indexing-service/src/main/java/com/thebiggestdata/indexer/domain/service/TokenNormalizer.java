package com.thebiggestdata.indexer.domain.service;

public class TokenNormalizer {

    public String normalize(String text) {
        return text
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
