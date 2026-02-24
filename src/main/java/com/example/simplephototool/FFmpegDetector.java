package com.example.simplephototool;

import java.util.concurrent.TimeUnit;

/**
 * Utility class to detect if FFmpeg is available on the system.
 * Caches the result after first check for performance.
 */
public class FFmpegDetector {
    private static Boolean available = null;
    private static String version = null;

    /**
     * Checks if FFmpeg is available and executable on the system.
     * This method caches the result after the first check.
     *
     * @return true if FFmpeg is available and working
     */
    public static boolean isAvailable() {
        if (available == null) {
            detectFFmpeg();
        }
        return available;
    }

    /**
     * Gets the FFmpeg version string if available.
     *
     * @return FFmpeg version string, or null if not available
     */
    public static String getVersion() {
        if (available == null) {
            detectFFmpeg();
        }
        return version;
    }

    /**
     * Forces a re-check of FFmpeg availability.
     * Useful if FFmpeg was installed after the application started.
     */
    public static void refresh() {
        available = null;
        version = null;
        detectFFmpeg();
    }

    /**
     * Performs the actual FFmpeg detection.
     */
    private static void detectFFmpeg() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read version info
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            String firstLine = reader.readLine();

            // Wait for process to complete (with timeout)
            boolean finished = process.waitFor(2, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0 && firstLine != null) {
                available = true;
                version = firstLine;
                System.out.println("FFmpeg detected: " + version);
            } else {
                available = false;
                System.out.println("FFmpeg not responding correctly");
            }

            reader.close();
        } catch (Exception e) {
            available = false;
            System.out.println("FFmpeg not found on system PATH: " + e.getMessage());
        }
    }

    /**
     * Gets a user-friendly status message about FFmpeg availability.
     *
     * @return Status message
     */
    public static String getStatusMessage() {
        if (isAvailable()) {
            return "FFmpeg is available: " + version;
        } else {
            return "FFmpeg is not installed or not in system PATH. " +
                   "Install FFmpeg for better performance: https://ffmpeg.org/download.html";
        }
    }
}

