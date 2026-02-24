package com.example.simplephototool;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * macOS-specific camera strategy using FFmpeg AVFoundation.
 * All operations use FFmpeg for device detection, preview, and snapshot.
 */
public class MacCameraStrategy implements CameraStrategy {
    
    private static final List<String> ffmpegAvfoundation = List.of("ffmpeg",
            "-hide_banner",
            "-f", "avfoundation");

    @Override
    public List<CameraDevice> detectDevices() {
        List<CameraDevice> devices = new ArrayList<>();
        
        // Use FFmpeg to list AVFoundation devices
        try {
            List<String> command = new ArrayList<>(ffmpegAvfoundation);
            command.add("-list_devices");
            command.add("true");
            command.add("-i");
            command.add("");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            boolean inVideoSection = false;
            int deviceIndex = 0;

            while ((line = reader.readLine()) != null) {
                if (line.contains("AVFoundation video devices:")) {
                    inVideoSection = true;
                    continue;
                }
                if (line.contains("AVFoundation audio devices:")) {
                    inVideoSection = false;
                    break;
                }
                if (inVideoSection && line.contains("]")) {
                    // Extract device name from line like: "[0] FaceTime HD Camera"
                    int startIdx = line.indexOf("]");
                    if (startIdx > 0) {
                        String deviceName = line.substring(startIdx + 1).trim();
                        devices.add(new CameraDevice(String.valueOf(deviceIndex), deviceName));
                        deviceIndex++;
                    }
                }
            }

            process.waitFor();
            reader.close();

        } catch (Exception e) {
            System.err.println("Failed to enumerate macOS AVFoundation devices: " + e.getMessage());
        }

        // Fallback: if detection failed, probe numeric indices
        if (devices.isEmpty()) {
            for (int i = 0; i < 5; i++) {
                devices.add(new CameraDevice(String.valueOf(i), "Camera " + i));
            }
        }
        
        return devices;
    }
    
    @Override
    public ProcessBuilder buildFFmpegCommand(Camera camera, Settings settings) {
        // macOS: Use AVFoundation input format
        String deviceId = camera.getDeviceId();

        // Get resolution from camera/settings
        int[] dimensions = getResolution(camera, settings);
        int width = (dimensions != null) ? dimensions[0] : 640;
        int height = (dimensions != null) ? dimensions[1] : 480;

        // Build command using common prefix
        List<String> command = new ArrayList<>(ffmpegAvfoundation);
        command.add("-video_size");
        command.add(width + "x" + height);
        command.add("-i");
        command.add(deviceId);
        command.add("-f");
        command.add("rawvideo");
        command.add("-pix_fmt");
        command.add("rgb24");
        command.add("-");

        return new ProcessBuilder(command);
    }

    @Override
    public boolean captureSnapshot(Camera camera, Settings settings) {
        // Use ffmpeg avfoundation for snapshot capture on macOS
        String outputPath = settings.getSnapshotOutputDirectory() + "/" + generateFilename(camera, settings.getFilenamePattern());
        String deviceId = camera.getDeviceId();

        List<String> command = new ArrayList<>(ffmpegAvfoundation);
        command.add("-i");
        command.add(deviceId);
        command.add("-frames:v");
        command.add("1");
        command.add("-y"); // Overwrite output file
        command.add(outputPath);

        ProcessBuilder pb = new ProcessBuilder(command);

        try {
            Process process = pb.start();
            // Capture output for debugging
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            while (settings.getVerboseOutput() && (line = reader.readLine()) != null) {
                System.out.println(line);
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("FFmpeg failed to capture snapshot from " + camera.getName() + " with exit code " + exitCode);
                return false;
            }
            System.out.println("Snapshot saved: " + outputPath);
            return true;
        } catch (Exception e) {
            System.err.println("Error capturing snapshot from " + camera.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getPlatformName() {
        return "macOS";
    }

    @Override
    public ProcessBuilder buildFFmpegHttpStreamCommand(Camera camera, Settings settings, int port, String hlsPath) {
        String deviceId = camera.getDeviceId();

        // Get resolution from camera/settings
        int[] dimensions = getResolution(camera, settings);
        int width = (dimensions != null) ? dimensions[0] : 640;
        int height = (dimensions != null) ? dimensions[1] : 480;

        // Build FFmpeg command to stream using HLS (HTTP Live Streaming)
        // JavaFX requires HLS with BOTH H.264 video AND AAC audio tracks
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-hide_banner");
        command.add("-y");
        // Video input from camera
        command.add("-f");
        command.add("avfoundation");
        command.add("-video_size");
        command.add(width + "x" + height);
        command.add("-framerate");
        command.add("15");
        command.add("-i");
        command.add(deviceId);
        // Silent audio input (required by JavaFX HLS)
        command.add("-f");
        command.add("lavfi");
        command.add("-i");
        command.add("anullsrc=r=44100:cl=mono");
        // Video encoding - H.264 baseline profile
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add("ultrafast");
        command.add("-tune");
        command.add("zerolatency");
        command.add("-profile:v");
        command.add("baseline");
        command.add("-level");
        command.add("3.0");
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add("-g");
        command.add("15");
        // Audio encoding - AAC (required by JavaFX HLS)
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add("64k");
        // Use shortest input to stop when video ends
        command.add("-shortest");
        // HLS output settings
        command.add("-f");
        command.add("hls");
        command.add("-hls_time");
        command.add("1");
        command.add("-hls_list_size");
        command.add("3");
        command.add("-hls_flags");
        command.add("delete_segments+append_list");
        command.add("-hls_segment_filename");
        command.add(hlsPath + "/segment_%03d.ts");
        command.add(hlsPath + "/stream.m3u8");

        return new ProcessBuilder(command);
    }
}
