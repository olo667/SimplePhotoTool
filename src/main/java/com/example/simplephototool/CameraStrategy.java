package com.example.simplephototool;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Strategy interface for platform-specific camera operations.
 */
public interface CameraStrategy {
    
    /**
     * Detects available camera devices on the platform.
     * 
     * @return List of detected camera devices
     */
    List<CameraDevice> detectDevices();
    
    /**
     * Creates and configures a frame grabber for the platform.
     *
     * @param deviceId The device identifier
     * @return Configured FrameGrabber ready to start
     */
    FrameGrabber createGrabber(String deviceId);

    /**
     * Creates and configures a frame grabber for the platform with specific resolution.
     *
     * @param deviceId The device identifier
     * @param width The desired width, or 0 to use default
     * @param height The desired height, or 0 to use default
     * @return Configured FrameGrabber ready to start
     */
    default FrameGrabber createGrabber(String deviceId, int width, int height) {
        FrameGrabber grabber = createGrabber(deviceId);
        if (width > 0 && height > 0) {
            grabber.setImageWidth(width);
            grabber.setImageHeight(height);
        }
        return grabber;
    }

    /**
     * Gets the platform name.
     * 
     * @return Platform name (e.g., "Linux", "Windows", "macOS")
     */
    String getPlatformName();
    
    /**
     * Captures a snapshot from a single camera using JavaCV.
     * This default implementation is shared across all platforms.
     * 
     * @param camera The camera to capture from
     * @param settings Application settings
     * @return true if successful
     */
    default boolean captureSnapshot(Camera camera, Settings settings) {
        FrameGrabber grabber = null;
        Java2DFrameConverter converter = null;
        try {
            String deviceId = camera.getDeviceId();
            
            // Get resolution for this camera
            String resolution = camera.getEffectiveResolution(settings.getDefaultResolution());
            int[] dimensions = Settings.parseResolution(resolution);
            
            // Use strategy to create platform-specific grabber with resolution
            if (dimensions != null) {
                grabber = createGrabber(deviceId, dimensions[0], dimensions[1]);
            } else {
                grabber = createGrabber(deviceId);
            }
            grabber.start();

            // Grab a few frames to let camera stabilize
            for (int i = 0; i < 5; i++) {
                grabber.grab();
            }
            
            Frame frame = grabber.grab();
            if (frame == null || frame.image == null) {
                System.err.println("Failed to grab frame from " + camera.getName());
                return false;
            }

            String filename = generateFilename(camera, settings.getFilenamePattern());
            String filepath = settings.getSnapshotOutputDirectory() + File.separator + filename;

            converter = new Java2DFrameConverter();
            BufferedImage image = converter.getBufferedImage(frame);
            
            File outputFile = new File(filepath);
            ImageIO.write(image, "jpg", outputFile);
            
            System.out.println("Snapshot saved: " + filepath);
            return true;
            
        } catch (Exception e) {
            System.err.println("Error capturing snapshot from " + camera.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (converter != null) {
                try {
                    converter.close();
                } catch (Exception ignored) {}
            }
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
    default String generateFilename(Camera camera, String pattern) {
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
