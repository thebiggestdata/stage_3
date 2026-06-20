package com.thebiggestdata.ingestion.infrastructure.ports;

import java.io.IOException;
import java.nio.file.Path;

public interface PathGenerator {
    Path generatePath() throws IOException;
}
