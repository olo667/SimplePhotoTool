package com.example.simplephototool;

import org.bytedeco.javacv.FFmpegFrameGrabber;

import java.util.List;

/**
 * Strategy interface for platform-specific camera operations.
 */
public interface CameraStrategy {
    
    /**
     * Detects available camera devices on the platform.
     * 
     * @return List of detected camera devices
     */
    List<CameraDevice> detectDevices();
    
    /**
     * Configures a frame grabber for the platform.
     * 
     * @param grabber The frame grabber to configure
     * @param deviceId The device identifier
     */
    void configureGrabber(FFmpegFrameGrabber grabber, String deviceId);
    
    /**
     * Gets the platform name.
     * 
     * @return Platform name (e.g., "Linux", "Windows", "macOS")
     */
    String getPlatformName();
}
