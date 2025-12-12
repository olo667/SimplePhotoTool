package com.example.simplephototool;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

/**
 * Controller for the settings dialog.
 */
public class SettingsDialogController {
    @FXML
    private TextField outputDirectoryField;

    private Settings settings;
    private Stage dialogStage;
    private boolean saveClicked = false;

    /**
     * Initializes the controller.
     */
    @FXML
    private void initialize() {
        // Initialization done in setSettings method
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
