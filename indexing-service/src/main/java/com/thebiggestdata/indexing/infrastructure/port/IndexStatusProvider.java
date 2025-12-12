package com.thebiggestdata.indexing.infrastructure.port;

import java.util.Map;

public interface IndexStatusProvider {
    Map<String, Object> status();
}
