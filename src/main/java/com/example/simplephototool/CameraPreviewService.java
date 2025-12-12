package com.example.simplephototool;

import javafx.application.Platform;
import javafx.scene.image.ImageView;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.JavaFXFrameConverter;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service to preview camera frames using JavaCV (FFmpegFrameGrabber) and display in a JavaFX ImageView.
 */
public class CameraPreviewService {
    private Thread worker;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public void startPreview(String deviceId, ImageView target) {
        stopPreview();

        running.set(true);
        worker = new Thread(() -> {
            FFmpegFrameGrabber grabber = null;
            try {
                // For Linux device nodes (/dev/video*), use video4linux2 format
                grabber = new FFmpegFrameGrabber(deviceId);
                if (deviceId.startsWith("/dev/")) {
                    grabber.setFormat("video4linux2");
                    grabber.setOption("-framerate", "30");
                }
                grabber.start();

                JavaFXFrameConverter converter = new JavaFXFrameConverter();
                while (running.get()) {
                    Frame frame = grabber.grabImage();
                    if (frame == null) {
                        Thread.sleep(10);
                        continue;
                    }
                    javafx.scene.image.Image fxImage = converter.convert(frame);
                    if (fxImage != null) {
                        Platform.runLater(() -> target.setImage(fxImage));
                    }
                }
            } catch (Exception e) {
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
                worker.join(200);
            } catch (InterruptedException ignored) {}
            worker = null;
        }
    }
}
