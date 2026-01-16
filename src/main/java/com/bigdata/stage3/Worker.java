package com.bigdata.stage3;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.collection.IQueue;

import java.util.concurrent.TimeUnit;

/**
 * Worker node that processes tasks from the distributed queue.
 * Usage: java -cp target/stage3-hazelcast-1.0.0.jar:~/.m2/repository/com/hazelcast/hazelcast/5.3.6/hazelcast-5.3.6.jar com.bigdata.stage3.Worker [workerId]
 */
public class Worker {
    
    private final String workerId;
    private final HazelcastInstance hazelcast;
    private final IQueue<Task> taskQueue;
    private final IMap<String, Result> resultMap;
    private volatile boolean running = true;
    private int processedTasks = 0;
    
    public Worker(String workerId) {
        this.workerId = workerId;
        
        // Create Hazelcast instance with local config
        this.hazelcast = Hazelcast.newHazelcastInstance(HazelcastConfig.createLocalConfig());
        
        // Get distributed queue and map
        this.taskQueue = hazelcast.getQueue(HazelcastConfig.TASK_QUEUE_NAME);
        this.resultMap = hazelcast.getMap(HazelcastConfig.RESULT_MAP_NAME);
        
        System.out.println("=== WORKER STARTED ===");
        System.out.println("Worker ID: " + workerId);
        System.out.println("Cluster: " + hazelcast.getCluster().getClusterState());
        System.out.println("Members: " + hazelcast.getCluster().getMembers().size());
        System.out.println("Ready to process tasks...\n");
    }
    
    /**
     * Starts processing tasks from the queue.
     */
    public void start() {
        while (running) {
            try {
                // Poll task from queue with timeout
                Task task = taskQueue.poll(2, TimeUnit.SECONDS);
                
                if (task != null) {
                    processTask(task);
                    processedTasks++;
                    
                    if (processedTasks % 10 == 0) {
                        System.out.println(workerId + " processed " + processedTasks + " tasks");
                    }
                }
            } catch (InterruptedException e) {
                System.out.println(workerId + " interrupted");
                Thread.currentThread().interrupt();
                running = false;
            } catch (Exception e) {
                System.err.println(workerId + " error: " + e.getMessage());
            }
        }
        
        System.out.println("\n" + workerId + " total tasks processed: " + processedTasks);
    }
    
    /**
     * Processes a single task and stores the result.
     */
    private void processTask(Task task) {
        long startTime = System.currentTimeMillis();
        
        try {
            String result = performOperation(task.getOperation(), task.getData());
            long processingTime = System.currentTimeMillis() - startTime;
            
            Result resultObject = new Result(task.getTaskId(), workerId, result, processingTime);
            resultMap.put(task.getTaskId(), resultObject);
            
            // Log individual task processing
            System.out.println(workerId + " completed " + task.getTaskId() + 
                    " (" + task.getOperation() + ") in " + processingTime + "ms");
            
        } catch (Exception e) {
            Result errorResult = new Result(task.getTaskId(), workerId, "Error: " + e.getMessage());
            resultMap.put(task.getTaskId(), errorResult);
            System.err.println(workerId + " failed " + task.getTaskId() + ": " + e.getMessage());
        }
    }
    
    /**
     * Performs the specified operation on the data.
     */
    private String performOperation(String operation, String data) {
        switch (operation) {
            case "CALCULATE_PRIME":
                return calculatePrime(data);
            case "UPPERCASE":
                return data.toUpperCase();
            case "REVERSE":
                return new StringBuilder(data).reverse().toString();
            default:
                return "Unknown operation: " + operation;
        }
    }
    
    /**
     * Calculates if a number is prime and returns the next prime.
     */
    private String calculatePrime(String data) {
        try {
            int number = Integer.parseInt(data);
            int nextPrime = findNextPrime(number);
            return "Next prime after " + number + " is " + nextPrime;
        } catch (NumberFormatException e) {
            return "Invalid number: " + data;
        }
    }
    
    /**
     * Finds the next prime number after the given number.
     */
    private int findNextPrime(int n) {
        int candidate = n + 1;
        while (!isPrime(candidate)) {
            candidate++;
        }
        return candidate;
    }
    
    /**
     * Checks if a number is prime.
     */
    private boolean isPrime(int n) {
        if (n <= 1) return false;
        if (n <= 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;
        
        for (int i = 5; i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) {
                return false;
            }
        }
        return true;
    }
    
    public void shutdown() {
        System.out.println("\n=== SHUTTING DOWN WORKER " + workerId + " ===");
        running = false;
        hazelcast.shutdown();
    }
    
    public static void main(String[] args) {
        String workerId = "WORKER-" + System.currentTimeMillis() % 1000;
        
        if (args.length > 0) {
            workerId = args[0];
        }
        
        Worker worker = new Worker(workerId);
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(worker::shutdown));
        
        // Start processing
        worker.start();
    }
}
