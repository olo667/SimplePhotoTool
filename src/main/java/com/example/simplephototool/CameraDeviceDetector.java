package com.example.simplephototool;

import java.util.List;

/**
 * Utility class to detect available camera devices on the system.
 * Uses platform-specific strategies via the Strategy pattern.
 */
public class CameraDeviceDetector {

    /**
     * Detects available camera devices on the system.
     * Works on Windows, Linux, and macOS using platform-specific strategies.
     */
    public static List<CameraDevice> detectDevices() {
        CameraStrategy strategy = CameraStrategyFactory.getStrategy();
        List<CameraDevice> devices = strategy.detectDevices();
        
        // If no devices found, add a default placeholder
        if (devices.isEmpty()) {
            devices.add(new CameraDevice("0", "Default Camera"));
        }
        
        System.out.println("Detected " + devices.size() + " camera(s)");
        return devices;
    }
}
