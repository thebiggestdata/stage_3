package com.thebiggestdata.indexer.domain.service;

import com.thebiggestdata.indexer.domain.model.TokenStream;
import java.util.*;

public class PostingBuilder {

    public Map<String, List<Integer>> build(TokenStream stream) {
        Set<String> uniqueTokens = new HashSet<>(stream.tokens());

        Map<String, List<Integer>> postings = new HashMap<>();

        for (String token : uniqueTokens) {
            List<Integer> bookIdList = new ArrayList<>();
            bookIdList.add(stream.bookId());

            postings.put(token, bookIdList);
        }
        return postings;
    }
}
