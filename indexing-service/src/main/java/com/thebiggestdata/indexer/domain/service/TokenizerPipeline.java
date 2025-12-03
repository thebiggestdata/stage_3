package com.thebiggestdata.indexer.domain.service;

import com.thebiggestdata.indexer.domain.model.RawDocument;
import com.thebiggestdata.indexer.domain.model.TokenStream;
import java.util.List;

public class TokenizerPipeline {
    private final TokenNormalizer normalizer;
    private final Tokenizer tokenizer;

    public TokenizerPipeline(TokenNormalizer normalizer, Tokenizer tokenizer) {
        this.normalizer = normalizer;
        this.tokenizer = tokenizer;
    }

    public TokenStream process(RawDocument doc) {
        String normalized = normalizer.normalize(doc.body());
        List<String> tokens = tokenizer.tokenize(normalized);
        return new TokenStream(doc.bookId(), tokens);
    }
}
