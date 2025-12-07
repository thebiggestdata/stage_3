package com.searchengine.indexer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class TokenizerService {

    private static final Logger logger = LoggerFactory.getLogger(TokenizerService.class);

    public List<String> tokenize(String content) {
        if (content == null || content.isEmpty()) return List.of();
        String normalized = normalize(content);
        List<String> tokens = split(normalized);
        logger.debug("Tokenized content into {} tokens", tokens.size());
        return tokens;
    }

    private String normalize(String text) {
        return text
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<String> split(String normalized) {
        if (normalized.isEmpty()) return List.of();
        return Arrays.stream(normalized.split(" "))
                .filter(word -> word.length() >= 3)
                .toList();
    }
}

