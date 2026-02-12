package com.weatherapp.app.logic;

public class HealthAdvisory {

    public static String getAdvisory(int aqi, double uvIndex) {
        StringBuilder advice = new StringBuilder();
        
        if (aqi > 150) {
            advice.append("Limit outdoor activities. Avoid prolonged exertion. ");
        } else if (aqi <= 50) {
            advice.append("Air quality is good. Great for outdoor activities! ");
        }
        
        if (uvIndex > 7) {
            advice.append("UV is high. Wear sunscreen and protective clothing.");
        }
        
        return advice.toString();
    }
}
