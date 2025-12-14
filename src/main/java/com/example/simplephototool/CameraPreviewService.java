package com.example.simplephototool;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service to preview camera frames using JavaCV across all platforms.
 * Uses Strategy pattern for platform-specific configuration.
 */
public class CameraPreviewService {
    private Thread worker;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private String currentDeviceId;
    private FrameGrabber grabber;
    private final CameraStrategy strategy;

    public CameraPreviewService() {
        this.strategy = CameraStrategyFactory.getStrategy();
    }

    /**
     * Starts preview for a device with default resolution.
     * 
     * @param deviceId The device ID to preview
     * @param target The ImageView to display preview
     */
    public void startPreview(String deviceId, ImageView target) {
        startPreview(deviceId, target, 0, 0);
    }

    /**
     * Starts preview for a device with specified resolution.
     * 
     * @param deviceId The device ID to preview
     * @param target The ImageView to display preview
     * @param width The desired width (0 for default)
     * @param height The desired height (0 for default)
     */
    public void startPreview(String deviceId, ImageView target, int width, int height) {
        stopPreview();

        this.currentDeviceId = deviceId;
        running.set(true);
        
        worker = new Thread(() -> {
            Java2DFrameConverter converter = null;
            try {
                // Use strategy to create platform-specific grabber with resolution
                if (width > 0 && height > 0) {
                    grabber = strategy.createGrabber(deviceId, width, height);
                } else {
                    grabber = strategy.createGrabber(deviceId);
                }
                grabber.start();

                converter = new Java2DFrameConverter();
                while (running.get()) {
                    Frame frame = grabber.grab();
                    if (frame == null || frame.image == null) {
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
                System.err.println("Error in camera preview: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (converter != null) {
                    try {
                        converter.close();
                    } catch (Exception ignored) {}
                }
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
