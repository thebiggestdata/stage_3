package com.thebiggestdata.search.infrastructure.port;

import java.util.Map;

public interface SearchStatsProvider {
    Map<String, Object> stats();
}