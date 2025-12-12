package com.example.simplephototool;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;

public class MainController {
    @FXML
    private ListView<String> cameraList;

    public MainController(ListView<String> cameraList) {
        this.cameraList = cameraList;
    }
}