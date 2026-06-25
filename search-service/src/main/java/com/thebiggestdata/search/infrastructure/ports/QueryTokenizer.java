package com.thebiggestdata.search.infrastructure.ports;

import java.util.List;

public interface QueryTokenizer {

    List<String> tokenize(String query);
}
