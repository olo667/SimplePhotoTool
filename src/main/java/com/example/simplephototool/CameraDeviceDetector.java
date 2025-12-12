package com.example.simplephototool;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to detect available camera devices on the system.
 */
public class CameraDeviceDetector {

    /**
     * Detects available camera devices on the system.
     * Currently supports Linux (via /dev/video*) and attempts to get device names.
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
        
        // If no devices found, add a placeholder
        if (devices.isEmpty()) {
            devices.add(new CameraDevice("default", "Default Camera"));
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
                String deviceName = getLinuxDeviceName(devicePath);
                if (deviceName == null) {
                    deviceName = device.getName();
                }
                devices.add(new CameraDevice(devicePath, deviceName + " (" + device.getName() + ")"));
            }
        }
        
        return devices;
    }

    private static String getLinuxDeviceName(String devicePath) {
        try {
            // Try to get device name using v4l2-ctl
            ProcessBuilder pb = new ProcessBuilder("v4l2-ctl", "--device=" + devicePath, "--info");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Card type")) {
                        int colonIndex = line.indexOf(':');
                        if (colonIndex != -1) {
                            return line.substring(colonIndex + 1).trim();
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // v4l2-ctl not available or error occurred
        }
        return null;
    }

    private static List<CameraDevice> detectWindowsDevices() {
        List<CameraDevice> devices = new ArrayList<>();
        
        // On Windows, we can try to use PowerShell to list devices
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command",
                "Get-PnpDevice -Class Camera -Status OK | Select-Object -ExpandProperty FriendlyName");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int index = 0;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        devices.add(new CameraDevice(String.valueOf(index), line));
                        index++;
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // PowerShell not available or error occurred
        }
        
        return devices;
    }

    private static List<CameraDevice> detectMacDevices() {
        List<CameraDevice> devices = new ArrayList<>();
        
        // On macOS, we can try to use system_profiler
        try {
            ProcessBuilder pb = new ProcessBuilder("system_profiler", "SPCameraDataType");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int index = 0;
                while ((line = reader.readLine()) != null) {
                    // Look for camera names (they appear after indentation)
                    if (line.matches("^\\s{4}\\S.*:$")) {
                        String name = line.trim().replace(":", "");
                        devices.add(new CameraDevice(String.valueOf(index), name));
                        index++;
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // system_profiler not available or error occurred
        }
        
        return devices;
    }
}
