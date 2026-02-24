package com.example.simplephototool;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Windows-specific camera strategy using FFmpeg DirectShow.
 * All operations use FFmpeg for device detection, preview, and snapshot.
 */
public class WindowsCameraStrategy implements CameraStrategy {
    
    private static final List<String> ffmpegDshow = List.of("ffmpeg",
            "-hide_banner",
            "-f", "dshow");

    @Override
    public List<CameraDevice> detectDevices() {
        // Detect DirectShow video devices
        return new ArrayList<>(detectDirectShowDevices());
    }

    /**
     * Detects cameras using DirectShow via FFmpeg.
     */
    private List<CameraDevice> detectDirectShowDevices() {
        List<CameraDevice> devices = new ArrayList<>();

        try {
            List<String> command = new ArrayList<>(ffmpegDshow);
            command.add("-list_devices");
            command.add("true");
            command.add("-i");
            command.add("dummy");
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            List<String> lines = reader.lines().toList();

            for(int i = 0; i < lines.size()-1; i+=2){
                CameraDevice device = CameraDevice.fromDshowString(lines.get(i), lines.get(i+1));
                if(device != null) devices.add(device);
            }
            
            process.waitFor();
            reader.close();
            
        } catch (Exception e) {
            System.err.println("Failed to enumerate Windows DirectShow devices: " + e.getMessage());
        }
        
        return devices;
    }

    @Override
    public ProcessBuilder buildFFmpegCommand(Camera camera, Settings settings) {
        // Windows: Use DirectShow input format
        String deviceId = camera.getDeviceId();

        // Get resolution from camera/settings
        int[] dimensions = getResolution(camera, settings);
        int width = (dimensions != null) ? dimensions[0] : 640;
        int height = (dimensions != null) ? dimensions[1] : 480;

        // Build command using common prefix
        List<String> command = new ArrayList<>(ffmpegDshow);
        command.add("-video_size");
        command.add(width + "x" + height);
        command.add("-i");
        command.add("video=" + deviceId);
        command.add("-f");
        command.add("rawvideo");
        command.add("-pix_fmt");
        command.add("rgb24");
        command.add("-");

        return new ProcessBuilder(command);
    }

    @Override
    public boolean captureSnapshot(Camera camera, Settings settings) {
        // Use ffmpeg dshow for snapshot capture on Windows
        String outputPath = settings.getSnapshotOutputDirectory() +"\\" + generateFilename(camera, settings.getFilenamePattern());

        String deviceName = camera.getDeviceId();

        List<String> command = new ArrayList<>(ffmpegDshow);
        command.add("-i");
        command.add("video=" + deviceName);
        command.add("-frames:v");
        command.add("1");
        command.add("-y"); // Overwrite output file
        command.add(outputPath);

        ProcessBuilder pb = new ProcessBuilder(command);

        try {
            Process process = pb.start();
            //capture output for debugging
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
            return true;
        } catch (Exception e) {
            System.err.println("Error capturing snapshot from " + camera.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getPlatformName() {
        return "Windows";
    }

    @Override
    public ProcessBuilder buildFFmpegHttpStreamCommand(Camera camera, Settings settings, int port, String hlsPath) {
        String deviceId = camera.getDeviceId();

        // Build FFmpeg command to stream using HLS (HTTP Live Streaming)
        // JavaFX requires HLS with BOTH H.264 video AND AAC audio tracks
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-hide_banner");
        command.add("-y");
        // Video input from camera
        command.add("-f");
        command.add("dshow");
        command.add("-rtbufsize");
        command.add("100M");
        command.add("-i");
        command.add("video=" + deviceId);
        // Silent audio input (required by JavaFX HLS)
        command.add("-f");
        command.add("lavfi");
        command.add("-i");
        command.add("anullsrc=r=44100:cl=mono");
        // Scale to reasonable preview size and set output framerate
        command.add("-vf");
        command.add("scale=640:-2");
        command.add("-r");
        command.add("15");
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
        command.add(hlsPath + "\\segment_%03d.ts");
        command.add(hlsPath + "\\stream.m3u8");

        return new ProcessBuilder(command);
    }
}
