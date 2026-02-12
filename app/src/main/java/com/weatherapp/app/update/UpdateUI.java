package com.weatherapp.app.update;

import android.app.Activity;
import android.content.Context;

import com.weatherapp.app.R;

public class UpdateUI {

    public static String getIconID(int condition, long isDay, long sunrise, long sunset) {
        // WMO Weather interpretation codes (WW)
        // 0: Clear sky
        // 1, 2, 3: Mainly clear, partly cloudy, and overcast
        // 45, 48: Fog
        // 51, 53, 55: Drizzle: Light, moderate, and dense intensity
        // 56, 57: Freezing Drizzle: Light and dense intensity
        // 61, 63, 65: Rain: Slight, moderate and heavy intensity
        // 66, 67: Freezing Rain: Light and heavy intensity
        // 71, 73, 75: Snow fall: Slight, moderate, and heavy intensity
        // 77: Snow grains
        // 80, 81, 82: Rain showers: Slight, moderate, and violent
        // 85, 86: Snow showers slight and heavy
        // 95: Thunderstorm: Slight or moderate
        // 96, 99: Thunderstorm with slight and heavy hail

        // Mapping to existing resources
        if (condition == 0) {
            return isDay == 1 ? "clear_day" : "clear_night";
        }
        if (condition == 1) {
            return isDay == 1 ? "few_clouds_day" : "few_clouds_night";
        }
        if (condition == 2) {
            return "scattered_clouds";
        }
        if (condition == 3) {
            return "broken_clouds";
        }
        if (condition == 45 || condition == 48) {
            return "broken_clouds";
        }
        if (condition >= 51 && condition <= 67) {
            return "rain";
        }
        if (condition >= 80 && condition <= 82) {
             return "rain";
        }
        if (condition >= 71 && condition <= 77) {
            return "snow";
        }
        if (condition >= 85 && condition <= 86) {
            return "snow";
        }
        if (condition >= 95 && condition <= 99) {
            return "thunderstorm";
        }

        // Fallback
        return "scattered_clouds";
    }

    public static String getWeatherDescription(int condition) {
        if (condition == 0) return "Clear sky";
        if (condition == 1) return "Mainly clear";
        if (condition == 2) return "Partly cloudy";
        if (condition == 3) return "Overcast";
        if (condition == 45) return "Fog";
        if (condition == 48) return "Depositing rime fog";
        if (condition == 51) return "Light drizzle";
        if (condition == 53) return "Moderate drizzle";
        if (condition == 55) return "Dense drizzle";
        if (condition == 56) return "Light freez_drizzle";
        if (condition == 57) return "Dense freez_drizzle";
        if (condition == 61) return "Slight rain";
        if (condition == 63) return "Moderate rain";
        if (condition == 65) return "Heavy rain";
        if (condition == 66) return "Light freez_rain";
        if (condition == 67) return "Heavy freez_rain";
        if (condition == 71) return "Slight snow fall";
        if (condition == 73) return "Moderate snow fall";
        if (condition == 75) return "Heavy snow fall";
        if (condition == 77) return "Snow grains";
        if (condition == 80) return "Slight rain showers";
        if (condition == 81) return "Moderate rain showers";
        if (condition == 82) return "Violent rain showers";
        if (condition == 85) return "Slight snow showers";
        if (condition == 86) return "Heavy snow showers";
        if (condition == 95) return "Thunderstorm";
        if (condition == 96) return "Thunderstorm & hail";
        if (condition == 99) return "Thunderstorm & heavy hail";
        return "Unknown";
    }

    public static String getWindDirection(int degrees) {
        if (degrees >= 337.5 || degrees < 22.5) return "N";
        if (degrees >= 22.5 && degrees < 67.5) return "NE";
        if (degrees >= 67.5 && degrees < 112.5) return "E";
        if (degrees >= 112.5 && degrees < 157.5) return "SE";
        if (degrees >= 157.5 && degrees < 202.5) return "S";
        if (degrees >= 202.5 && degrees < 247.5) return "SW";
        if (degrees >= 247.5 && degrees < 292.5) return "W";
        if (degrees >= 292.5 && degrees < 337.5) return "NW";
        return "N";
    }

