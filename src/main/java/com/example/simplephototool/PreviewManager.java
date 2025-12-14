package com.example.simplephototool;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.layout.FlowPane;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple camera preview tiles in a grid layout.
 * Handles resource limits and provides centralized control over all previews.
 */
public class PreviewManager {
    
    private static final int DEFAULT_MAX_CONCURRENT = 4;
    
    private final FlowPane container;
    private final Map<String, PreviewTile> tiles = new ConcurrentHashMap<>();
    private final ObservableList<Camera> cameras;
    private Settings settings;
    private int maxConcurrentPreviews;
    private int runningCount = 0;
    
    /**
     * Creates a new PreviewManager with the specified settings.
     *
     * @param cameras Observable list of cameras to manage
     * @param settings Application settings
     */
    public PreviewManager(ObservableList<Camera> cameras, Settings settings) {
        this(cameras, settings, DEFAULT_MAX_CONCURRENT);
    }
    
    /**
     * Creates a new PreviewManager with the specified settings and limits.
     *
     * @param cameras Observable list of cameras to manage
     * @param settings Application settings
     * @param maxConcurrentPreviews Maximum number of simultaneous previews
     */
    public PreviewManager(ObservableList<Camera> cameras, Settings settings, int maxConcurrentPreviews) {
        this.cameras = cameras;
        this.settings = settings;
        this.maxConcurrentPreviews = maxConcurrentPreviews;
        
        // Create the container FlowPane
        container = new FlowPane();
        container.setHgap(10);
        container.setVgap(10);
        container.setPadding(new Insets(10));
        container.setStyle("-fx-background-color: #1a1a1a;");
        
        // Initial population
        rebuildTiles();
        
        // Listen for camera list changes
        cameras.addListener((ListChangeListener<Camera>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved()) {
                    Platform.runLater(this::rebuildTiles);
                }
            }
        });
    }
    
    /**
     * Rebuilds all tiles from the camera list.
     */
    private void rebuildTiles() {
        // Stop and remove old tiles
        for (PreviewTile tile : tiles.values()) {
            tile.dispose();
        }
        tiles.clear();
        container.getChildren().clear();
        runningCount = 0;
        
        // Create new tiles for each camera
        for (Camera camera : cameras) {
            addTile(camera);
        }
    }
    
    /**
     * Adds a tile for the specified camera.
     *
     * @param camera The camera to add
     */
    private void addTile(Camera camera) {
        PreviewTile tile = new PreviewTile(camera, settings);
        
        // Track running state for resource management
        tile.runningProperty().addListener((obs, wasRunning, isRunning) -> {
            if (isRunning) {
                runningCount++;
            } else {
                runningCount = Math.max(0, runningCount - 1);
            }
        });
        
        tiles.put(camera.getDeviceId(), tile);
        container.getChildren().add(tile);
    }
    
    /**
     * Gets the container node for embedding in the UI.
     *
     * @return The FlowPane container
     */
    public FlowPane getContainer() {
        return container;
    }
    
    /**
     * Starts preview for a specific camera.
     *
     * @param deviceId The device ID to start
     * @return true if started, false if at limit or not found
     */
    public boolean startPreview(String deviceId) {
        PreviewTile tile = tiles.get(deviceId);
        if (tile == null) {
            return false;
        }
        
        if (runningCount >= maxConcurrentPreviews) {
            System.out.println("Maximum concurrent previews reached (" + maxConcurrentPreviews + ")");
            return false;
        }
        
        tile.startPreview();
        return true;
    }
    
    /**
     * Stops preview for a specific camera.
     *
     * @param deviceId The device ID to stop
     */
    public void stopPreview(String deviceId) {
        PreviewTile tile = tiles.get(deviceId);
        if (tile != null) {
            tile.stopPreview();
        }
    }
    
    /**
     * Starts all camera previews (up to the limit).
     */
    public void startAllPreviews() {
        for (PreviewTile tile : tiles.values()) {
            if (runningCount >= maxConcurrentPreviews) {
                break;
            }
            if (!tile.isRunning()) {
                tile.startPreview();
            }
        }
    }
    
    /**
     * Stops all camera previews.
     */
    public void stopAllPreviews() {
        for (PreviewTile tile : tiles.values()) {
            tile.stopPreview();
        }
    }
    
    /**
     * Gets the number of currently running previews.
     *
     * @return Running preview count
     */
    public int getRunningCount() {
        return runningCount;
    }
    
    /**
     * Gets the maximum allowed concurrent previews.
     *
     * @return Maximum concurrent previews
     */
    public int getMaxConcurrentPreviews() {
        return maxConcurrentPreviews;
    }
    
    /**
     * Sets the maximum allowed concurrent previews.
     *
     * @param max New maximum
     */
    public void setMaxConcurrentPreviews(int max) {
        this.maxConcurrentPreviews = max;
    }
    
    /**
     * Updates settings for all tiles.
     *
     * @param settings New settings
     */
    public void setSettings(Settings settings) {
        this.settings = settings;
        for (PreviewTile tile : tiles.values()) {
            tile.setSettings(settings);
        }
    }
    
    /**
     * Gets a tile by device ID.
     *
     * @param deviceId The device ID
     * @return The tile or null if not found
     */
    public PreviewTile getTile(String deviceId) {
        return tiles.get(deviceId);
    }
    
    /**
     * Gets all tiles.
     *
     * @return Map of device IDs to tiles
     */
    public Map<String, PreviewTile> getTiles() {
        return new HashMap<>(tiles);
    }
    
    /**
     * Refreshes tiles (rebuilds from camera list).
     */
    public void refresh() {
        Platform.runLater(this::rebuildTiles);
    }
    
    /**
     * Cleans up all resources.
     */
    public void shutdown() {
        stopAllPreviews();
        tiles.clear();
        container.getChildren().clear();
    }
}
