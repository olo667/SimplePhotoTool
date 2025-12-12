package com.example.simplephototool;

import org.bytedeco.javacv.FFmpegFrameGrabber;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Linux-specific camera strategy using video4linux2.
 */
public class LinuxCameraStrategy implements CameraStrategy {
    
    @Override
    public List<CameraDevice> detectDevices() {
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
    
    @Override
    public void configureGrabber(FFmpegFrameGrabber grabber, String deviceId) {
        grabber.setFormat("video4linux2");
        grabber.setOption("framerate", "30");
    }
    
    @Override
    public String getPlatformName() {
        return "Linux";
    }
}
