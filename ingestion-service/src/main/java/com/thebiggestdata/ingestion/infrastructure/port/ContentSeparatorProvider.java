package com.thebiggestdata.ingestion.infrastructure.port;

import java.util.List;

public interface ContentSeparatorProvider {
    List<String> provide(String content);
}
