package com.weatherapp.app.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@androidx.room.Database(entities = {CityEntity.class, WeatherCacheEntity.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends androidx.room.RoomDatabase {
    public abstract WeatherDao weatherDao();
}
