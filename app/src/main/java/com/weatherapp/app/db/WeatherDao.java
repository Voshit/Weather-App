package com.weatherapp.app.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCity(CityEntity city);

    @Query("SELECT * FROM cities")
    List<CityEntity> getAllCities();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWeather(WeatherCacheEntity weather);

    @Query("SELECT * FROM weather_cache WHERE url = :url ORDER BY timestamp DESC LIMIT 1")
    WeatherCacheEntity getWeather(String url);

    @Query("DELETE FROM weather_cache WHERE url = :url")
    void deleteWeather(String url);
    @androidx.room.Delete
    void deleteCity(CityEntity city);

    @androidx.room.Update
    void updateCity(CityEntity city);

    @Query("UPDATE cities SET currentTemp = :temp, weatherDesc = :desc WHERE name = :name")
    void updateCityBasic(String name, String temp, String desc);

    @Query("UPDATE cities SET aqi = :aqi WHERE name = :name")
    void updateCityAQI(String name, String aqi);

    @Query("UPDATE cities SET isFavorite = :isFavorite WHERE name = :name")
    void updateCityFavorite(String name, boolean isFavorite);

    @Query("SELECT * FROM cities WHERE name = :name LIMIT 1")
    CityEntity getCityByName(String name);

    @Query("DELETE FROM cities WHERE name = :name")
    void deleteCityByName(String name);

    @Query("SELECT * FROM cities ORDER BY isFavorite DESC, id ASC")
    List<CityEntity> getAllCitiesOrdered();
}
