package com.example.simplephototool;

/**
 * Factory for creating platform-specific camera strategies.
 */
public class CameraStrategyFactory {
    
    private static CameraStrategy instance;
    
    /**
     * Gets the appropriate camera strategy for the current platform.
     * 
     * @return Platform-specific camera strategy
     */
    public static CameraStrategy getStrategy() {
        if (instance == null) {
            String os = System.getProperty("os.name").toLowerCase();
            
            if (os.contains("linux")) {
                instance = new LinuxCameraStrategy();
            } else if (os.contains("windows")) {
                instance = new WindowsCameraStrategy();
            } else if (os.contains("mac")) {
                instance = new MacCameraStrategy();
            } else {
                // Default fallback
                instance = new LinuxCameraStrategy();
            }
            
            System.out.println("Using camera strategy: " + instance.getPlatformName());
        }
        
        return instance;
    }
}
