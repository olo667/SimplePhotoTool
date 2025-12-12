package com.example.simplephototool;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service to preview camera frames using JavaCV across all platforms.
 * Supports Linux (v4l2), Windows (DirectShow), and macOS (AVFoundation).
 */
public class CameraPreviewService {
    private Thread worker;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private String currentDeviceId;
    private FFmpegFrameGrabber grabber;

    public void startPreview(String deviceId, ImageView target) {
        stopPreview();

        this.currentDeviceId = deviceId;
        running.set(true);
        startJavaCVPreview(deviceId, target);
    }
    
    private void startJavaCVPreview(String deviceId, ImageView target) {
        worker = new Thread(() -> {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                
                // Set platform-specific format
                if (os.contains("linux")) {
                    grabber = new FFmpegFrameGrabber(deviceId);
                    grabber.setFormat("video4linux2");
                    grabber.setOption("framerate", "30");
                } else if (os.contains("windows")) {
                    // DirectShow on Windows - deviceId should be "video=X" format
                    grabber = new FFmpegFrameGrabber(deviceId);
                    grabber.setFormat("dshow");
                    grabber.setOption("framerate", "30");
                } else if (os.contains("mac")) {
                    grabber = new FFmpegFrameGrabber(deviceId);
                    grabber.setFormat("avfoundation");
                    grabber.setOption("framerate", "30");
                } else {
                    grabber = new FFmpegFrameGrabber(deviceId);
                }
                
                grabber.start();

                Java2DFrameConverter converter = new Java2DFrameConverter();
                while (running.get()) {
                    Frame frame = grabber.grabImage();
                    if (frame == null) {
                        Thread.sleep(10);
                        continue;
                    }
                    BufferedImage buffered = converter.getBufferedImage(frame);
                    if (buffered != null) {
                        javafx.scene.image.Image fxImage = SwingFXUtils.toFXImage(buffered, null);
                        Platform.runLater(() -> target.setImage(fxImage));
                    }
                }
            } catch (Exception e) {
                System.err.println("Error in JavaCV preview: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    if (grabber != null) grabber.stop();
                } catch (Exception ignored) {}
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    public void stopPreview() {
        running.set(false);
        if (worker != null) {
            try {
                worker.join(500);
            } catch (InterruptedException ignored) {}
            worker = null;
        }
        if (grabber != null) {
            try {
                grabber.stop();
            } catch (Exception ignored) {}
            grabber = null;
        }
        currentDeviceId = null;
    }
    
    public boolean isRunning() {
        return running.get();
    }
    
    public String getCurrentDeviceId() {
        return currentDeviceId;
    }
}
