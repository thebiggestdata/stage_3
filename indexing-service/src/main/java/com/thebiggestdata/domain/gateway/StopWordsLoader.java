package com.thebiggestdata.domain.gateway;

import java.util.Set;

public interface StopWordsLoader {
    Set<String> load();
}
