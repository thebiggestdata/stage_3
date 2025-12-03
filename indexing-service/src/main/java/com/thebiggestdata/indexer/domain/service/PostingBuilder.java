package com.thebiggestdata.indexer.domain.service;

import com.thebiggestdata.indexer.domain.model.TokenStream;
import java.util.*;

public class PostingBuilder {

    public Map<String, List<Integer>> build(TokenStream stream) {
        Map<String, List<Integer>> postings = new HashMap<>();
        for (String token : stream.tokens()) {
            postings.computeIfAbsent(token, k -> new ArrayList<>())
                    .add(stream.bookId());
        }
        return postings;
    }
}
