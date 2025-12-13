package com.example.simplephototool;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;

import java.util.ArrayList;
import java.util.List;

/**
 * macOS-specific camera strategy using AVFoundation.
 */
public class MacCameraStrategy implements CameraStrategy {
    
    @Override
    public List<CameraDevice> detectDevices() {
        List<CameraDevice> devices = new ArrayList<>();
        
        // On macOS, JavaCV uses AVFoundation with numeric indices
        // Probe for up to 10 cameras
        for (int i = 0; i < 10; i++) {
            devices.add(new CameraDevice(String.valueOf(i), "Camera " + i));
        }
        
        return devices;
    }
    
    @Override
    public FrameGrabber createGrabber(String deviceId) {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(deviceId);
        grabber.setFormat("avfoundation");
        grabber.setOption("framerate", "30");
        grabber.setImageWidth(640);
        grabber.setImageHeight(480);
        return grabber;
    }
    
    @Override
    public String getPlatformName() {
        return "macOS";
    }
}
