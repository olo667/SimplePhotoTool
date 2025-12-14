package com.example.simplephototool;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for the Edit Camera dialog.
 * Allows editing camera name and resolution settings.
 */
public class EditCameraDialogController {
    @FXML
    private Label deviceIdLabel;

    @FXML
    private TextField cameraNameField;

    @FXML
    private ComboBox<String> resolutionComboBox;

    @FXML
    private CheckBox useDefaultResolutionCheckBox;

    @FXML
    private Button cancelButton;

    @FXML
    private Button saveButton;

    private Camera camera;
    private boolean saveClicked = false;

    @FXML
    public void initialize() {
        // Initialize resolution combo box with available options
        resolutionComboBox.setItems(FXCollections.observableArrayList(Settings.RESOLUTION_OPTIONS));
        
        // Bind resolution combo box disable state to the checkbox
        resolutionComboBox.disableProperty().bind(useDefaultResolutionCheckBox.selectedProperty());
        
        // Disable Save button if name is empty
        saveButton.disableProperty().bind(cameraNameField.textProperty().isEmpty());
    }

    /**
     * Sets the camera to be edited.
     * 
     * @param camera The camera to edit
     */
    public void setCamera(Camera camera) {
        this.camera = camera;
        if (camera != null) {
            deviceIdLabel.setText("Device ID: " + camera.getDeviceId());
            cameraNameField.setText(camera.getName());
            
            // Set resolution settings
            String cameraResolution = camera.getResolution();
            if (cameraResolution == null || cameraResolution.isEmpty()) {
                // Camera uses default resolution
                useDefaultResolutionCheckBox.setSelected(true);
                resolutionComboBox.setValue(Settings.DEFAULT_RESOLUTION);
            } else {
                // Camera has custom resolution
                useDefaultResolutionCheckBox.setSelected(false);
                resolutionComboBox.setValue(cameraResolution);
            }
        }
    }

    @FXML
    private void onCancel() {
        saveClicked = false;
        closeDialog();
    }

    @FXML
    private void onSave() {
        if (camera != null) {
            String name = cameraNameField.getText().trim();
            if (!name.isEmpty()) {
                camera.setName(name);
                
                // Set resolution based on checkbox state
                if (useDefaultResolutionCheckBox.isSelected()) {
                    camera.setResolution(null); // Use default
                } else {
                    camera.setResolution(resolutionComboBox.getValue());
                }
                
                saveClicked = true;
            }
        }
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Returns true if the user clicked Save.
     * 
     * @return true if saved
     */
    public boolean isSaveClicked() {
        return saveClicked;
    }
}