    public static String convertTemp(String temp, boolean isFahrenheit) {
        try {
             double t = Double.parseDouble(temp);
             if (isFahrenheit) {
                 return String.format("%.0f", (t * 9/5) + 32);
             }
             return temp; // Already Celsius
        } catch (NumberFormatException e) {
            return temp;
        }
    }

    public static String getUVAdvice(double uvIndex) {
        if (uvIndex < 3) return "Low - No protection needed";
        if (uvIndex < 6) return "Moderate - Wear sunscreen";
        if (uvIndex < 8) return "High - Sunscreen & hat required";
        if (uvIndex < 11) return "Very High - Avoid sun exposure";
        return "Extreme - Stay indoors";
    }

    public static String getVisibilityDescription(double visibilityKm) {
        if (visibilityKm >= 10) return "Excellent";
        if (visibilityKm >= 4) return "Good";
        if (visibilityKm >= 2) return "Moderate";
        if (visibilityKm >= 1) return "Poor";
        return "Very Poor";
    }

    public static String getFeelsLikeDescription(double temp, double feelsLike, double humidity, double windSpeed, int condition, boolean isDay) {
        // "Scorching" – For extreme heat where the sun feels intense and oppressive.
        if (temp >= 35) return "Scorching heat";

        // "Pool Weather" – High 80s or 90s (approx 30C+); the only solution is getting in the water.
        if (temp >= 30 && temp < 35) return "Pool Weather";

        // "Muggy" – When high humidity makes the air feel thick and "sticky".
        if (humidity >= 70 && temp >= 25) return "Muggy and humid";

        // "Toasty" – A comfortable, dry warmth.
        if (temp >= 24 && temp < 30 && humidity < 50) return "Toasty warmth";

        // "Balmy" – That perfect, tropical-feeling warmth with a soft breeze (ideal for your 77°F).
        if (temp >= 21 && temp < 26 && windSpeed < 15) return "Balmy";

        // "Idyllic" – When the temperature, wind, and sky are all objectively perfect.
        if (temp >= 20 && temp < 25 && condition <= 1 && windSpeed < 10) return "Idyllic conditions";

        // "T-shirt Weather" – Warm enough to go without layers.
        if (temp >= 20 && temp < 25) return "T-shirt Weather";

        // "Crisp" – Cool, fresh, and dry; usually associated with clear autumn days.
        if (temp >= 10 && temp < 18 && humidity < 60) return "Crisp and fresh";

        // "Brisk" – Chilly enough that a cold wind makes you walk a little faster.
        if (temp >= 5 && temp < 13 && windSpeed >= 15) return "Brisk and breezy";

        // "Light Jacket Weather" – The transition zone where you need one layer.
        if (temp >= 13 && temp < 18) return "Light Jacket Weather";

        // "Raw" – When it’s cold, damp, and windy.
        if (temp < 10 && humidity > 70 && windSpeed > 10) return "Raw cold";

        // "Biting" – Intense cold accompanied by high winds.
        if (temp < 0 && windSpeed > 20) return "Biting cold";
        
        // Fallbacks
        if (feelsLike < temp - 3) return "Feels colder due to wind";
        if (feelsLike > temp + 3) return "Feels warmer due to humidity";
        
        return "Similar to actual temp";
    }

    public static String TranslateDay(String dayToBeTranslated, Context context) {
        switch (dayToBeTranslated.trim()) {
            case "Monday":
                return context.getResources().getString(R.string.monday);
            case "Tuesday":
                return context.getResources().getString(R.string.tuesday);
            case "Wednesday":
                return context.getResources().getString(R.string.wednesday);
            case "Thursday":
                return context.getResources().getString(R.string.thursday);
            case "Friday":
                return context.getResources().getString(R.string.friday);
            case "Saturday":
                return context.getResources().getString(R.string.saturday);
            case "Sunday":
                return context.getResources().getString(R.string.sunday);
        }
        return dayToBeTranslated;
    }
}
