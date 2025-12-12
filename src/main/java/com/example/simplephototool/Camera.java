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

    public Camera(String name) {
        this.name.set(name);
        this.deviceId.set("default");
    }

    public Camera(String name, String deviceId) {
        this.name.set(name);
        this.deviceId.set(deviceId);
    }

    public Camera(String name, String deviceId, boolean active) {
        this.name.set(name);
        this.deviceId.set(deviceId);
        this.active.set(active);
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

    @Override
    public String toString() {
        return getName();
    }
}
