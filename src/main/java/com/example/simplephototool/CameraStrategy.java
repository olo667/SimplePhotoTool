package com.example.simplephototool;

import org.bytedeco.javacv.FrameGrabber;

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
     * Creates and configures a frame grabber for the platform.
     *
     * @param deviceId The device identifier
     * @return Configured FrameGrabber ready to start
     */
    FrameGrabber createGrabber(String deviceId);

    /**
     * Gets the platform name.
     * 
     * @return Platform name (e.g., "Linux", "Windows", "macOS")
     */
    String getPlatformName();
}
