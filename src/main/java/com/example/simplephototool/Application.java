package com.example.simplephototool;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class Application extends javafx.application.Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        
        // Get controller to call snapshot method
        MainController controller = fxmlLoader.getController();
        
        // Add keyboard shortcut for Enter key
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                controller.onTakeSnapshot();
                event.consume();
            }
        });
        
        // Handle window close to stop preview
        stage.setOnCloseRequest(event -> {
            controller.stopPreview();
        });
        
        stage.setTitle("Simple Photo Tool");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}