package com.example.simplephototool;

import com.github.sarxos.webcam.Webcam;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for capturing snapshots from cameras using platform-appropriate backend.
 */
public class SnapshotService {
    
    /**
     * Captures snapshots from all active cameras.
     * 
     * @param cameras List of all cameras
     * @param settings Application settings containing output directory and filename pattern
     * @return Number of successful snapshots
     */
    public static int captureSnapshots(List<Camera> cameras, Settings settings) {
        // Filter only active cameras
        List<Camera> activeCameras = cameras.stream()
                .filter(Camera::isActive)
                .collect(Collectors.toList());
        
        if (activeCameras.isEmpty()) {
            System.out.println("No active cameras to capture from.");
            return 0;
        }

        // Ensure output directory exists
        Path outputPath = Paths.get(settings.getSnapshotOutputDirectory());
        try {
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }
        } catch (IOException e) {
            System.err.println("Failed to create output directory: " + e.getMessage());
            return 0;
        }

        int successCount = 0;
        for (Camera camera : activeCameras) {
            try {
                if (captureSnapshot(camera, settings)) {
                    successCount++;
                }
            } catch (Exception e) {
                System.err.println("Error capturing from camera " + camera.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("Captured " + successCount + " of " + activeCameras.size() + " snapshots.");
        return successCount;
    }
    
    /**
     * Captures a snapshot from a single camera.
     * 
     * @param camera The camera to capture from
     * @param settings Application settings
     * @return true if successful
     */
    private static boolean captureSnapshot(Camera camera, Settings settings) {
        String deviceId = camera.getDeviceId();
        
        // Use appropriate backend based on device ID
        if (deviceId.startsWith("/dev/")) {
            return captureSnapshotJavaCV(camera, settings);
        } else {
            return captureSnapshotWebcam(camera, settings);
        }
    }
    
    private static boolean captureSnapshotWebcam(Camera camera, Settings settings) {
        Webcam webcam = null;
        try {
            int index = Integer.parseInt(camera.getDeviceId());
            List<Webcam> webcams = Webcam.getWebcams();
            
            if (index < 0 || index >= webcams.size()) {
                System.err.println("Invalid camera index: " + index);
                return false;
            }
            
            webcam = webcams.get(index);
            
            if (!webcam.open()) {
                System.err.println("Failed to open webcam: " + webcam.getName());
                return false;
            }
            
            BufferedImage image = webcam.getImage();
            if (image == null) {
                System.err.println("Failed to capture image from " + camera.getName());
                return false;
            }

            String filename = generateFilename(camera, settings.getFilenamePattern());
            String filepath = settings.getSnapshotOutputDirectory() + File.separator + filename;

            File outputFile = new File(filepath);
            ImageIO.write(image, "jpg", outputFile);
            
            System.out.println("Snapshot saved: " + filepath);
            return true;
            
        } catch (Exception e) {
            System.err.println("Error capturing webcam snapshot from " + camera.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (webcam != null && webcam.isOpen()) {
                webcam.close();
            }
        }
    }
    
    private static boolean captureSnapshotJavaCV(Camera camera, Settings settings) {
        FFmpegFrameGrabber grabber = null;
        try {
            grabber = new FFmpegFrameGrabber(camera.getDeviceId());
            grabber.setFormat("video4linux2");
            grabber.start();

            // Grab a few frames to let camera stabilize
            for (int i = 0; i < 5; i++) {
                grabber.grabImage();
            }
            
            Frame frame = grabber.grabImage();
            if (frame == null) {
                System.err.println("Failed to grab frame from " + camera.getName());
                return false;
            }

            String filename = generateFilename(camera, settings.getFilenamePattern());
            String filepath = settings.getSnapshotOutputDirectory() + File.separator + filename;

            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage image = converter.getBufferedImage(frame);
            
            File outputFile = new File(filepath);
            ImageIO.write(image, "jpg", outputFile);
            
            System.out.println("Snapshot saved: " + filepath);
            return true;
            
        } catch (Exception e) {
            System.err.println("Error capturing JavaCV snapshot from " + camera.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (grabber != null) {
                try {
                    grabber.stop();
                } catch (Exception ignored) {}
            }
        }
    }
    
    /**
     * Generates a filename from the pattern, replacing placeholders.
     * 
     * @param camera The camera
     * @param pattern The filename pattern
     * @return Generated filename
     */
    private static String generateFilename(Camera camera, String pattern) {
        String filename = pattern;
        
        // Replace {id} with camera name (sanitized)
        String sanitizedName = camera.getName().replaceAll("[^a-zA-Z0-9-_]", "_");
        filename = filename.replace("{id}", sanitizedName);
        
        // Replace {timestamp} with current timestamp
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = dateFormat.format(new Date());
        filename = filename.replace("{timestamp}", timestamp);
        
        return filename;
    }
}
