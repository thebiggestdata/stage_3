package com.thebiggestdata.search.domain.port;

import java.util.List;

public interface InvertedIndexReaderPort {

    List<Integer> getBookIdsForToken(String token);
}
