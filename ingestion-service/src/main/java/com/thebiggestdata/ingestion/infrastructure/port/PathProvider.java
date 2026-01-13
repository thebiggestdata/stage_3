package com.thebiggestdata.ingestion.infrastructure.port;

import java.nio.file.Path;

public interface PathProvider {
    Path provide();
}
