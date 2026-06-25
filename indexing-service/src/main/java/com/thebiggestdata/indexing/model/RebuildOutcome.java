package com.thebiggestdata.indexing.model;

import java.util.List;

public record RebuildOutcome(boolean completed, int maxBookId, List<String> failures) {

    public RebuildOutcome {
        failures = List.copyOf(failures);
    }

    public boolean successful() {
        return completed && failures.isEmpty();
    }
}
