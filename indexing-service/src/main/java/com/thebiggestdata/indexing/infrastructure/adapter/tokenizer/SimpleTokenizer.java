package com.thebiggestdata.indexing.infrastructure.adapter.tokenizer;

import com.thebiggestdata.indexing.infrastructure.port.Tokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class SimpleTokenizer implements Tokenizer {

    private static final Logger logger = LoggerFactory.getLogger(SimpleTokenizer.class);

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
            "has", "he", "in", "is", "it", "its", "of", "on", "that", "the",
            "to", "was", "will", "with", "i", "you", "we", "they", "but", "or",
            "this", "which", "not", "have", "had", "been", "their", "there", "here"
    );

    @Override
    public List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        String[] words = text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .split("\\s+");

        Set<String> uniqueTokens = Arrays.stream(words)
                .filter(word -> !word.isEmpty())
                .filter(word -> word.length() > 2)
                .filter(word -> !STOP_WORDS.contains(word))
                .collect(Collectors.toSet());

        List<String> tokens = new ArrayList<>(uniqueTokens);
        logger.debug("Tokenized {} words into {} unique tokens", words.length, tokens.size());

        return tokens;
    }
}