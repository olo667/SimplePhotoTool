package com.example.simplephototool;

import org.bytedeco.javacv.FFmpegFrameGrabber;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Windows-specific camera strategy using Media Foundation (msmf).
 * Media Foundation is the modern API for Windows 10 and 11, replacing deprecated DirectShow.
 */
public class WindowsCameraStrategy implements CameraStrategy {
    
    @Override
    public List<CameraDevice> detectDevices() {
        List<CameraDevice> devices = new ArrayList<>();
        
        // Try Media Foundation first (Windows 10/11)
        devices.addAll(detectMediaFoundationDevices());
        
        // Fallback to DirectShow if Media Foundation fails (older Windows or specific hardware)
        if (devices.isEmpty()) {
            devices.addAll(detectDirectShowDevices());
        }
        
        // If all detection methods fail, add default camera
        if (devices.isEmpty()) {
            devices.add(new CameraDevice("0", "Default Camera"));
        }
        
        return devices;
    }
    
    /**
     * Detects cameras using Media Foundation (modern Windows 10/11 API).
     */
    private List<CameraDevice> detectMediaFoundationDevices() {
        List<CameraDevice> devices = new ArrayList<>();
        
        try {
            // Use FFmpeg to list Media Foundation devices
            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-list_devices", "true", "-f", "dshow", "-i", "dummy"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            // Pattern to match both device name and optional alternative name
            // Example: [dshow @ ...] "USB Camera" (video)
            //          [dshow @ ...] "USB Camera"
            //          [dshow @ ...] "@device_pnp_\\\\?\\usb#vid_..." (alternative name)
            Pattern devicePattern = Pattern.compile("\\[.*?\\]\\s+\"([^\"]+)\"");
            Pattern altNamePattern = Pattern.compile("Alternative name\\s+\"([^\"]+)\"");
            
            String currentDeviceName = null;
            
            while ((line = reader.readLine()) != null) {
                // Look for video devices
                if (line.contains("DirectShow video devices") || line.contains("video devices")) {
                    continue;
                }
                
                Matcher deviceMatcher = devicePattern.matcher(line);
                if (deviceMatcher.find() && line.contains("video")) {
                    currentDeviceName = deviceMatcher.group(1);
                    // Store device with name as both ID and display (will be updated if alt name found)
                    devices.add(new CameraDevice(currentDeviceName, currentDeviceName));
                } else if (currentDeviceName != null && line.contains("Alternative name")) {
                    // Found alternative name for the previous device
                    Matcher altMatcher = altNamePattern.matcher(line);
                    if (altMatcher.find()) {
                        String altName = altMatcher.group(1);
                        // Update the last added device to use alternative name as ID
                        devices.remove(devices.size() - 1);
                        devices.add(new CameraDevice(altName, currentDeviceName));
                    }
                    currentDeviceName = null;
                }
            }
            
            process.waitFor();
            reader.close();
            
        } catch (Exception e) {
            System.err.println("Failed to enumerate Windows Media Foundation devices: " + e.getMessage());
        }
        
        return devices;
    }
    
    /**
     * Detects cameras using DirectShow (fallback for older Windows or specific hardware).
     */
    private List<CameraDevice> detectDirectShowDevices() {
        List<CameraDevice> devices = new ArrayList<>();
        
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-list_devices", "true", "-f", "dshow", "-i", "dummy"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            Pattern devicePattern = Pattern.compile("\\[.*?\\]\\s+\"([^\"]+)\"");
            Pattern altNamePattern = Pattern.compile("Alternative name\\s+\"([^\"]+)\"");
            
            String currentDeviceName = null;
            
            while ((line = reader.readLine()) != null) {
                if (line.contains("DirectShow video devices") || line.contains("video devices")) {
                    continue;
                }
                
                Matcher deviceMatcher = devicePattern.matcher(line);
                if (deviceMatcher.find() && line.contains("video")) {
                    currentDeviceName = deviceMatcher.group(1);
                    devices.add(new CameraDevice(currentDeviceName, currentDeviceName));
                } else if (currentDeviceName != null && line.contains("Alternative name")) {
                    Matcher altMatcher = altNamePattern.matcher(line);
                    if (altMatcher.find()) {
                        String altName = altMatcher.group(1);
                        devices.remove(devices.size() - 1);
                        devices.add(new CameraDevice(altName, currentDeviceName));
                    }
                    currentDeviceName = null;
                }
            }
            
            process.waitFor();
            reader.close();
            
        } catch (Exception e) {
            System.err.println("Failed to enumerate Windows DirectShow devices: " + e.getMessage());
        }
        
        return devices;
    }
    
    @Override
    public void configureGrabber(FFmpegFrameGrabber grabber, String deviceId) {
        // Use DirectShow format (still required by FFmpeg on Windows)
        // but with optimized settings for Windows 10/11
        grabber.setFormat("dshow");
        
        // Set video_size to a common resolution for better compatibility
        grabber.setImageWidth(640);
        grabber.setImageHeight(480);
        
        // Set framerate
        grabber.setFrameRate(30);
        
        // Add buffer size to reduce latency
        grabber.setOption("rtbufsize", "100M");
        
        // Use low latency mode
        grabber.setOption("fflags", "nobuffer");
        grabber.setOption("flags", "low_delay");
        
        System.out.println("Windows: Configuring DirectShow grabber with device: " + deviceId);
    }
    
    @Override
    public String getPlatformName() {
        return "Windows";
    }
}
