package com.thebiggestdata.domain.gateway;

import java.util.List;

public interface TextTokenizer {
    List<String> tokenize(String text);
}
