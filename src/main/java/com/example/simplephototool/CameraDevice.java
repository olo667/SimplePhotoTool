package com.example.simplephototool;

/**
 * Represents an available camera device on the system.
 */
public class CameraDevice {
    private final String deviceId;
    private final String displayName;

    public CameraDevice(String deviceId, String displayName) {
        this.deviceId = deviceId;
        this.displayName = displayName;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
