package com.thebiggestdata.indexing.infrastructure.adapters.tokenizer;

import com.thebiggestdata.indexing.infrastructure.ports.Tokenizer;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class TextTokenizer implements Tokenizer {
    private static final Pattern CLEANUP_PATTERN = Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern SPLIT_PATTERN = Pattern.compile("\\s+");

    private final Set<String> stopwords;

    public TextTokenizer(Set<String> stopwords) {
        this.stopwords = stopwords;
    }

    @Override
    public List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        String cleaned = CLEANUP_PATTERN.matcher(text.toLowerCase()).replaceAll(" ");
        String[] tokens = SPLIT_PATTERN.split(cleaned);

        return Arrays.stream(tokens)
                .filter(token -> !token.isEmpty())
                .filter(token -> token.length() > 2)
                .filter(token -> !stopwords.contains(token))
                .toList();
    }
}
