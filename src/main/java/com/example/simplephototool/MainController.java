package com.example.simplephototool;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Map;

public class MainController {
    @FXML
    private ListView<Camera> cameraList;

    @FXML
    private Button addCamera;

    @FXML
    private GridPane previewGrid;
    
    @FXML
    private ScrollPane previewScrollPane;

    private ObservableList<Camera> cameras = FXCollections.observableArrayList();
    private PreviewManager previewManager;
    private Settings settings;

    @FXML
    public void initialize() {
        // Load settings
        settings = SettingsManager.loadSettings();
        cameras.addAll(settings.getCameras());
        
        // Set up the camera list with custom cell factory
        cameraList.setItems(cameras);
        cameraList.setCellFactory(listView -> new CameraListCell(
            this::deleteCamera, 
            this::editCamera, 
            this::onPreviewSelected
        ));

        // Initialize preview manager with camera list
        previewManager = new PreviewManager(cameras, settings);
        
        // Populate the grid with preview items for cameras with preview enabled
        rebuildPreviewGrid();

        // Listen for camera list changes to update grid
        cameras.addListener((ListChangeListener<Camera>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved()) {
                    previewManager.refresh();
                    rebuildPreviewGrid();
                }
            }
        });
        
        // Listen for previewEnabled property changes on each camera
        for (Camera camera : cameras) {
            camera.previewEnabledProperty().addListener((obs, wasEnabled, isEnabled) -> {
                previewManager.refresh();
                rebuildPreviewGrid();
            });
        }
    }

    /**
     * Rebuilds the preview grid with CameraPreviewItems for cameras with preview enabled.
     * Items are arranged in a grid with 2 columns.
     */
    private void rebuildPreviewGrid() {
        previewGrid.getChildren().clear();
        
        int col = 0;
        int row = 0;
        int maxCols = 2; // Number of columns in the grid
        
        for (Camera camera : cameras) {
            // Only show preview items for cameras with preview enabled (previewActive checkbox ticked)
            if (!camera.isPreviewEnabled()) {
                continue;
            }
            
            CameraPreviewItem item = previewManager.getPreviewItem(camera.getDeviceId());
            if (item != null && !previewGrid.getChildren().contains(item)) {
                GridPane.setColumnIndex(item, col);
                GridPane.setRowIndex(item, row);
                previewGrid.getChildren().add(item);
                
                col++;
                if (col >= maxCols) {
                    col = 0;
                    row++;
                }
            }
        }
    }

    @FXML
    private void onAddCamera() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("add-camera-dialog.fxml"));
            Scene scene = new Scene(loader.load());
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add Camera");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(addCamera.getScene().getWindow());
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            
            dialogStage.showAndWait();
            
            AddCameraDialogController controller = loader.getController();
            Camera newCamera = controller.getResult();
            
            if (newCamera != null) {
                // Add listener for previewEnabled property changes
                newCamera.previewEnabledProperty().addListener((obs, wasEnabled, isEnabled) -> {
                    previewManager.refresh();
                    rebuildPreviewGrid();
                });
                cameras.add(newCamera);
                saveCameras();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveCameras() {
        settings.setCameras(cameras);
        try {
            SettingsManager.saveSettings(settings);
        } catch (IOException e) {
            System.err.println("Error saving cameras: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("settings-dialog.fxml"));
            Scene scene = new Scene(loader.load());
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Settings");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(addCamera.getScene().getWindow());
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            
            SettingsDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setSettings(settings);
            
            dialogStage.showAndWait();
            
            if (controller.isSaveClicked()) {
                // Update preview manager with new settings
                previewManager.setSettings(settings);
                System.out.println("Settings updated");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void onStartAllPreviews() {
        previewManager.startAllPreviews();
    }
    
    @FXML
    private void onStopAllPreviews() {
        previewManager.stopAllPreviews();
    }

    private void deleteCamera(Camera camera) {
        // Stop preview for this camera if running
        previewManager.stopPreview(camera.getDeviceId());
        cameras.remove(camera);
        saveCameras();
    }

    private void editCamera(Camera camera) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("edit-camera-dialog.fxml"));
            Scene scene = new Scene(loader.load());
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Camera");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(addCamera.getScene().getWindow());
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            
            EditCameraDialogController controller = loader.getController();
            controller.setCamera(camera);
            
            dialogStage.showAndWait();
            
            if (controller.isSaveClicked()) {
                // Refresh the list view to show updated name
                cameraList.refresh();
                // Refresh preview tiles
                previewManager.refresh();
                rebuildPreviewGrid();
                saveCameras();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onPreviewSelected(Camera camera) {
        if (camera == null) return;
        // Toggle preview for the selected camera
        PreviewTile tile = previewManager.getTile(camera.getDeviceId());
        if (tile != null) {
            tile.togglePreview();
        }
    }

    @FXML
    public void onTakeSnapshot() {
        // Collect running preview device IDs to restart after snapshot
        Map<String, PreviewTile> tiles = previewManager.getTiles();
        java.util.List<String> runningDevices = new java.util.ArrayList<>();
        
        for (Map.Entry<String, PreviewTile> entry : tiles.entrySet()) {
            if (entry.getValue().isRunning()) {
                runningDevices.add(entry.getKey());
            }
        }
        
        // Stop all previews before taking snapshot
        previewManager.stopAllPreviews();
        
        // Run snapshot capture in background thread
        new Thread(() -> {
            // Give the devices time to release
            if (!runningDevices.isEmpty()) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            
            int count = SnapshotService.captureSnapshots(cameras, settings);
            System.out.println("Captured " + count + " snapshots.");
            
            // Restart previews that were running
            if (!runningDevices.isEmpty()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                javafx.application.Platform.runLater(() -> {
                    for (String deviceId : runningDevices) {
                        previewManager.startPreview(deviceId);
                    }
                });
            }
        }).start();
    }

    public void stopPreview() {
        if (previewManager != null) {
            previewManager.stopAllPreviews();
        }
    }
    
    /**
     * Shuts down all resources. Call when application is closing.
     */
    public void shutdown() {
        if (previewManager != null) {
            previewManager.shutdown();
        }
    }
}
