package com.example.simplephototool;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages camera preview items for active cameras.
 * Creates and maintains CameraPreviewItem instances for cameras with active checkbox ticked.
 */
public class PreviewManager {
    
    private final ObservableList<Camera> cameras;
    private Settings settings;
    private final Map<String, CameraPreviewItem> previewItems = new HashMap<>();
    
    // Keep PreviewTile for backward compatibility
    private final Map<String, PreviewTile> tiles = new HashMap<>();
    
    private TileSize currentTileSize = TileSize.MEDIUM;
    
    /**
     * Creates a new PreviewManager.
     *
     * @param cameras Observable list of cameras
     * @param settings Application settings
     */
    public PreviewManager(ObservableList<Camera> cameras, Settings settings) {
        this.cameras = cameras;
        this.settings = settings;
        
        // Build initial preview items for active cameras
        refresh();
        
        // Listen for changes to the camera list
        cameras.addListener((ListChangeListener<Camera>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved() || change.wasUpdated()) {
                    refresh();
                }
            }
        });
    }
    
    /**
     * Refreshes the preview items based on cameras with preview enabled.
     */
    public void refresh() {
        // Create preview items for cameras with preview enabled that don't have one yet
        for (Camera camera : cameras) {
            String deviceId = camera.getDeviceId();
            
            // Listen to previewEnabled property changes
            camera.previewEnabledProperty().addListener((obs, wasEnabled, isEnabled) -> {
                if (isEnabled && !previewItems.containsKey(deviceId)) {
                    createPreviewItem(camera);
                } else if (!isEnabled && previewItems.containsKey(deviceId)) {
                    removePreviewItem(deviceId);
                }
            });
            
            if (camera.isPreviewEnabled() && !previewItems.containsKey(deviceId)) {
                createPreviewItem(camera);
            } else if (!camera.isPreviewEnabled() && previewItems.containsKey(deviceId)) {
                removePreviewItem(deviceId);
            }
            
            // Also create legacy PreviewTile for backward compatibility
            if (!tiles.containsKey(deviceId)) {
                PreviewTile tile = new PreviewTile(camera, settings, currentTileSize);
                tiles.put(deviceId, tile);
            }
        }
        
        // Remove preview items for cameras that no longer exist
        previewItems.keySet().removeIf(deviceId -> 
            cameras.stream().noneMatch(c -> c.getDeviceId().equals(deviceId)));
        tiles.keySet().removeIf(deviceId -> 
            cameras.stream().noneMatch(c -> c.getDeviceId().equals(deviceId)));
    }
    
    /**
     * Creates a preview item for the specified camera.
     */
    private void createPreviewItem(Camera camera) {
        CameraPreviewItem item = new CameraPreviewItem(camera, settings, currentTileSize);
        previewItems.put(camera.getDeviceId(), item);
    }
    
    /**
     * Removes and disposes a preview item.
     */
    private void removePreviewItem(String deviceId) {
        CameraPreviewItem item = previewItems.remove(deviceId);
        if (item != null) {
            item.dispose();
        }
    }
    
    /**
     * Gets all preview items for active cameras.
     *
     * @return Map of device ID to CameraPreviewItem
     */
    public Map<String, CameraPreviewItem> getPreviewItems() {
        return new HashMap<>(previewItems);
    }
    
    /**
     * Gets a specific preview item by device ID.
     *
     * @param deviceId The device ID
     * @return The preview item, or null if not found
     */
    public CameraPreviewItem getPreviewItem(String deviceId) {
        return previewItems.get(deviceId);
    }
    
    /**
     * Starts previews for all active cameras.
     */
    public void startAllPreviews() {
        for (CameraPreviewItem item : previewItems.values()) {
            if (!item.isRunning()) {
                item.startPreview();
            }
        }
    }
    
    /**
     * Stops all running previews.
     */
    public void stopAllPreviews() {
        for (CameraPreviewItem item : previewItems.values()) {
            if (item.isRunning()) {
                item.stopPreview();
            }
        }
        
        // Also stop legacy tiles
        for (PreviewTile tile : tiles.values()) {
            if (tile.isRunning()) {
                tile.stopPreview();
            }
        }
        
        // Reset port counter when all streams are stopped
        FFmpegStreamService.resetPortCounter();
    }
    
    /**
     * Starts preview for a specific camera.
     *
     * @param deviceId The device ID
     */
    public void startPreview(String deviceId) {
        CameraPreviewItem item = previewItems.get(deviceId);
        if (item != null && !item.isRunning()) {
            item.startPreview();
        }
        
        // Also check legacy tiles
        PreviewTile tile = tiles.get(deviceId);
        if (tile != null && !tile.isRunning()) {
            tile.startPreview();
        }
    }
    
    /**
     * Stops preview for a specific camera.
     *
     * @param deviceId The device ID
     */
    public void stopPreview(String deviceId) {
        CameraPreviewItem item = previewItems.get(deviceId);
        if (item != null && item.isRunning()) {
            item.stopPreview();
        }
        
        // Also check legacy tiles
        PreviewTile tile = tiles.get(deviceId);
        if (tile != null && tile.isRunning()) {
            tile.stopPreview();
        }
    }
    
    /**
     * Updates settings.
     *
     * @param settings The new settings
     */
    public void setSettings(Settings settings) {
        this.settings = settings;
        for (CameraPreviewItem item : previewItems.values()) {
            item.setSettings(settings);
        }
        for (PreviewTile tile : tiles.values()) {
            tile.setSettings(settings);
        }
    }
    
    /**
     * Sets the tile size for all preview items.
     *
     * @param size The new tile size
     */
    public void setTileSize(TileSize size) {
        this.currentTileSize = size;
        for (CameraPreviewItem item : previewItems.values()) {
            item.setTileSize(size);
        }
        for (PreviewTile tile : tiles.values()) {
            tile.setTileSize(size);
        }
    }
    
    /**
     * Shuts down all resources.
     */
    public void shutdown() {
        stopAllPreviews();
        for (CameraPreviewItem item : previewItems.values()) {
            item.dispose();
        }
        for (PreviewTile tile : tiles.values()) {
            tile.dispose();
        }
        previewItems.clear();
        tiles.clear();
    }
    
    // Legacy methods for backward compatibility with PreviewTile
    
    /**
     * Gets a preview tile by device ID (legacy).
     *
     * @param deviceId The device ID
     * @return The preview tile
     */
    public PreviewTile getTile(String deviceId) {
        return tiles.get(deviceId);
    }
    
    /**
     * Gets all preview tiles (legacy).
     *
     * @return Map of device ID to PreviewTile
     */
    public Map<String, PreviewTile> getTiles() {
        return new HashMap<>(tiles);
    }
}
