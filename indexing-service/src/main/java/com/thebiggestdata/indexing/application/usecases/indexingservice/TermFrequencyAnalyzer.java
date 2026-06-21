package com.thebiggestdata.indexing.application.usecases.indexingservice;

import com.thebiggestdata.indexing.infrastructure.ports.TokenizerPort;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

public class TermFrequencyAnalyzer {

    private final TokenizerPort tokenizer;

    public TermFrequencyAnalyzer(TokenizerPort tokenizer) {
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

    public int countTotalTokens(Map<String, Long> frequencies) {
        return frequencies.values().stream().mapToInt(Long::intValue).sum();
    }
}
