package com.weatherapp.app.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cities")
public class CityEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public double lat;
    public double lon;
    public String currentTemp;
    public String weatherDesc;
    public String aqi;

    public boolean isFavorite;

    public CityEntity(String name, double lat, double lon) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.currentTemp = "--";
        this.weatherDesc = "";
        this.aqi = "";
        this.isFavorite = false;
    }
}
