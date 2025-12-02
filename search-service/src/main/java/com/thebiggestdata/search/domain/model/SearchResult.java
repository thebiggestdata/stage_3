package com.thebiggestdata.search.domain.model;

import java.util.List;

public record SearchResult(
        String query,
        List<Integer> bookIds
) {}
