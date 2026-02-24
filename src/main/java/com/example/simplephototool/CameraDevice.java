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

    public static CameraDevice fromDshowString(String line1, String line2) {
    //        "OBS-Camera" (video)
    //                [dshow @ 0000024b14675f40]   Alternative name "@device_sw_{860BB310-5D01-11D0-BD3B-00A0C911CE86}\{27B05C2D-93DC-474A-A5DA-9BBA34CB2A9C}"
        String name = line1.substring(line1.indexOf('"')+1, line1.lastIndexOf('"'));
        String type = line1.substring(line1.lastIndexOf('(') + 1, line1.lastIndexOf(')'));
        if (!"video".equalsIgnoreCase(type.trim())) {
            return null;
        }
        return new CameraDevice(name, name);
    }
}
