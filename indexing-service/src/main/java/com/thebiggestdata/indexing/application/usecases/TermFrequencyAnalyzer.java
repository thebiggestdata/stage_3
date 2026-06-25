package com.thebiggestdata.indexing.application.usecases;

import com.thebiggestdata.indexing.infrastructure.ports.Tokenizer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class TermFrequencyAnalyzer {

    private final Tokenizer tokenizer;

    public TermFrequencyAnalyzer(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public Analysis analyze(String text) {
        List<String> tokens = tokenizer.tokenize(text);
        Map<String, Long> frequencies = tokens.stream()
                .collect(Collectors.groupingBy(token -> token, Collectors.counting()));
        return new Analysis(frequencies, tokens.size());
    }

    public record Analysis(Map<String, Long> frequencies, int totalTokens) {
        public Analysis {
            frequencies = Map.copyOf(frequencies);
        }
    }
}
