package com.example.simplephototool;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.function.Consumer;

/**
 * Controller for the camera list item FXML.
 */
public class CameraListItemController {
    @FXML
    private HBox rootPane;

    @FXML
    private CheckBox previewActive;

    @FXML
    private CheckBox activeCheckBox;

    @FXML
    private Label cameraNameLabel;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    private Camera camera;
    private Consumer<Camera> onDeleteCallback;
    private Consumer<Camera> onPreviewCallback;
    private Consumer<Camera> onEditCallback;

    /**
     * Sets the camera to display in this list item.
     */
    public void setCamera(Camera camera) {
        this.camera = camera;
        if (camera != null) {
            cameraNameLabel.setText(camera.getName());
            activeCheckBox.setSelected(camera.isActive());
            previewActive.setSelected(camera.isPreviewEnabled());
            
            // Bind the checkboxes to the camera's properties
            activeCheckBox.selectedProperty().bindBidirectional(camera.activeProperty());
            previewActive.selectedProperty().bindBidirectional(camera.previewEnabledProperty());
            
            // Update visual style based on active state
            updateActiveStyle();
        }
    }

    /**
     * Sets the callback to be invoked when the delete button is clicked.
     */
    public void setOnDeleteCallback(Consumer<Camera> callback) {
        this.onDeleteCallback = callback;
    }

    /**
     * Sets the callback to be invoked when the edit button is clicked.
     */
    public void setOnEditCallback(Consumer<Camera> callback) {
        this.onEditCallback = callback;
    }

    /**
     * Sets the callback to be invoked when the preview checkbox state changes.
     */
    public void setOnPreviewCallback(Consumer<Camera> callback) {
        this.onPreviewCallback = callback;
        if (previewActive != null) {
            previewActive.setOnAction(e -> {
                if (onPreviewCallback != null) {
                    onPreviewCallback.accept(camera);
                }
            });
        }
    }

    /**
     * Gets whether the preview checkbox is selected.
     */
    public boolean isPreviewActive() {
        return previewActive != null && previewActive.isSelected();
    }

    @FXML
    private void onActiveToggle() {
        if (camera != null) {
            updateActiveStyle();
        }
    }

    @FXML
    private void onDelete() {
        if (camera != null && onDeleteCallback != null) {
            onDeleteCallback.accept(camera);
        }
    }

    @FXML
    private void onEdit() {
        if (camera != null && onEditCallback != null) {
            onEditCallback.accept(camera);
        }
    }

    /**
     * Updates the visual style of the list item based on the active state.
     */
    private void updateActiveStyle() {
        if (camera.isActive()) {
            rootPane.setStyle("-fx-opacity: 1.0;");
            cameraNameLabel.setStyle("-fx-text-fill: black;");
        } else {
            rootPane.setStyle("-fx-opacity: 0.6;");
            cameraNameLabel.setStyle("-fx-text-fill: gray;");
        }
    }

    public HBox getRootPane() {
        return rootPane;
    }
}
