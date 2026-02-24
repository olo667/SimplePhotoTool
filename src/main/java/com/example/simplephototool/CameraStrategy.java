package com.example.simplephototool;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Strategy interface for platform-specific camera operations.
 * Uses FFmpeg for all camera operations across platforms.
 */
public interface CameraStrategy {
    
    /**
     * Detects available camera devices on the platform.
     * 
     * @return List of detected camera devices
     */
    List<CameraDevice> detectDevices();

    /**
     * Builds a platform-specific FFmpeg command for video capture.
     * The command should output raw RGB24 frames to stdout.
     *
     * @param camera The camera to preview
     * @param settings Application settings (for resolution, etc.)
     * @return ProcessBuilder configured with FFmpeg command
     */
    ProcessBuilder buildFFmpegCommand(Camera camera, Settings settings);

    /**
     * Gets the pixel format used by FFmpeg output for this platform.
     * Default is RGB24 (3 bytes per pixel).
     *
     * @return The pixel format name
     */
    default String getFFmpegPixelFormat() {
        return "rgb24";
    }

    /**
     * Gets the bytes per pixel for the FFmpeg output format.
     * Default is 3 for RGB24.
     *
     * @return Bytes per pixel
     */
    default int getBytesPerPixel() {
        return 3; // RGB24
    }

    /**
     * Gets the resolution dimensions for a camera.
     * Helper method to parse resolution string into width/height array.
     *
     * @param camera The camera
     * @param settings Application settings
     * @return int array [width, height], or null if invalid
     */
    default int[] getResolution(Camera camera, Settings settings) {
        String resolution = camera.getEffectiveResolution(settings.getDefaultResolution());
        return Settings.parseResolution(resolution);
    }

    /**
     * Gets the platform name.
     * 
     * @return Platform name (e.g., "Linux", "Windows", "macOS")
     */
    String getPlatformName();
    
    /**
     * Captures a snapshot from a single camera using FFmpeg.
     *
     * @param camera The camera to capture from
     * @param settings Application settings
     * @return true if successful
     */
    boolean captureSnapshot(Camera camera, Settings settings);

    /**
     * Generates a filename from the pattern, replacing placeholders.
     * 
     * @param camera The camera
     * @param pattern The filename pattern
     * @return Generated filename
     */
    default String generateFilename(Camera camera, String pattern) {
        String filename = pattern;
        
        // Replace {id} with camera name (sanitized)
        String sanitizedName = camera.getName().replaceAll("[^a-zA-Z0-9-_]", "_");
        filename = filename.replace("{id}", sanitizedName);
        
        // Replace {timestamp} with current timestamp
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = dateFormat.format(new Date());
        filename = filename.replace("{timestamp}", timestamp);
        
        return filename;
    }

    /**
     * Builds a platform-specific FFmpeg command for HLS streaming.
     * The command should stream video from the camera to HLS files.
     *
     * @param camera The camera to stream from
     * @param settings Application settings (for resolution, etc.)
     * @param port Not used for HLS (kept for API compatibility)
     * @param hlsPath The directory path for HLS segment files
     * @return ProcessBuilder configured with FFmpeg HLS streaming command
     */
    ProcessBuilder buildFFmpegHttpStreamCommand(Camera camera, Settings settings, int port, String hlsPath);
}
