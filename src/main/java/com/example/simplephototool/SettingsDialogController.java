package com.example.simplephototool;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Controller for the settings dialog.
 */
public class SettingsDialogController {
    @FXML
    private TextField outputDirectoryField;
    
    @FXML
    private TextField filenamePatternField;

    @FXML
    private ComboBox<String> defaultResolutionComboBox;
    
    @FXML
    private CheckBox hardwareEncodingCheckbox;
    
    @FXML
    private Label encoderStatusLabel;

    private Settings settings;
    private Stage dialogStage;
    private boolean saveClicked = false;

    /**
     * Initializes the controller.
     */
    @FXML
    private void initialize() {
        // Initialize resolution combo box with available options
        defaultResolutionComboBox.setItems(FXCollections.observableArrayList(Settings.RESOLUTION_OPTIONS));
        
        // Detect available hardware encoders in background and update status label
        new Thread(() -> {
            List<HardwareEncoderFactory.EncoderType> encoders = HardwareEncoderFactory.detectAvailableEncoders();
            HardwareEncoderFactory.EncoderType best = HardwareEncoderFactory.getBestAvailableEncoder();
            javafx.application.Platform.runLater(() -> {
                int hwCount = encoders.size() - 1; // Subtract software encoder
                if (hwCount > 0) {
                    encoderStatusLabel.setText("Best available: " + best.getDisplayName());
                    encoderStatusLabel.setStyle("-fx-text-fill: green; -fx-font-size: 10px;");
                } else {
                    encoderStatusLabel.setText("No hardware encoders detected");
                    encoderStatusLabel.setStyle("-fx-text-fill: orange; -fx-font-size: 10px;");
                }
            });
        }).start();
    }

    /**
     * Sets the dialog stage.
     * 
     * @param dialogStage the dialog stage
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * Sets the settings to be edited in the dialog.
     * 
     * @param settings the settings object
     */
    public void setSettings(Settings settings) {
        this.settings = settings;
        outputDirectoryField.setText(settings.getSnapshotOutputDirectory());
        filenamePatternField.setText(settings.getFilenamePattern());
        
        // Set default resolution selection
        String currentResolution = settings.getDefaultResolution();
        if (currentResolution != null && !currentResolution.isEmpty()) {
            defaultResolutionComboBox.setValue(currentResolution);
        } else {
            defaultResolutionComboBox.setValue(Settings.DEFAULT_RESOLUTION);
        }
        
        // Set hardware encoding checkbox
        hardwareEncodingCheckbox.setSelected(settings.isHardwareEncodingEnabled());
    }

    /**
     * Returns true if the user clicked Save, false otherwise.
     * 
     * @return true if saved
     */
    public boolean isSaveClicked() {
        return saveClicked;
    }

    /**
     * Handles the browse button action to select output directory.
     */
    @FXML
    private void handleBrowse() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Snapshot Output Directory");
        
        // Set initial directory if it exists
        File currentDir = new File(outputDirectoryField.getText());
        if (currentDir.exists()) {
            directoryChooser.setInitialDirectory(currentDir);
        }

        File selectedDirectory = directoryChooser.showDialog(dialogStage);
        if (selectedDirectory != null) {
            outputDirectoryField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    /**
     * Handles the save button action.
     */
    @FXML
    private void handleSave() {
        settings.setSnapshotOutputDirectory(outputDirectoryField.getText());
        settings.setFilenamePattern(filenamePatternField.getText());
        settings.setDefaultResolution(defaultResolutionComboBox.getValue());
        settings.setHardwareEncodingEnabled(hardwareEncodingCheckbox.isSelected());
        
        try {
            SettingsManager.saveSettings(settings);
            saveClicked = true;
            dialogStage.close();
        } catch (IOException e) {
            System.err.println("Error saving settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles the cancel button action.
     */
    @FXML
    private void handleCancel() {
        dialogStage.close();
    }
}
