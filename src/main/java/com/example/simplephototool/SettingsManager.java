package com.example.simplephototool;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Manages loading and saving application settings to a configuration file.
 */
public class SettingsManager {
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.config/simplephototool";
    private static final String CONFIG_FILE = CONFIG_DIR + "/settings.properties";
    private static final String CAMERA_FILE = CONFIG_DIR + "/cameras.txt";

    /**
     * Loads settings from the configuration file.
     * 
     * @return Settings object with loaded configuration, or default settings if file doesn't exist
     */
    public static Settings loadSettings() {
        Settings settings = new Settings();
        Properties props = new Properties();

        try {
            Path configPath = Paths.get(CONFIG_FILE);
            if (Files.exists(configPath)) {
                try (InputStream input = Files.newInputStream(configPath)) {
                    props.load(input);
                    
                    String outputDir = props.getProperty("snapshot.output.directory");
                    if (outputDir != null && !outputDir.isEmpty()) {
                        settings.setSnapshotOutputDirectory(outputDir);
                    }
                    
                    String filenamePattern = props.getProperty("filename.pattern");
                    if (filenamePattern != null && !filenamePattern.isEmpty()) {
                        settings.setFilenamePattern(filenamePattern);
                    }
                }
            }

            // Load cameras
            Path cameraPath = Paths.get(CAMERA_FILE);
            if (Files.exists(cameraPath)) {
                List<Camera> cameras = new ArrayList<>();
                List<String> lines = Files.readAllLines(cameraPath);
                
                for (String line : lines) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 2) {
                        Camera camera = new Camera(parts[0], parts[1]);
                        cameras.add(camera);
                    }
                }
                settings.setCameras(cameras);
            }
        } catch (IOException e) {
            System.err.println("Error loading settings: " + e.getMessage());
        }

        return settings;
    }

    /**
     * Saves settings to the configuration file.
     * 
     * @param settings Settings object to save
     * @throws IOException if there's an error writing to the file
     */
    public static void saveSettings(Settings settings) throws IOException {
        // Create config directory if it doesn't exist
        Path configDir = Paths.get(CONFIG_DIR);
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }

        // Save general settings
        Properties props = new Properties();
        props.setProperty("snapshot.output.directory", settings.getSnapshotOutputDirectory());
        props.setProperty("filename.pattern", settings.getFilenamePattern());

        try (OutputStream output = Files.newOutputStream(Paths.get(CONFIG_FILE))) {
            props.store(output, "SimplePhotoTool Settings");
        }

        // Save cameras
        List<String> lines = new ArrayList<>();
        for (Camera camera : settings.getCameras()) {
            lines.add(camera.getName() + "|" + camera.getDeviceId());
        }
        Files.write(Paths.get(CAMERA_FILE), lines);
    }
}
