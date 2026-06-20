package com.thebiggestdata.usecase;

import com.thebiggestdata.domain.gateway.TextTokenizer;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

public class TermFrequencyCalculator {

    private final TextTokenizer tokenizer;

    public TermFrequencyCalculator(TextTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public Map<String, Long> analyze(String text) {
        if (text == null || text.isBlank()) return Map.of();

        List<String> tokens = tokenizer.tokenize(text);

        return tokens.parallelStream()
                .map(String::toLowerCase)
                .collect(Collectors.groupingByConcurrent(
                        token -> token,
                        Collectors.counting()
                ));
    }

    public int countTotalTokens(String text) {
        if (text == null || text.isBlank()) return 0;
        return tokenizer.tokenize(text).size();
    }
}
