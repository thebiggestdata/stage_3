package com.thebiggestdata.domain.gateway;

import java.io.IOException;
import java.nio.file.Path;

public interface PathBuilder {
    Path generatePath() throws IOException;
}
