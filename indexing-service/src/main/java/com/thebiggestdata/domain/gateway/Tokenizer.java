package com.thebiggestdata.domain.gateway;

import java.util.List;

public interface Tokenizer {
    List<String> tokenize(String text);
}
