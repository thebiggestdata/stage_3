package com.thebiggestdata.ingestion.model;

import java.io.Serializable;

public record DuplicatedBook (
        String header,
        String body,
        String srcNode
) implements Serializable {
}
