package com.thebiggestdata.indexer.domain.model;

import java.util.List;

public record TokenStream(
        int bookId,
        List<String> tokens
) {}
