package com.weatherapp.app.model;

public class User {
    private String uid;
    private String name;
    private String email;
    private String tempUnit; // "C" or "F"
    private String windUnit; // "km/h" or "m/s"
    private String defaultCity; // e.g., "Paris"
    private boolean aqiVisible;

    public User() {
        // Required empty constructor for Firestore
    }

    public User(String uid, String name, String email) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.tempUnit = "C"; // Default
        this.windUnit = "km/h"; // Default
        this.defaultCity = "";
        this.aqiVisible = true;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTempUnit() {
        return tempUnit;
    }

    public void setTempUnit(String tempUnit) {
        this.tempUnit = tempUnit;
    }

    public String getWindUnit() {
        return windUnit;
    }

    public void setWindUnit(String windUnit) {
        this.windUnit = windUnit;
    }

    public String getDefaultCity() {
        return defaultCity;
    }

    public void setDefaultCity(String defaultCity) {
        this.defaultCity = defaultCity;
    }

    public boolean isAqiVisible() {
        return aqiVisible;
    }

    public void setAqiVisible(boolean aqiVisible) {
        this.aqiVisible = aqiVisible;
    }
}
