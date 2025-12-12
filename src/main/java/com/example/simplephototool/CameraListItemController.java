package com.example.simplephototool;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

import java.util.function.Consumer;

/**
 * Controller for the camera list item FXML.
 */
public class CameraListItemController {
    @FXML
    private HBox rootPane;

    @FXML
    private CheckBox activeCheckBox;

    @FXML
    private Label cameraNameLabel;

    @FXML
    private Button deleteButton;
    @FXML
    private RadioButton previewRadio;

    private Camera camera;
    private Consumer<Camera> onDeleteCallback;
    private Consumer<Camera> onPreviewCallback;

    /**
     * Sets the camera to display in this list item.
     */
    public void setCamera(Camera camera) {
        this.camera = camera;
        if (camera != null) {
            cameraNameLabel.setText(camera.getName());
            activeCheckBox.setSelected(camera.isActive());
            
            // Bind the checkbox to the camera's active property
            activeCheckBox.selectedProperty().bindBidirectional(camera.activeProperty());
            
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

    public void setOnPreviewCallback(Consumer<Camera> callback) {
        this.onPreviewCallback = callback;
        if (previewRadio != null) {
            previewRadio.setOnAction(e -> {
                if (previewRadio.isSelected() && onPreviewCallback != null) {
                    onPreviewCallback.accept(camera);
                }
            });
        }
    }

    public void setToggleGroup(ToggleGroup group) {
        if (previewRadio != null) {
            previewRadio.setToggleGroup(group);
        }
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
