package com.example.simplephototool;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Factory class for detecting and generating FFmpeg hardware encoder arguments.
 * Supports NVIDIA NVENC, Intel QuickSync, and AMD AMF encoders.
 */
public class HardwareEncoderFactory {
    
    /**
     * Enum representing available encoder types.
     */
    public enum EncoderType {
        SOFTWARE("libx264", "Software (x264)"),
        NVIDIA_NVENC("h264_nvenc", "NVIDIA NVENC"),
        INTEL_QSV("h264_qsv", "Intel QuickSync"),
        AMD_AMF("h264_amf", "AMD AMF");
        
        private final String ffmpegName;
        private final String displayName;
        
        EncoderType(String ffmpegName, String displayName) {
            this.ffmpegName = ffmpegName;
            this.displayName = displayName;
        }
        
        public String getFFmpegName() {
            return ffmpegName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
        
        public static EncoderType fromFFmpegName(String name) {
            for (EncoderType type : values()) {
                if (type.ffmpegName.equals(name)) {
                    return type;
                }
            }
            return SOFTWARE;
        }
        
        public static EncoderType fromDisplayName(String name) {
            for (EncoderType type : values()) {
                if (type.displayName.equals(name)) {
                    return type;
                }
            }
            return SOFTWARE;
        }
    }
    
    // Cache detected encoders to avoid repeated detection
    private static List<EncoderType> availableEncoders = null;
    private static boolean detectionComplete = false;
    
    /**
     * Detects all available hardware encoders on the system.
     * Results are cached after first detection.
     *
     * @return List of available encoder types (always includes SOFTWARE)
     */
    public static synchronized List<EncoderType> detectAvailableEncoders() {
        if (detectionComplete) {
            return new ArrayList<>(availableEncoders);
        }
        
        availableEncoders = new ArrayList<>();
        availableEncoders.add(EncoderType.SOFTWARE); // Always available
        
        System.out.println("[HardwareEncoderFactory] Detecting available hardware encoders...");
        
        // Check NVIDIA NVENC
        if (isEncoderAvailable("h264_nvenc")) {
            availableEncoders.add(EncoderType.NVIDIA_NVENC);
            System.out.println("[HardwareEncoderFactory] NVIDIA NVENC detected");
        }
        
        // Check Intel QuickSync
        if (isEncoderAvailable("h264_qsv")) {
            availableEncoders.add(EncoderType.INTEL_QSV);
            System.out.println("[HardwareEncoderFactory] Intel QuickSync detected");
        }
        
        // Check AMD AMF
        if (isEncoderAvailable("h264_amf")) {
            availableEncoders.add(EncoderType.AMD_AMF);
            System.out.println("[HardwareEncoderFactory] AMD AMF detected");
        }
        
        detectionComplete = true;
        System.out.println("[HardwareEncoderFactory] Detection complete. Available: " + availableEncoders);
        
        return new ArrayList<>(availableEncoders);
    }
    
    /**
     * Checks if a specific encoder is available by testing with FFmpeg.
     *
     * @param encoderName The FFmpeg encoder name
     * @return true if encoder is available and working
     */
    private static boolean isEncoderAvailable(String encoderName) {
        try {
            // Use FFmpeg to test if encoder is available
            // Use 256x256 because NVENC requires minimum ~145x145
            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-hide_banner", "-loglevel", "error",
                "-f", "lavfi", "-i", "nullsrc=s=256x256:d=0.1",
                "-c:v", encoderName,
                "-f", "null", "-"
            );
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // Read output to prevent blocking
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {
                    // Consume output
                }
            }
            
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return false;
            }
            
            return process.exitValue() == 0;
            
        } catch (Exception e) {
            System.err.println("[HardwareEncoderFactory] Error testing encoder " + encoderName + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets FFmpeg encoder arguments for the specified encoder type.
     *
     * @param encoderType The encoder type to use
     * @return List of FFmpeg arguments for video encoding
     */
    public static List<String> getEncoderArguments(EncoderType encoderType) {
        List<String> args = new ArrayList<>();
        
        switch (encoderType) {
            case NVIDIA_NVENC:
                args.add("-c:v");
                args.add("h264_nvenc");
                args.add("-preset");
                args.add("p1");  // Fastest preset
                args.add("-tune");
                args.add("ll");  // Low latency
                args.add("-rc");
                args.add("cbr"); // Constant bitrate for streaming
                args.add("-b:v");
                args.add("2M");
                args.add("-profile:v");
                args.add("baseline");
                args.add("-level");
                args.add("3.0");
                break;
                
            case INTEL_QSV:
                args.add("-c:v");
                args.add("h264_qsv");
                args.add("-preset");
                args.add("veryfast");
                args.add("-look_ahead");
                args.add("0");  // Disable lookahead for lower latency
                args.add("-b:v");
                args.add("2M");
                args.add("-profile:v");
                args.add("baseline");
                args.add("-level");
                args.add("30");
                break;
                
            case AMD_AMF:
                args.add("-c:v");
                args.add("h264_amf");
                args.add("-usage");
                args.add("ultralowlatency");
                args.add("-quality");
                args.add("speed");
                args.add("-rc");
                args.add("cbr");
                args.add("-b:v");
                args.add("2M");
                args.add("-profile:v");
                args.add("baseline");
                args.add("-level");
                args.add("30");
                break;
                
            case SOFTWARE:
            default:
                args.add("-c:v");
                args.add("libx264");
                args.add("-preset");
                args.add("ultrafast");
                args.add("-tune");
                args.add("zerolatency");
                args.add("-profile:v");
                args.add("baseline");
                args.add("-level");
                args.add("3.0");
                break;
        }
        
        // Common arguments for all encoders
        args.add("-pix_fmt");
        args.add("yuv420p");
        
        return args;
    }
    
    /**
     * Gets the best available hardware encoder, or falls back to software.
     *
     * @return The best available encoder type
     */
    public static EncoderType getBestAvailableEncoder() {
        List<EncoderType> available = detectAvailableEncoders();
        
        // Preference order: NVENC > QSV > AMF > Software
        if (available.contains(EncoderType.NVIDIA_NVENC)) {
            return EncoderType.NVIDIA_NVENC;
        }
        if (available.contains(EncoderType.INTEL_QSV)) {
            return EncoderType.INTEL_QSV;
        }
        if (available.contains(EncoderType.AMD_AMF)) {
            return EncoderType.AMD_AMF;
        }
        
        return EncoderType.SOFTWARE;
    }
    
    /**
     * Resets the detection cache, forcing re-detection on next call.
     */
    public static synchronized void resetDetectionCache() {
        detectionComplete = false;
        availableEncoders = null;
    }
    
    /**
     * Gets all encoder display names for UI dropdown.
     *
     * @return Array of display names for available encoders
     */
    public static String[] getAvailableEncoderDisplayNames() {
        List<EncoderType> available = detectAvailableEncoders();
        return available.stream()
            .map(EncoderType::getDisplayName)
            .toArray(String[]::new);
    }
}
