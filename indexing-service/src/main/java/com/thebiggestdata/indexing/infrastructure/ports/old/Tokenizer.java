package com.thebiggestdata.indexing.infrastructure.ports.old;

import java.util.List;

public interface Tokenizer {
    List<String> tokenize(String text);
}
