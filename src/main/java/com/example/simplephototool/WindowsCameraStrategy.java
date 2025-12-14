package com.example.simplephototool;

import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacv.VideoInputFrameGrabber;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Windows-specific camera strategy using OpenCV/VideoInput for better compatibility.
 * Uses device indices for reliable camera access.
 */
public class WindowsCameraStrategy implements CameraStrategy {
    
    @Override
    public List<CameraDevice> detectDevices() {
        List<CameraDevice> devices = new ArrayList<>();
        
        // Try VideoInput first (native Windows API, most reliable on Windows 11)
        devices.addAll(detectVideoInputDevices());

        // If VideoInput fails, try DirectShow via FFmpeg as fallback
        if (devices.isEmpty()) {
            devices.addAll(detectDirectShowDevices());
        }

        // If all detection methods fail, probe cameras by index
        if (devices.isEmpty()) {
            devices.addAll(probeDevicesByIndex());
        }

        return devices;
    }

    /**
     * Detects cameras using VideoInput library (native Windows DirectShow access).
     * This is the most reliable method for Windows 10/11.
     */
    private List<CameraDevice> detectVideoInputDevices() {
        List<CameraDevice> devices = new ArrayList<>();

        try {
            // VideoInputFrameGrabber.getDeviceDescriptions() returns device names
            String[] deviceDescriptions = VideoInputFrameGrabber.getDeviceDescriptions();

            if (deviceDescriptions != null) {
                for (int i = 0; i < deviceDescriptions.length; i++) {
                    String deviceName = deviceDescriptions[i];
                    if (deviceName != null && !deviceName.isEmpty()) {
                        devices.add(new CameraDevice(String.valueOf(i), deviceName));
                        System.out.println("Windows VideoInput: Found device [" + i + "]: " + deviceName);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("VideoInput device enumeration failed: " + e.getMessage());
        }
        
        return devices;
    }
    
    /**
     * Detects cameras using DirectShow via FFmpeg.
     * Requires ffmpeg to be installed and in PATH.
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
                    System.out.println("Windows DirectShow: Found video device [" + deviceIndex + "]: " + deviceName);
                    deviceIndex++;
                }
            }
            
            process.waitFor();
            reader.close();
            
        } catch (Exception e) {
            System.err.println("DirectShow device enumeration failed (ffmpeg not in PATH?): " + e.getMessage());
        }

        return devices;
    }

    /**
     * Probes cameras by attempting to open them by index.
     * This is a fallback when other detection methods fail.
     */
    private List<CameraDevice> probeDevicesByIndex() {
        List<CameraDevice> devices = new ArrayList<>();

        System.out.println("Windows: Probing cameras by index...");

        // Try up to 10 device indices
        for (int i = 0; i < 10; i++) {
            try {
                OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(i);
                grabber.start();

                // If we get here, the camera opened successfully
                grabber.stop();
                grabber.close();

                devices.add(new CameraDevice(String.valueOf(i), "Camera " + i));
                System.out.println("Windows: Found camera at index " + i);

            } catch (Exception e) {
                // Camera at this index doesn't exist or can't be opened
                // Stop probing after first failure if we already found cameras
                if (!devices.isEmpty()) {
                    break;
                }
            }
        }
        
        return devices;
    }
    
    @Override
    public FrameGrabber createGrabber(String deviceId) {
        int deviceIndex = Integer.parseInt(deviceId);

        // Try VideoInputFrameGrabber first (more reliable on Windows 11)
        try {
            VideoInputFrameGrabber grabber = new VideoInputFrameGrabber(deviceIndex);
            grabber.setImageWidth(640);
            grabber.setImageHeight(480);
            System.out.println("Windows: Created VideoInput grabber for device index: " + deviceIndex);
            return grabber;
        } catch (Exception e) {
            System.err.println("VideoInput grabber failed, falling back to OpenCV: " + e.getMessage());
        }

        // Fallback to OpenCVFrameGrabber
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(deviceIndex);
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
