package com.example.simplephototool;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing application settings.
 */
public class Settings {
    private String snapshotOutputDirectory;
    private List<Camera> cameras;

    public Settings() {
        this.snapshotOutputDirectory = System.getProperty("user.home") + "/Pictures/SimplePhotoTool";
        this.cameras = new ArrayList<>();
    }

    public String getSnapshotOutputDirectory() {
        return snapshotOutputDirectory;
    }

    public void setSnapshotOutputDirectory(String snapshotOutputDirectory) {
        this.snapshotOutputDirectory = snapshotOutputDirectory;
    }

    public List<Camera> getCameras() {
        return cameras;
    }

    public void setCameras(List<Camera> cameras) {
        this.cameras = cameras;
    }
}
