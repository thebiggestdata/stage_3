package com.thebiggestdata.application.usecases.indexingservice;

import com.thebiggestdata.infrastructure.ports.Tokenizer;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

public class TermFrequencyAnalyzer {

    private final Tokenizer tokenizer;

    public TermFrequencyAnalyzer(Tokenizer tokenizer) {
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
