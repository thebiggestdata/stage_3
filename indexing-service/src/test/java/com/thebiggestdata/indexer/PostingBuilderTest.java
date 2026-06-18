package com.thebiggestdata.indexer;

import com.thebiggestdata.indexer.domain.model.TokenStream;
import com.thebiggestdata.indexer.domain.service.PostingBuilder;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PostingBuilderTest {
    @Test
    void shouldBuildPostingsFromTokenStream() {
        TokenStream stream = new TokenStream(1, List.of("hello", "world", "hello"));
        PostingBuilder builder = new PostingBuilder();

        Map<String, List<Integer>> postings = builder.build(stream);

        assertEquals(2, postings.size());
        assertTrue(postings.containsKey("hello"));
        assertTrue(postings.containsKey("world"));
        assertEquals(List.of(1), postings.get("hello"));
    }
}
