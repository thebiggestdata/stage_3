package com.thebiggestdata.infrastructure.ports;

public interface ReplicationExecuter {
    void replicate(int bookId);
}
