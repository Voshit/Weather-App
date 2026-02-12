package com.weatherapp.app.logic;

public class SummaryGenerator {
    
    public static String generateSummary(double currentTemp, double maxTemp, double minTemp, double rainChance, int aqi, String condition) {
        StringBuilder summary = new StringBuilder();
        
        // Current Temp Context
        summary.append("Currently ").append((int)currentTemp).append("°. ");
        
        // Temperature context
        if (maxTemp > 30) {
            summary.append("Hot day ahead with a high of ").append((int)maxTemp).append("°. ");
        } else if (maxTemp < 10) {
            summary.append("Cold day ahead with a high of ").append((int)maxTemp).append("°. ");
        } else {
            summary.append("Pleasant day with a high of ").append((int)maxTemp).append("°. ");
        }
        
        // Rain context
        if (rainChance > 50) {
            summary.append("Expect rain (" + (int)rainChance + "% chance). ");
        } else {
            summary.append("No significant rain expected. ");
        }
        
        // AQI context
        if (aqi > 100) {
            summary.append("Air quality is poor.");
        } else {
            summary.append("Air quality is acceptable.");
        }
        
        return summary.toString();
    }
}
