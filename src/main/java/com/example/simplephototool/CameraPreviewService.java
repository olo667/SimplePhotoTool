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
 * Uses Strategy pattern for platform-specific configuration.
 */
public class CameraPreviewService {
    private Thread worker;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private String currentDeviceId;
    private FFmpegFrameGrabber grabber;
    private final CameraStrategy strategy;

    public CameraPreviewService() {
        this.strategy = CameraStrategyFactory.getStrategy();
    }

    public void startPreview(String deviceId, ImageView target) {
        stopPreview();

        this.currentDeviceId = deviceId;
        running.set(true);
        
        worker = new Thread(() -> {
            Java2DFrameConverter converter = null;
            try {
                grabber = new FFmpegFrameGrabber(deviceId);
                strategy.configureGrabber(grabber, deviceId);
                grabber.start();

                converter = new Java2DFrameConverter();
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
