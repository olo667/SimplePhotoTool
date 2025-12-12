package com.example.simplephototool;

import com.github.sarxos.webcam.Webcam;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to detect available camera devices on the system.
 * Uses platform-specific detection methods.
 */
public class CameraDeviceDetector {

    /**
     * Detects available camera devices on the system.
     * Works on Windows, Linux, and macOS.
     */
    public static List<CameraDevice> detectDevices() {
        List<CameraDevice> devices = new ArrayList<>();
        
        String os = System.getProperty("os.name").toLowerCase();
        
        // Try webcam-capture first (works best on Windows)
        try {
            List<Webcam> webcams = Webcam.getWebcams();
            
            for (int i = 0; i < webcams.size(); i++) {
                Webcam webcam = webcams.get(i);
                String deviceId = String.valueOf(i);
                String deviceName = webcam.getName();
                
                devices.add(new CameraDevice(deviceId, deviceName));
            }
            
            if (!devices.isEmpty()) {
                System.out.println("Detected " + devices.size() + " cameras using webcam-capture");
                return devices;
            }
        } catch (Exception e) {
            System.out.println("webcam-capture failed, trying platform-specific detection: " + e.getMessage());
        }
        
        // Fallback to platform-specific detection
        if (os.contains("linux")) {
            devices = detectLinuxDevices();
        } else if (os.contains("windows")) {
            // Already tried webcam-capture above
            devices.add(new CameraDevice("0", "Default Camera"));
        } else if (os.contains("mac")) {
            devices.add(new CameraDevice("0", "Default Camera"));
        }
        
        // If still no devices found, add a placeholder
        if (devices.isEmpty()) {
            devices.add(new CameraDevice("0", "Default Camera"));
        }
        
        return devices;
    }
    
    private static List<CameraDevice> detectLinuxDevices() {
        List<CameraDevice> devices = new ArrayList<>();
        
        // Check /dev/video* devices
        File devDir = new File("/dev");
        File[] videoDevices = devDir.listFiles((dir, name) -> name.startsWith("video"));
        
        if (videoDevices != null) {
            for (File device : videoDevices) {
                String devicePath = device.getAbsolutePath();
                String deviceName = device.getName();
                
                devices.add(new CameraDevice(devicePath, "Camera " + deviceName));
            }
        }
        
        System.out.println("Detected " + devices.size() + " Linux video devices");
        return devices;
    }
}
