package com.example.simplephototool;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.List;

/**
 * Controller for the Add Camera dialog.
 */
public class AddCameraDialogController {
    @FXML
    private ComboBox<CameraDevice> deviceComboBox;

    @FXML
    private TextField cameraLabelField;

    @FXML
    private Button cancelButton;

    @FXML
    private Button addButton;

    private Camera result = null;

    @FXML
    public void initialize() {
        // Load available camera devices
        List<CameraDevice> devices = CameraDeviceDetector.detectDevices();
        deviceComboBox.setItems(FXCollections.observableArrayList(devices));
        
        // Select first device by default
        if (!devices.isEmpty()) {
            deviceComboBox.getSelectionModel().selectFirst();
            // Set default label based on device name
            updateDefaultLabel();
        }
        
        // Update default label when device selection changes
        deviceComboBox.setOnAction(e -> updateDefaultLabel());
        
        // Disable Add button if no device selected or label is empty
        addButton.disableProperty().bind(
            deviceComboBox.getSelectionModel().selectedItemProperty().isNull()
            .or(cameraLabelField.textProperty().isEmpty())
        );
    }

    private void updateDefaultLabel() {
        CameraDevice selected = deviceComboBox.getSelectionModel().getSelectedItem();
        if (selected != null && cameraLabelField.getText().isEmpty()) {
            cameraLabelField.setText(selected.getDisplayName());
        }
    }

    @FXML
    private void onCancel() {
        result = null;
        closeDialog();
    }

    @FXML
    private void onAdd() {
        CameraDevice selectedDevice = deviceComboBox.getSelectionModel().getSelectedItem();
        String label = cameraLabelField.getText().trim();
        
        if (selectedDevice != null && !label.isEmpty()) {
            result = new Camera(label, selectedDevice.getDeviceId());
        }
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Returns the created Camera, or null if the dialog was cancelled.
     */
    public Camera getResult() {
        return result;
    }
}
