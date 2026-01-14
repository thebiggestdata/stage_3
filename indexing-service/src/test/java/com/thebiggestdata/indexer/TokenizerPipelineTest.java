package com.thebiggestdata.indexer;

import com.thebiggestdata.indexer.domain.model.RawDocument;
import com.thebiggestdata.indexer.domain.model.TokenStream;
import com.thebiggestdata.indexer.domain.service.TokenNormalizer;
import com.thebiggestdata.indexer.domain.service.Tokenizer;
import com.thebiggestdata.indexer.domain.service.TokenizerPipeline;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenizerPipelineTest {
    @Test
    void shouldNormalizeAndTokenize() {
        TokenNormalizer normalizer = new TokenNormalizer();
        Tokenizer tokenizer = new Tokenizer();
        TokenizerPipeline pipeline = new TokenizerPipeline(normalizer, tokenizer);

        RawDocument doc = new RawDocument(1, "Hello, World! 123");
        TokenStream result = pipeline.process(doc);

        assertEquals(3, result.tokens().size());
        assertTrue(result.tokens().contains("hello"));
        assertTrue(result.tokens().contains("world"));
        assertTrue(result.tokens().contains("123"));
    }
}
