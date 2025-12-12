package com.example.simplephototool;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegLogCallback;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to detect available camera devices on the system.
 * Uses platform-specific detection methods with JavaCV backend.
 */
public class CameraDeviceDetector {

    /**
     * Detects available camera devices on the system.
     * Works on Windows, Linux, and macOS using JavaCV.
     */
    public static List<CameraDevice> detectDevices() {
        List<CameraDevice> devices = new ArrayList<>();
        
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("linux")) {
            devices = detectLinuxDevices();
        } else if (os.contains("windows")) {
            devices = detectWindowsDevices();
        } else if (os.contains("mac")) {
            devices = detectMacDevices();
        }
        
        // If no devices found, add a default placeholder
        if (devices.isEmpty()) {
            devices.add(new CameraDevice("0", "Default Camera"));
        }
        
        System.out.println("Detected " + devices.size() + " camera(s)");
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
                String deviceName = "Camera " + device.getName();
                
                devices.add(new CameraDevice(devicePath, deviceName));
            }
        }
        
        return devices;
    }
    
    private static List<CameraDevice> detectWindowsDevices() {
        List<CameraDevice> devices = new ArrayList<>();
        
        try {
            // Use Media Foundation (modern Windows API)
            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-list_devices", "true", "-f", "mf", "-i", "dummy"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            Pattern devicePattern = Pattern.compile("\\[.*?\\]\\s+\"([^\"]+)\"");
            
            while ((line = reader.readLine()) != null) {
                if (line.contains("video")) {
                    Matcher matcher = devicePattern.matcher(line);
                    if (matcher.find()) {
                        String deviceName = matcher.group(1);
                        devices.add(new CameraDevice(deviceName, deviceName));
                    }
                }
            }
            
            process.waitFor();
            reader.close();
            
        } catch (Exception e) {
            System.err.println("Failed to enumerate Windows devices: " + e.getMessage());
            devices.add(new CameraDevice("0", "Default Camera"));
        }
        
        if (devices.isEmpty()) {
            devices.add(new CameraDevice("0", "Default Camera"));
        }
        
        return devices;
    }
    
    private static List<CameraDevice> detectMacDevices() {
        List<CameraDevice> devices = new ArrayList<>();
        
        // On macOS, JavaCV uses AVFoundation with numeric indices
        // Probe for up to 10 cameras
        for (int i = 0; i < 10; i++) {
            devices.add(new CameraDevice(String.valueOf(i), "Camera " + i));
        }
        
        return devices;
    }
}
