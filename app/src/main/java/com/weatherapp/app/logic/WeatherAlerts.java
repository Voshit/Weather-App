package com.weatherapp.app.logic;

import java.util.ArrayList;
import java.util.List;

public class WeatherAlerts {

    public static List<String> getAlerts(double temp, double rainChance, double windSpeed, int aqi) {
        List<String> alerts = new ArrayList<>();

        if (aqi > 150) {
            alerts.add("Health Alert: Air Quality is Unhealthy. Wear a mask!");
        }
        
        if (rainChance > 60) {
            alerts.add("Rain Alert: High chance of rain. Carry an umbrella.");
        }
        
        if (windSpeed > 40) {
            alerts.add("Wind Alert: Strong winds detected.");
        }
        
        if (temp > 40) {
            alerts.add("Heat Alert: Extreme heat. Stay hydrated.");
        } else if (temp < 0) {
            alerts.add("Freeze Alert: Temperature below freezing.");
        }

        return alerts;
    }
}
