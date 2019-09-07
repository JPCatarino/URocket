package com.jaagora.urocket.lib;

public class Telemetry {
    private Double timestamp;
    private Double acceleration;
    private Double altitude;

    public Telemetry() {
    }

    public Telemetry(Double timestamp, Double acceleration, Double altitude) {
        this.timestamp = timestamp;
        this.acceleration = acceleration;
        this.altitude = altitude;
    }

    public Double getTimestamp() {
        return timestamp;
    }

    public Double getAcceleration() {
        return acceleration;
    }

    public Double getAltitude() {
        return altitude;
    }
}
