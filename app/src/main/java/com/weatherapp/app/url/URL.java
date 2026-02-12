package com.weatherapp.app.url;

import com.weatherapp.app.location.LocationCord;

public class URL {
    private String link;
    private static String city_url;

    public URL() {
        link = "https://api.open-meteo.com/v1/forecast?latitude="
                + LocationCord.lat + "&longitude=" + LocationCord.lon
                + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m,pressure_msl,is_day,apparent_temperature,uv_index,visibility,cloud_cover"
                + "&hourly=temperature_2m,weather_code,precipitation_probability,rain,wind_speed_10m,wind_gusts_10m,uv_index"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,precipitation_sum"
                + "&timeformat=unixtime"
                + "&timezone=auto";
    }

    public static String getAirQualityUrl() {
        return "https://air-quality-api.open-meteo.com/v1/air-quality?latitude="
                + LocationCord.lat + "&longitude=" + LocationCord.lon
                + "&current=us_aqi,pm10,pm2_5,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone"
                + "&timezone=auto";
    }

    public String getLink() {
        return link;
    }


    public static void setCity_url(String cityName) {
        city_url = "https://geocoding-api.open-meteo.com/v1/search?name=" + cityName + "&count=1&language=en&format=json";
    }

    public static String getCity_url() {
        return city_url;
    }

}