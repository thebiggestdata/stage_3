package com.bigdata.stage3;

import java.io.Serializable;
import java.time.Instant;

/**
 * Represents a task to be processed by workers in the distributed system.
 * Tasks are serializable so they can be transmitted through Hazelcast queues.
 */
public class Task implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String taskId;
    private final String data;
    private final long createdAt;
    private final String operation;
    
    /**
     * Creates a new task.
     * 
     * @param taskId Unique identifier for this task
     * @param data Input data to be processed
     * @param operation Type of operation to perform (e.g., "CALCULATE_PRIME", "UPPERCASE", "REVERSE")
     */
    public Task(String taskId, String data, String operation) {
        this.taskId = taskId;
        this.data = data;
        this.operation = operation;
        this.createdAt = Instant.now().toEpochMilli();
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public String getData() {
        return data;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public String getOperation() {
        return operation;
    }
    
    @Override
    public String toString() {
        return "Task{" +
                "taskId='" + taskId + '\'' +
                ", data='" + data + '\'' +
                ", operation='" + operation + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
