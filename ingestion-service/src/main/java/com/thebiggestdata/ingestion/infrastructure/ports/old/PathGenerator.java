package com.thebiggestdata.ingestion.infrastructure.ports.old;

import java.io.IOException;
import java.nio.file.Path;

public interface PathGenerator {
    Path generatePath() throws IOException;
}
