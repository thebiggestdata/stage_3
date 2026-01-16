package com.bigdata.stage3;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.collection.IQueue;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Master node that distributes tasks to workers and collects results.
 * Usage: java -cp target/stage3-hazelcast-1.0.0.jar:~/.m2/repository/com/hazelcast/hazelcast/5.3.6/hazelcast-5.3.6.jar com.bigdata.stage3.Master [numberOfTasks]
 */
public class Master {
    
    private final HazelcastInstance hazelcast;
    private final IQueue<Task> taskQueue;
    private final IMap<String, Result> resultMap;
    
    public Master() {
        // Create Hazelcast instance with local config
        this.hazelcast = Hazelcast.newHazelcastInstance(HazelcastConfig.createLocalConfig());
        
        // Get distributed queue and map
        this.taskQueue = hazelcast.getQueue(HazelcastConfig.TASK_QUEUE_NAME);
        this.resultMap = hazelcast.getMap(HazelcastConfig.RESULT_MAP_NAME);
        
        System.out.println("=== MASTER NODE STARTED ===");
        System.out.println("Cluster: " + hazelcast.getCluster().getClusterState());
        System.out.println("Members: " + hazelcast.getCluster().getMembers().size());
        System.out.println("Waiting for workers to connect...");
    }
    
    /**
     * Distributes tasks to the queue for workers to process.
     */
    public void distributeTasks(int numberOfTasks) {
        System.out.println("\n=== DISTRIBUTING TASKS ===");
        
        // Clear previous results
        resultMap.clear();
        
        for (int i = 0; i < numberOfTasks; i++) {
            String taskId = "TASK-" + (i + 1);
            Task task;
            
            // Create different types of tasks
            if (i % 3 == 0) {
                task = new Task(taskId, String.valueOf(i + 100), "CALCULATE_PRIME");
            } else if (i % 3 == 1) {
                task = new Task(taskId, "hello world " + i, "UPPERCASE");
            } else {
                task = new Task(taskId, "processing " + i, "REVERSE");
            }
            
            try {
                taskQueue.put(task);
                if ((i + 1) % 10 == 0) {
                    System.out.println("Distributed " + (i + 1) + " tasks...");
                }
            } catch (InterruptedException e) {
                System.err.println("Error distributing task: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("Total tasks distributed: " + numberOfTasks);
    }
    
    /**
     * Monitors the progress and displays results.
     */
    public void monitorResults(int expectedResults) {
        System.out.println("\n=== MONITORING RESULTS ===");
        
        long startTime = System.currentTimeMillis();
        int lastReportedCount = 0;
        
        while (resultMap.size() < expectedResults) {
            try {
                int currentSize = resultMap.size();
                
                if (currentSize > lastReportedCount) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double throughput = (currentSize * 1000.0) / elapsed;
                    System.out.printf("Progress: %d/%d (%.1f%%) - Throughput: %.2f tasks/sec - Active Workers: %d%n",
                            currentSize, expectedResults, 
                            (currentSize * 100.0 / expectedResults),
                            throughput,
                            hazelcast.getCluster().getMembers().size() - 1);
                    lastReportedCount = currentSize;
                }
                
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        showStatistics(expectedResults, totalTime);
    }
    
    /**
     * Displays final statistics.
     */
    private void showStatistics(int totalTasks, long totalTimeMs) {
        System.out.println("\n=== PROCESSING COMPLETE ===");
        System.out.println("Total tasks: " + totalTasks);
        System.out.println("Total time: " + totalTimeMs + " ms");
        System.out.println("Average throughput: " + String.format("%.2f", (totalTasks * 1000.0 / totalTimeMs)) + " tasks/sec");
        
        // Count results by worker
        System.out.println("\n=== RESULTS BY WORKER ===");
        resultMap.values().stream()
                .filter(Result::isSuccess)
                .collect(java.util.stream.Collectors.groupingBy(Result::getWorkerId, java.util.stream.Collectors.counting()))
                .forEach((workerId, count) -> 
                    System.out.println(workerId + ": " + count + " tasks"));
        
        // Show some sample results
        System.out.println("\n=== SAMPLE RESULTS ===");
        resultMap.values().stream()
                .limit(5)
                .forEach(System.out::println);
        
        long successCount = resultMap.values().stream().filter(Result::isSuccess).count();
        long failureCount = resultMap.values().stream().filter(r -> !r.isSuccess()).count();
        System.out.println("\nSuccess: " + successCount + ", Failures: " + failureCount);
    }
    
    public void shutdown() {
        System.out.println("\n=== SHUTTING DOWN MASTER ===");
        hazelcast.shutdown();
    }
    
    public static void main(String[] args) {
        int numberOfTasks = 100;
        
        if (args.length > 0) {
            try {
                numberOfTasks = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid number of tasks. Using default: 100");
            }
        }
        
        Master master = new Master();
        
        // Wait a bit for workers to connect
        System.out.println("\nPress ENTER when workers are ready, or wait 5 seconds...");
        try {
            Scanner scanner = new Scanner(System.in);
            long waitStart = System.currentTimeMillis();
            while (!scanner.hasNextLine() && (System.currentTimeMillis() - waitStart) < 5000) {
                TimeUnit.MILLISECONDS.sleep(100);
            }
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Distribute tasks and monitor
        master.distributeTasks(numberOfTasks);
        master.monitorResults(numberOfTasks);
        
        // Keep master running for a bit
        System.out.println("\nMaster will shutdown in 5 seconds...");
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        master.shutdown();
    }
}
