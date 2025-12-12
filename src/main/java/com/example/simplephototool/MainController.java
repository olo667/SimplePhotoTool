package com.example.simplephototool;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class MainController {
    @FXML
    private ListView<Camera> cameraList;

    @FXML
    private Button addCamera;

    @FXML
    private ImageView previewImageView;

    private ObservableList<Camera> cameras = FXCollections.observableArrayList();
    private ToggleGroup previewToggleGroup = new ToggleGroup();
    private CameraPreviewService previewService;
    private Settings settings;

    @FXML
    public void initialize() {
        // Load settings
        settings = SettingsManager.loadSettings();
        cameras.addAll(settings.getCameras());
        
        // Set up the camera list with custom cell factory
        cameraList.setItems(cameras);
        cameraList.setCellFactory(listView -> new CameraListCell(this::deleteCamera, previewToggleGroup, this::onPreviewSelected));

        previewService = new CameraPreviewService();
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
                // Settings already saved by the controller
                System.out.println("Settings updated");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteCamera(Camera camera) {
        cameras.remove(camera);
        saveCameras();
    }

    private void onPreviewSelected(Camera camera) {
        if (camera == null) return;
        // start preview for the selected camera's deviceId
        if (previewService != null) {
            previewService.startPreview(camera.getDeviceId(), previewImageView);
        }
    }

    public void stopPreview() {
        if (previewService != null) {
            previewService.stopPreview();
        }
    }
}