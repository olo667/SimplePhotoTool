package com.example.simplephototool;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing application settings.
 */
public class Settings {
    private String snapshotOutputDirectory;
    private String filenamePattern;
    private String defaultResolution;
    private Boolean verboseOutput;
    private List<Camera> cameras;

    /** Common resolution options available for cameras */
    public static final String[] RESOLUTION_OPTIONS = {
        "640x480",
        "800x600",
        "1024x768",
        "1280x720",
        "1280x960",
        "1920x1080",
        "2560x1440",
        "3840x2160"
    };

    public static final String DEFAULT_RESOLUTION = "1280x720";

    public Settings() {
        this.snapshotOutputDirectory = System.getProperty("user.home") + "/Pictures/SimplePhotoTool";
        this.filenamePattern = "camera-{id}_{timestamp}.jpg";
        this.defaultResolution = DEFAULT_RESOLUTION;
        this.verboseOutput = false;
        this.cameras = new ArrayList<>();
    }

    public String getSnapshotOutputDirectory() {
        return snapshotOutputDirectory;
    }

    public void setSnapshotOutputDirectory(String snapshotOutputDirectory) {
        this.snapshotOutputDirectory = snapshotOutputDirectory;
    }

    public String getFilenamePattern() {
        return filenamePattern;
    }

    public void setFilenamePattern(String filenamePattern) {
        this.filenamePattern = filenamePattern;
    }

    public List<Camera> getCameras() {
        return cameras;
    }

    public void setCameras(List<Camera> cameras) {
        this.cameras = cameras;
    }

    public String getDefaultResolution() {
        return defaultResolution != null ? defaultResolution : DEFAULT_RESOLUTION;
    }

    public void setDefaultResolution(String defaultResolution) {
        this.defaultResolution = defaultResolution;
    }

    /**
     * Parses a resolution string into width and height.
     *
     * @param resolution Resolution string in format "WIDTHxHEIGHT"
     * @return int array with [width, height], or null if parsing fails
     */
    public static int[] parseResolution(String resolution) {
        return null;
    }

    public boolean getVerboseOutput() {
        return verboseOutput;
    }

    public void setVerboseOutput(boolean verboseOutput) {
        this.verboseOutput = verboseOutput;
    }
}
