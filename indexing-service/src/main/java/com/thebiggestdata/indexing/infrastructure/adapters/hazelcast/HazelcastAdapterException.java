package com.thebiggestdata.indexing.infrastructure.adapters.hazelcast;

public final class HazelcastAdapterException extends RuntimeException {

    public HazelcastAdapterException(String message) {
        super(message);
    }

    public HazelcastAdapterException(String message, Throwable cause) {
        super(message, cause);
    }
}
