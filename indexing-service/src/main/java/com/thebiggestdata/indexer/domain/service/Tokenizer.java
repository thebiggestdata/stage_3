package com.thebiggestdata.indexer.domain.service;

import java.util.Arrays;
import java.util.List;

public class Tokenizer {

    public List<String> tokenize(String normalized) {
        if (normalized.isEmpty())
            return List.of();

        return Arrays.asList(normalized.split(" "));
    }
}
