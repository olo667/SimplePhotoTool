package com.example.simplephototool;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model class representing a camera with its properties.
 */
public class Camera {
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty deviceId = new SimpleStringProperty();
    private final BooleanProperty active = new SimpleBooleanProperty(true);
    private final StringProperty resolution = new SimpleStringProperty();

    public Camera(String name) {
        this.name.set(name);
        this.deviceId.set("default");
        this.resolution.set(null); // Use default resolution from settings
    }

    public Camera(String name, String deviceId) {
        this.name.set(name);
        this.deviceId.set(deviceId);
        this.resolution.set(null); // Use default resolution from settings
    }

    public Camera(String name, String deviceId, boolean active) {
        this.name.set(name);
        this.deviceId.set(deviceId);
        this.active.set(active);
        this.resolution.set(null); // Use default resolution from settings
    }

    public Camera(String name, String deviceId, boolean active, String resolution) {
        this.name.set(name);
        this.deviceId.set(deviceId);
        this.active.set(active);
        this.resolution.set(resolution);
    }

    // Name property
    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    // DeviceId property
    public String getDeviceId() {
        return deviceId.get();
    }

    public void setDeviceId(String deviceId) {
        this.deviceId.set(deviceId);
    }

    public StringProperty deviceIdProperty() {
        return deviceId;
    }

    // Active property
    public boolean isActive() {
        return active.get();
    }

    public void setActive(boolean active) {
        this.active.set(active);
    }

    public BooleanProperty activeProperty() {
        return active;
    }

    // Resolution property
    public String getResolution() {
        return resolution.get();
    }

    public void setResolution(String resolution) {
        this.resolution.set(resolution);
    }

    public StringProperty resolutionProperty() {
        return resolution;
    }

    /**
     * Gets the effective resolution for this camera.
     * Returns the camera-specific resolution if set, otherwise returns the default resolution.
     * 
     * @param defaultResolution The default resolution from settings
     * @return The effective resolution to use
     */
    public String getEffectiveResolution(String defaultResolution) {
        String res = getResolution();
        return (res != null && !res.isEmpty()) ? res : defaultResolution;
    }

    @Override
    public String toString() {
        return getName();
    }
}
