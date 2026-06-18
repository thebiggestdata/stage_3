package com.thebiggestdata.infrastructure.ports;

import java.util.Set;

public interface StopWordsLoader {
    Set<String> load();
}
