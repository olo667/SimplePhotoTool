package com.example.simplephototool;

import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Windows-specific camera strategy using OpenCV for better compatibility.
 * Uses device indices for reliable camera access.
 */
public class WindowsCameraStrategy implements CameraStrategy {
    
    @Override
    public List<CameraDevice> detectDevices() {
        List<CameraDevice> devices = new ArrayList<>();
        
        // Detect DirectShow video devices
        devices.addAll(detectDirectShowDevices());

        // If detection fails, add default camera by index
        if (devices.isEmpty()) {
            devices.add(new CameraDevice("0", "Default Camera"));
        }
        
        return devices;
    }
    
    /**
     * Detects cameras using DirectShow via FFmpeg.
     * Stores device index as ID for reliable JavaCV access.
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
            // Pattern to match device name in quotes followed by (video)
            // Example: [dshow @ 00000260f92f62c0] "UVC Camera" (video)
            Pattern devicePattern = Pattern.compile("\\[dshow.*?]\\s+\"([^\"]+)\"\\s+\\(video\\)");

            int deviceIndex = 0;
            while ((line = reader.readLine()) != null) {
                // Skip alternative name lines
                if (line.contains("Alternative name")) {
                    continue;
                }
                
                Matcher deviceMatcher = devicePattern.matcher(line);
                if (deviceMatcher.find()) {
                    String deviceName = deviceMatcher.group(1);
                    // Use device index as ID for reliable access via OpenCV/JavaCV
                    devices.add(new CameraDevice(String.valueOf(deviceIndex), deviceName));
                    System.out.println("Windows: Found video device [" + deviceIndex + "]: " + deviceName);
                    deviceIndex++;
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
    public FrameGrabber createGrabber(String deviceId) {
        // Use OpenCVFrameGrabber with device index for Windows
        // This is more reliable than FFmpegFrameGrabber with DirectShow
        int deviceIndex = Integer.parseInt(deviceId);
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(deviceIndex);

        // Set resolution
        grabber.setImageWidth(640);
        grabber.setImageHeight(480);
        
        System.out.println("Windows: Created OpenCV grabber for device index: " + deviceIndex);
        return grabber;
    }
    
    @Override
    public String getPlatformName() {
        return "Windows";
    }
}
