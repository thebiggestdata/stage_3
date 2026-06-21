package com.thebiggestdata.indexing.infrastructure.ports.old;

import java.util.Set;

public interface StopWordsLoader {
    Set<String> load();
}
