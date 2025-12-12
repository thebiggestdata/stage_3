package com.thebiggestdata.indexing.infrastructure.port;

import java.util.List;

public interface Tokenizer {
    List<String> tokenize(String text);
}

