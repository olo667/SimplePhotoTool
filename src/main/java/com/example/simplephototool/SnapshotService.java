package com.example.simplephototool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Service for capturing snapshots from cameras using JavaCV on all platforms.
 * Uses Strategy pattern for platform-specific configuration.
 */
public class SnapshotService {
    
    private static final CameraStrategy strategy = CameraStrategyFactory.getStrategy();
    
    /**
     * Captures snapshots from all active cameras in parallel.
     * Each camera runs on its own thread for faster overall capture.
     * 
     * @param cameras List of all cameras
     * @param settings Application settings containing output directory and filename pattern
     * @return Number of successful snapshots
     */
    public static int captureSnapshots(List<Camera> cameras, Settings settings) {
        // Filter only active cameras
        List<Camera> activeCameras = cameras.stream()
                .filter(Camera::isActive)
                .collect(Collectors.toList());
        
        if (activeCameras.isEmpty()) {
            System.out.println("No active cameras to capture from.");
            return 0;
        }

        // Ensure output directory exists
        Path outputPath = Paths.get(settings.getSnapshotOutputDirectory());
        try {
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }
        } catch (IOException e) {
            System.err.println("Failed to create output directory: " + e.getMessage());
            return 0;
        }

        // Create a thread pool with one thread per camera for parallel capture
        ExecutorService executor = Executors.newFixedThreadPool(activeCameras.size());
        AtomicInteger successCount = new AtomicInteger(0);
        
        // Submit all camera capture tasks in parallel
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Camera camera : activeCameras) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    if (strategy.captureSnapshot(camera, settings)) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("Error capturing from camera " + camera.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }, executor);
            futures.add(future);
        }
        
        // Wait for all captures to complete
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            System.err.println("Error waiting for snapshot captures: " + e.getMessage());
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("Captured " + successCount.get() + " of " + activeCameras.size() + " snapshots.");
        return successCount.get();
    }
}
