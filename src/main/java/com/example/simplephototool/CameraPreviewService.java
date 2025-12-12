package com.example.simplephototool;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service to preview camera frames using platform-appropriate backend.
 * Uses JavaCV for Linux (/dev/video*) and webcam-capture for Windows.
 */
public class CameraPreviewService {
    private Thread worker;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private String currentDeviceId;
    private Webcam webcam;
    private FFmpegFrameGrabber grabber;

    public void startPreview(String deviceId, ImageView target) {
        stopPreview();

        this.currentDeviceId = deviceId;
        running.set(true);
        
        // Determine which backend to use based on deviceId
        if (deviceId.startsWith("/dev/")) {
            startJavaCVPreview(deviceId, target);
        } else {
            startWebcamPreview(deviceId, target);
        }
    }
    
    private void startWebcamPreview(String deviceId, ImageView target) {
        worker = new Thread(() -> {
            try {
                int index = Integer.parseInt(deviceId);
                List<Webcam> webcams = Webcam.getWebcams();
                
                if (index >= 0 && index < webcams.size()) {
                    webcam = webcams.get(index);
                    
                    if (!webcam.open()) {
                        System.err.println("Failed to open webcam: " + webcam.getName());
                        return;
                    }

                    while (running.get()) {
                        BufferedImage image = webcam.getImage();
                        if (image != null) {
                            javafx.scene.image.Image fxImage = SwingFXUtils.toFXImage(image, null);
                            Platform.runLater(() -> target.setImage(fxImage));
                        }
                        Thread.sleep(33); // ~30 FPS
                    }
                }
            } catch (Exception e) {
                System.err.println("Error in webcam preview: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (webcam != null && webcam.isOpen()) {
                    webcam.close();
                }
            }
        });
        worker.setDaemon(true);
        worker.start();
    }
    
    private void startJavaCVPreview(String deviceId, ImageView target) {
        worker = new Thread(() -> {
            try {
                grabber = new FFmpegFrameGrabber(deviceId);
                grabber.setFormat("video4linux2");
                grabber.setOption("-framerate", "30");
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
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
            webcam = null;
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
