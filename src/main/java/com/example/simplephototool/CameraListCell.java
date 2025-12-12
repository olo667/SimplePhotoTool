package com.example.simplephototool;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListCell;
import javafx.scene.control.ToggleGroup;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Custom ListCell for displaying Camera items with active/inactive toggle and delete action.
 */
public class CameraListCell extends ListCell<Camera> {
    private CameraListItemController controller;
    private final Consumer<Camera> onDeleteCallback;
    private final ToggleGroup previewToggleGroup;
    private final Consumer<Camera> onPreviewCallback;

    public CameraListCell(Consumer<Camera> onDeleteCallback, ToggleGroup previewToggleGroup, Consumer<Camera> onPreviewCallback) {
        this.onDeleteCallback = onDeleteCallback;
        this.previewToggleGroup = previewToggleGroup;
        this.onPreviewCallback = onPreviewCallback;
    }

    @Override
    protected void updateItem(Camera camera, boolean empty) {
        super.updateItem(camera, empty);

        if (empty || camera == null) {
            setText(null);
            setGraphic(null);
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("camera-list-item.fxml")
                );
                loader.load();
                controller = loader.getController();
                controller.setCamera(camera);
                controller.setOnDeleteCallback(onDeleteCallback);
                controller.setOnPreviewCallback(onPreviewCallback);
                controller.setToggleGroup(previewToggleGroup);
                setGraphic(controller.getRootPane());
            } catch (IOException e) {
                e.printStackTrace();
                setText(camera.getName());
            }
        }
    }
}
