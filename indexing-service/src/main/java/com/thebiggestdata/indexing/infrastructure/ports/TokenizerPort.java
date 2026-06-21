package com.thebiggestdata.indexing.infrastructure.ports;

import java.util.List;

public interface TokenizerPort {
    List<String> tokenize(String text);
}
