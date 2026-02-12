package com.weatherapp.app.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "weather_cache")
public class WeatherCacheEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String url;
    public String jsonResponse;
    public long timestamp;

    public WeatherCacheEntity(String url, String jsonResponse, long timestamp) {
        this.url = url;
        this.jsonResponse = jsonResponse;
        this.timestamp = timestamp;
    }
}
