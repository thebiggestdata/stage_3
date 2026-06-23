package com.thebiggestdata.search.infrastructure.adapters.tokenizer;

import com.thebiggestdata.search.infrastructure.ports.QueryTokenizer;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SimpleQueryTokenizer implements QueryTokenizer {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    @Override
    public List<String> tokenize(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String normalized = NON_ALPHANUMERIC.matcher(query.toLowerCase(Locale.ROOT))
                .replaceAll(" ")
                .trim();
        if (normalized.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(WHITESPACE.split(normalized));
    }
}
