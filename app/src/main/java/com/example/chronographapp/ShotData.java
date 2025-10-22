package com.example.chronographapp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ShotData {
    private int shotNumber;
    private float velocity;
    private float energy;
    private String timestamp;
    private long timeInMillis;

    public ShotData(int shotNumber, float velocity, float energy) {
        this.shotNumber = shotNumber;
        this.velocity = velocity;
        this.energy = energy;
        this.timeInMillis = System.currentTimeMillis();
        this.timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(new Date(timeInMillis));
    }

    public ShotData(int shotNumber, float velocity, float energy, String timestamp) {
        this.shotNumber = shotNumber;
        this.velocity = velocity;
        this.energy = energy;
        this.timestamp = timestamp;
        this.timeInMillis = System.currentTimeMillis();
    }

    public int getShotNumber() { return shotNumber; }
    public float getVelocity() { return velocity; }
    public float getEnergy() { return energy; }
    public String getTimestamp() { return timestamp; }
    public long getTimeInMillis() { return timeInMillis; }

    public void setVelocity(float velocity) { this.velocity = velocity; }
    public void setEnergy(float energy) { this.energy = energy; }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(),
                "Выстрел #%d: %.1f м/с, %.2f Дж (%s)",
                shotNumber, velocity, energy, timestamp);
    }
}