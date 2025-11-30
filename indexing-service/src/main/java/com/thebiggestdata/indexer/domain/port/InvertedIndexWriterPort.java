package com.thebiggestdata.indexer.domain.port;

import java.util.Map;
import java.util.List;

public interface InvertedIndexWriterPort {
    void write(Map<String, List<Integer>> postings);
}
