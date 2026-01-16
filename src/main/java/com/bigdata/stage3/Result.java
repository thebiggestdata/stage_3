package com.bigdata.stage3;

import java.io.Serializable;
import java.time.Instant;

/**
 * Represents the result of a processed task.
 * Results are serializable so they can be stored in Hazelcast distributed maps.
 */
public class Result implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String taskId;
    private final String workerId;
    private final String result;
    private final long finishedAt;
    private final long processingTimeMs;
    private final boolean success;
    private final String errorMessage;
    
    /**
     * Creates a successful result.
     */
    public Result(String taskId, String workerId, String result, long processingTimeMs) {
        this.taskId = taskId;
        this.workerId = workerId;
        this.result = result;
        this.processingTimeMs = processingTimeMs;
        this.finishedAt = Instant.now().toEpochMilli();
        this.success = true;
        this.errorMessage = null;
    }
    
    /**
     * Creates a failed result.
     */
    public Result(String taskId, String workerId, String errorMessage) {
        this.taskId = taskId;
        this.workerId = workerId;
        this.result = null;
        this.processingTimeMs = 0;
        this.finishedAt = Instant.now().toEpochMilli();
        this.success = false;
        this.errorMessage = errorMessage;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public String getWorkerId() {
        return workerId;
    }
    
    public String getResult() {
        return result;
    }
    
    public long getFinishedAt() {
        return finishedAt;
    }
    
    public long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    @Override
    public String toString() {
        if (success) {
            return "Result{" +
                    "taskId='" + taskId + '\'' +
                    ", workerId='" + workerId + '\'' +
                    ", result='" + result + '\'' +
                    ", processingTimeMs=" + processingTimeMs +
                    '}';
        } else {
            return "Result{" +
                    "taskId='" + taskId + '\'' +
                    ", workerId='" + workerId + '\'' +
                    ", error='" + errorMessage + '\'' +
                    '}';
        }
    }
}
