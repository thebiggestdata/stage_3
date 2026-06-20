package com.thebiggestdata.domain.gateway;

import java.util.Set;

public interface StopWordsProvider {
    Set<String> load();
}
