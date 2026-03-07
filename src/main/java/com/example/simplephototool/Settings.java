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
    private Boolean hardwareEncodingEnabled;

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
        this.hardwareEncodingEnabled = false;
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
        if (resolution == null || resolution.isEmpty()) {
            return null;
        }
        try {
            String[] parts = resolution.toLowerCase().split("x");
            if (parts.length == 2) {
                int width = Integer.parseInt(parts[0].trim());
                int height = Integer.parseInt(parts[1].trim());
                return new int[] { width, height };
            }
        } catch (NumberFormatException e) {
            // Invalid format
        }
        return null;
    }

    public boolean getVerboseOutput() {
        return verboseOutput;
    }

    public void setVerboseOutput(boolean verboseOutput) {
        this.verboseOutput = verboseOutput;
    }
    
    public boolean isHardwareEncodingEnabled() {
        return hardwareEncodingEnabled != null && hardwareEncodingEnabled;
    }
    
    public void setHardwareEncodingEnabled(boolean enabled) {
        this.hardwareEncodingEnabled = enabled;
    }
    
    /**
     * Gets the encoder type based on current settings.
     * If hardware encoding is enabled, auto-selects best available.
     * If disabled, returns SOFTWARE.
     *
     * @return The encoder type to use
     */
    public HardwareEncoderFactory.EncoderType getEncoderType() {
        if (!isHardwareEncodingEnabled()) {
            return HardwareEncoderFactory.EncoderType.SOFTWARE;
        }
        return HardwareEncoderFactory.getBestAvailableEncoder();
    }
}
