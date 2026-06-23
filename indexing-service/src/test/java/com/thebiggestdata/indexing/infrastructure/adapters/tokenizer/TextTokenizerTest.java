package com.thebiggestdata.indexing.infrastructure.adapters.tokenizer;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextTokenizerTest {

    @Test
    void normalizesFiltersAndPreservesRepeatedTerms() {
        TextTokenizer tokenizer = new TextTokenizer(Set.of("the"));

        List<String> tokens = tokenizer.tokenize("The CLEAN, clean code — is readable!");

        assertEquals(List.of("clean", "clean", "code", "readable"), tokens);
    }
}
