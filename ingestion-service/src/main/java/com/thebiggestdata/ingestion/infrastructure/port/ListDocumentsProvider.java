package com.thebiggestdata.ingestion.infrastructure.port;

import java.util.Map;

public interface ListDocumentsProvider {
    Map<String, Object> list();
}
