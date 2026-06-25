package com.thebiggestdata.search.model;

public record IndexGeneration(String value) {

    public IndexGeneration {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Index generation must not be blank");
        }
    }
}
