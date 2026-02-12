package com.weatherapp.app;

import static com.weatherapp.app.location.CityFinder.getCityNameUsingNetwork;
import static com.weatherapp.app.location.CityFinder.setLongitudeLatitude;
import static com.weatherapp.app.network.InternetConnectivity.isInternetConnected;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.weatherapp.app.adapter.DaysAdapter;
import com.weatherapp.app.databinding.ActivityHomeBinding;
import com.weatherapp.app.location.LocationCord;
import com.weatherapp.app.toast.Toaster;
import com.weatherapp.app.update.UpdateUI;
import com.weatherapp.app.url.URL;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;
import androidx.room.Room;
import com.weatherapp.app.db.AppDatabase;
import com.weatherapp.app.db.WeatherCacheEntity;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;

import com.weatherapp.app.adapter.HourlyAdapter;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import com.google.firebase.auth.FirebaseAuth;
import com.weatherapp.app.model.User;
import com.weatherapp.app.logic.FirestoreHelper;

public class HomeActivity extends AppCompatActivity {

    private final int WEATHER_FORECAST_APP_UPDATE_REQ_CODE = 101;   // for app update
    private static final int PERMISSION_CODE = 1;                   // for user location permission
    private String name, description, temperature, wind_speed, humidity;
    private int condition;
    private long update_time, sunset, sunrise;
    private String city = "";
    private String pressure = "-", visibility = "-", clouds = "-", dailyRain = "-", uvIndexVal = "-";
    private final int REQUEST_CODE_EXTRA_INPUT = 101;
    private final int REQUEST_CHECK_SETTINGS = 1001;
    private ActivityHomeBinding binding;
    private AppDatabase db;
    private boolean isFahrenheit = false;
    private boolean isMetersPerSecond = false;
    private User currentUser;
    private FirestoreHelper firestoreHelper;
    private String currentFeelsLikeDesc = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // binding
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        // set navigation bar color
        setNavigationBarColor();
        
        db = Room.databaseBuilder(getApplicationContext(),
                 AppDatabase.class, "weather_db")
                 .allowMainThreadQueries()
                 .fallbackToDestructiveMigration()
                 .build();

        // checkUpdate(); // Optional

        // Setup Details RecyclerView (3 columns like reference)
        binding.detailsRv.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 3));
        binding.detailsRv.setNestedScrollingEnabled(false);

        // listeners(); 
        listeners();

        // Check Auth
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        firestoreHelper = new FirestoreHelper();
        // loadUserProfile(); -> Moved to onResume to ensure sync

        // Don't call getDataUsingNetwork() here - let onResume handle it
        // This prevents crash when requesting location permission immediately after login
    }

    private void loadUserProfile() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestoreHelper.getUser(uid, new FirestoreHelper.UserCallback() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                isFahrenheit = "F".equals(user.getTempUnit());
                isMetersPerSecond = "m/s".equals(user.getWindUnit());
                
                // If default city is set and current city is empty, load it
                if (city.isEmpty() && user.getDefaultCity() != null && !user.getDefaultCity().isEmpty()) {
                    searchCity(user.getDefaultCity());
                }
                
                // Refresh UI if data already loaded
                if (temperature != null) updateUI(condition == 0 ? 1 : 0); // Hacky re-trigger
            }

            @Override
            public void onFailure(Exception e) {
                // Ignore or log
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EXTRA_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                if (data.hasExtra("selected_city")) {
                    String selectedCity = data.getStringExtra("selected_city");
                    searchCity(selectedCity);
                } else {
                    // Legacy voice input or other
                    ArrayList<String> arrayList = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (arrayList != null && !arrayList.isEmpty()) {
                         searchCity(Objects.requireNonNull(arrayList).get(0).toUpperCase());
                    }
                }
            }
        } else if (requestCode == REQUEST_CHECK_SETTINGS) {
             if (resultCode == RESULT_OK) {
                 // User enabled location
                 getDataUsingNetwork();
             } else {
                 Toaster.errorToast(this, "Location needs to be enabled for local weather.");
                 hideProgressBar();
             }
        }
    }



    private void setNavigationBarColor() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(getResources().getColor(R.color.mainBGColor));
        }
    }

    private void setUpDaysRecyclerView(org.json.JSONObject dailyData) {
        DaysAdapter daysAdapter = new DaysAdapter(this, dailyData);
        binding.dayRv.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        binding.dayRv.setAdapter(daysAdapter);
        runLayoutAnimation(binding.dayRv);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void listeners() {
        // Manage Cities functionality moved to Profile
        // binding.settingsIcon removed
        
        // Profile
        binding.profileIcon.setOnClickListener(view -> {
             Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
             startActivity(intent);
        });
        
        binding.swipeRefresh.setOnRefreshListener(() -> {
            checkConnection();
        });
        
        // binding.settingsIcon.setOnLongClickListener... removed
        
        // Removed search dialog in favor of Manage Cities
        binding.cityNameTv.setOnClickListener(v -> {
             Intent intent = new Intent(HomeActivity.this, ManageCitiesActivity.class);
             startActivityForResult(intent, REQUEST_CODE_EXTRA_INPUT);
        });
    }

    private void showSearchDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Search City");
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        
        builder.setPositiveButton("Search", (dialog, which) -> {
            String cityText = input.getText().toString();
            searchCity(cityText);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // SwipeRefreshLayout removed from layout for simplicity in this iteration
    // private void setRefreshLayoutColor() ...

    private void searchCity(String cityName) {
        if (cityName == null || cityName.isEmpty()) {
            Toaster.errorToast(this, "Please enter the city name");
        } else {
            setLatitudeLongitudeUsingCity(cityName);
        }
    }

    private void getDataUsingNetwork() {
        // Check permissions first
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
            return;
        }
        
        // Use SettingsClient to check if Location Services are enabled
        checkLocationSettings();
    }

    private void checkLocationSettings() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        com.google.android.gms.tasks.Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> {
            // All location settings are satisfied. The client can initialize
            // location requests here.
            fetchInternalLocation();
        });

        task.addOnFailureListener(this, e -> {
            if (e instanceof com.google.android.gms.common.api.ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    com.google.android.gms.common.api.ResolvableApiException resolvable = (com.google.android.gms.common.api.ResolvableApiException) e;
                    resolvable.startResolutionForResult(HomeActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    // Ignore the error.
                }
            } else {
                 hideProgressBar();
                 Toaster.errorToast(this, "Location services unavailable.");
            }
        });
    }

    private void fetchInternalLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        client.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    setLongitudeLatitude(location);
                    city = getCityNameUsingNetwork(this, location);
                    Log.i("LOCATION", "Got location - Lat: " + LocationCord.lat + ", Lon: " + LocationCord.lon + ", City: " + city);
                    getTodayWeatherInfo(city);
                } else {
                    Log.e("LOCATION", "Location is null");
                    // Request updates if last location is null (optional, skipping for now to keep simple)
                     hideProgressBar();
                     Toaster.errorToast(this, "Unable to get location.");
                }
            }).addOnFailureListener(e -> {
                Log.e("LOCATION", "Failed to get location: " + e.getMessage());
                hideProgressBar();
                // Toaster.errorToast(this, "Failed to get location: " + e.getMessage());
            });
    }

    private void setLatitudeLongitudeUsingCity(String cityName) {
        URL.setCity_url(cityName);
        RequestQueue requestQueue = Volley.newRequestQueue(HomeActivity.this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, URL.getCity_url(), null, response -> {
            try {
                if(response.has("results")) {
                    LocationCord.lat = response.getJSONArray("results").getJSONObject(0).getString("latitude");
                    LocationCord.lon = response.getJSONArray("results").getJSONObject(0).getString("longitude");
                    
                    // Save city if not exists
                    try {
                        if (db.weatherDao().getCityByName(cityName) == null) {
                            db.weatherDao().insertCity(new com.weatherapp.app.db.CityEntity(cityName, Double.parseDouble(LocationCord.lat), Double.parseDouble(LocationCord.lon)));
                        }
                    } catch (Exception e) {}
                    
                    getTodayWeatherInfo(cityName);
                } else {
                    Toaster.errorToast(this, "City not found");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, error -> Toaster.errorToast(this, "Please enter the correct city name"));
        requestQueue.add(jsonObjectRequest);
    }

    @SuppressLint("DefaultLocale")
    private void getTodayWeatherInfo(String name) {
        URL url = new URL();
        Log.i("API_REQUEST", "Fetching weather for: " + name);
        Log.i("API_REQUEST", "URL: " + url.getLink());
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url.getLink(), null, response -> {
            Log.i("API_SUCCESS", "Received response");
            this.name = name;
            // Cache the response
            try {
                db.weatherDao().insertWeather(new WeatherCacheEntity(url.getLink(), response.toString(), System.currentTimeMillis()));
            } catch (Exception e) {
                Log.e("DB", "Error caching data: " + e.getMessage());
            }
            parseWeatherData(response);
        }, error -> {
             Log.e("API_ERROR", "Error fetching weather: " + error.toString());
             // Try to load from cache
             WeatherCacheEntity cached = db.weatherDao().getWeather(url.getLink());
             if (cached != null) {
                 Log.i("CACHE", "Loading from cache");
                 this.name = name;
                 try {
                     JSONObject response = new JSONObject(cached.jsonResponse);
                     parseWeatherData(response);
                     Toaster.errorToast(this, "Showing cached data. No internet connection.");
                 } catch (JSONException e) {
                     e.printStackTrace();
                     hideProgressBar();
                 }
             } else {
                 hideProgressBar();
                 Toaster.errorToast(this, "Failed to fetch weather data. Please check your internet connection.");
             }
        });
        requestQueue.add(jsonObjectRequest);
        Log.i("json_req", "Day 0");
    }

    private void parseWeatherData(JSONObject response) {
        try {
            if(response.has("current")) {
                update_time = response.getJSONObject("current").getLong("time");

                condition = response.getJSONObject("current").getInt("weather_code");
                int isDay = response.getJSONObject("current").getInt("is_day");
                
                sunrise = response.getJSONObject("daily").getJSONArray("sunrise").getLong(0); 
                sunset = response.getJSONObject("daily").getJSONArray("sunset").getLong(0);
                
                description = UpdateUI.getWeatherDescription(condition);

                temperature = String.format("%.0f", response.getJSONObject("current").getDouble("temperature_2m"));
                wind_speed = String.format("%.0f", response.getJSONObject("current").getDouble("wind_speed_10m"));
                humidity = String.format("%.0f", response.getJSONObject("current").getDouble("relative_humidity_2m"));
                double feelsLikeVal = response.getJSONObject("current").getDouble("apparent_temperature");
                String feelsLike = String.format("%.0f", feelsLikeVal);
                 String uvIndex = String.format("%.0f", response.getJSONObject("current").getDouble("uv_index"));
                 uvIndexVal = uvIndex;
                 
                 // Enhanced Feels Like
                 try {
                     double tempVal = Double.parseDouble(temperature);
                     // feelsLikeVal already defined and parsed above at line 412
                     
                     
                     double humVal = Double.parseDouble(humidity);

                     double windVal = Double.parseDouble(wind_speed);
                     
                     String feelsLikeDesc = UpdateUI.getFeelsLikeDescription(tempVal, feelsLikeVal, humVal, windVal, condition, isDay == 1);
                     currentFeelsLikeDesc = feelsLikeDesc; // Save state
                     binding.feelsLikeTv.setText("Feels like " + feelsLike + "°. " + feelsLikeDesc);
                 } catch (Exception e) {
                     binding.feelsLikeTv.setText("Feels like " + feelsLike + "°");
                 }
                 
                 int windDir = response.getJSONObject("current").getInt("wind_direction_10m");
                 String windDirection = UpdateUI.getWindDirection((int)windDir);
  
                 double rainVal = response.getJSONObject("daily").getJSONArray("precipitation_sum").getDouble(0);
                 dailyRain = String.format("%.1f mm", rainVal);
                 
                 // New metrics
                 double pressVal = response.getJSONObject("current").getDouble("pressure_msl");
                 pressure = String.format("%.0f", pressVal);
                 
                 double visVal = response.getJSONObject("current").getDouble("visibility") / 1000.0; // Convert m to km
                 visibility = String.format("%.1f", visVal);
                 
                 double cloudVal = response.getJSONObject("current").getDouble("cloud_cover");
                 clouds = String.format("%.0f", cloudVal);
                 
                 updateDetails("-", uvIndexVal, humidity, wind_speed, windDirection, "0", pressure, visibility, clouds, dailyRain);
                
                if (response.has("daily")) {
                     JSONObject daily = response.getJSONObject("daily");
                     
                     double maxVal = daily.getJSONArray("temperature_2m_max").getDouble(0);
                     double minVal = daily.getJSONArray("temperature_2m_min").getDouble(0);
                     
                     maxTempStr = maxVal;
                     minTempStr = minVal;
                     
                     // String todayMaxT = String.format("%.0f", maxVal);
                     // String todayMinT = String.format("%.0f", minVal);
                     // binding.minMaxTempTv.setText("H:" + todayMaxT + "° L:" + todayMinT + "°");
                     
                     // Set Description
                     binding.weatherDescTvMain.setText(description);
                     com.weatherapp.app.adapter.DaysAdapter daysAdapter = new com.weatherapp.app.adapter.DaysAdapter(this, daily);
                     daysAdapter.setOnDayClickListener((pos, data) -> {
                        try {
                            int index = pos + 1;
                            org.json.JSONArray timeArr = data.getJSONArray("time");
                            if (index < timeArr.length()) {
                                 long time = timeArr.getLong(index);
                                 String dayName = new java.text.SimpleDateFormat("EEEE", java.util.Locale.ENGLISH).format(new java.util.Date(time * 1000));
                                 String maxT = String.format("%.0f", data.getJSONArray("temperature_2m_max").getDouble(index));
                                 String minT = String.format("%.0f", data.getJSONArray("temperature_2m_min").getDouble(index));
                                 int wCode = data.getJSONArray("weather_code").getInt(index);
                                 
                                 // Update Main Card temporarily for preview
                                 binding.weatherDescTvMain.setText(dayName); 
                                 binding.minMaxTempTv.setText("H:" + maxT + "° L:" + minT + "°");
                                 binding.currentTempTv.setText(maxT + "°"); 
                                 binding.feelsLikeTv.setText("Forecast");
                                 
                                 long sunrise = data.getJSONArray("sunrise").getLong(index);
                                 long sunset = data.getJSONArray("sunset").getLong(index);
                                 binding.mainWeatherIcon.setImageResource(
                                    getResources().getIdentifier(UpdateUI.getIconID(wCode, 1, sunrise, sunset), "drawable", getPackageName())
                                 );
                                 
                                 binding.smartSummaryTv.setText("Forecast for " + dayName + ": High of " + maxT + "°, Low of " + minT + "°.");
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                    binding.dayRv.setAdapter(daysAdapter);
                }
                
                updateUI(isDay); // Moved here to ensure H/L temp unit conversion uses updated maxTempStr/minTempStr

                
                // Persist Basic Weather Data
                try {
                    db.weatherDao().updateCityBasic(name, temperature, description);
                } catch (Exception e) {
                   Log.e("DB", "Error updating city: " + e.getMessage());
                }

                if (response.has("hourly")) {
                    JSONObject hourly = response.getJSONObject("hourly");
                    setupHourlyRecyclerView(hourly);
                    setupCharts(hourly);
                    
                    try {
                        rainProb = hourly.getJSONArray("precipitation_probability").getDouble(0);
                    } catch (Exception e) {}
                    
                    updateSmartSummary();
                    
                    // Wind gusts from hourly (current hour) - now handled in updateDetails
                    // Removed binding.windGustTv reference as view was deleted
                }
                
                getAirQuality();
                setupMap("wind"); // Default
                setupMapListeners();
                
                hideProgressBar();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    
    @SuppressLint("SetTextI18n")
    private void updateDetails(String aqi, String uv, String humidity, String windSpeed, String windDir, 
                               String windGust, String pressure, String visibility, String clouds, String dailyRain) {
                               
        java.util.List<com.weatherapp.app.model.DetailItem> items = new ArrayList<>();
        
        // AQI - Check preference
        if (currentUser == null || currentUser.isAqiVisible()) {
            String aqiStatus = "Moderate"; 
            int aqiColor = 0xFFFFFFFF; // White default
            try {
                int aqiVal = Integer.parseInt(aqi);
                if (aqiVal <= 50) {
                    aqiStatus = "Good";
                    aqiColor = 0xFF66BB6A; // Green
                } else if (aqiVal <= 100) {
                    aqiStatus = "Moderate";
                    aqiColor = 0xFFFFA726; // Orange
                } else if (aqiVal <= 150) {
                    aqiStatus = "Unhealthy for Sensitive Groups";
                    aqiColor = 0xFFFF7043; // Deep Orange
                } else {
                    aqiStatus = "Unhealthy";
                    aqiColor = 0xFFEF5350; // Red
                }
            } catch (NumberFormatException e) { aqiStatus = ""; }
            
            items.add(new com.weatherapp.app.model.DetailItem("Air Quality", aqi, aqiStatus, R.drawable.ic_cloud, aqiColor));
        }
        
        // UV
        String uvAdvice = "Moderate";
        int uvColor = 0xFFFFFFFF;
        try {
            double uvVal = Double.parseDouble(uv);
            if (uvVal <= 2) {
                uvAdvice = "Low";
                uvColor = 0xFF66BB6A;
            } else if (uvVal <= 5) {
                uvAdvice = "Moderate";
                uvColor = 0xFFFFA726;
            } else if (uvVal <= 7) {
                uvAdvice = "High";
                uvColor = 0xFFFF7043;
            } else {
                uvAdvice = "Very High";
                uvColor = 0xFFEF5350;
            }
        } catch (NumberFormatException e) { uvAdvice = ""; }
        items.add(new com.weatherapp.app.model.DetailItem("UV Index", uv, uvAdvice, R.drawable.ic_uv, uvColor));
        
        // Humidity
        items.add(new com.weatherapp.app.model.DetailItem("Humidity", humidity + "%", "Dew point: --", R.drawable.ic_humidity, 0xFF42A5F5));
        
        // Wind - Check unit
        String finalWindSpeed = windSpeed;
        String unit = "km/h";
        if (isMetersPerSecond) {
             try {
                 double ws = Double.parseDouble(windSpeed);
                 finalWindSpeed = String.format("%.1f", ws * 0.277778);
                 unit = "m/s";
             } catch(Exception e) {}
        }
        items.add(new com.weatherapp.app.model.DetailItem("Wind", finalWindSpeed + " " + unit, windDir + " (Gusts: " + windGust + ")", R.drawable.ic_wind, 0xFF26C6DA));
        
        // Rain
        items.add(new com.weatherapp.app.model.DetailItem("Daily Rain", dailyRain, "", R.drawable.ic_rain_drop, 0xFF5C6BC0));
        
        // Pressure
        items.add(new com.weatherapp.app.model.DetailItem("Pressure", pressure + " hPa", "", R.drawable.ic_pressure, 0xFFAB47BC));
        
        // Visibility
        items.add(new com.weatherapp.app.model.DetailItem("Visibility", visibility + " km", "Excellent", R.drawable.ic_visibility, 0xFF78909C));
        
        // Cloud Cover
        items.add(new com.weatherapp.app.model.DetailItem("Cloud Cover", clouds + "%", "", R.drawable.ic_cloud, 0xFF90CAF9));
        
        com.weatherapp.app.adapter.DetailsAdapter adapter = new com.weatherapp.app.adapter.DetailsAdapter(this, items);
        binding.detailsRv.setAdapter(adapter);
        runLayoutAnimation(binding.detailsRv);
    }   
    
    @SuppressLint("SetTextI18n")
    private void updateUI(int isDay) {
        binding.cityNameTv.setText(name);
        
        // Date Format: Monday, 12 Feb
        String dateString = new SimpleDateFormat("EEEE, d MMM", Locale.ENGLISH).format(new Date(update_time * 1000));
        binding.dateTv.setText(dateString);

        binding.mainWeatherIcon.setImageResource(
                getResources().getIdentifier(
                        UpdateUI.getIconID(condition, isDay, sunrise, sunset),
                        "drawable",
                        getPackageName()
                ));
        binding.weatherDescTvMain.setText(description);
        
        String tempUnit = isFahrenheit ? "°F" : "°C";
        String displayTemp = UpdateUI.convertTemp(temperature, isFahrenheit);
        String displayFeels = UpdateUI.convertTemp(temperature, isFahrenheit); 

        // Animation for city name
        android.view.animation.Animation fadeInSlide = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.anim_fade_in_slide);
        binding.cityNameTv.startAnimation(fadeInSlide); 
        
        // Use today's min/max from forecast if available
        String todayMaxT = maxTempStr != 0 ? String.format("%.0f", maxTempStr) : "27"; // Use existing double vars
        String todayMinT = minTempStr != 0 ? String.format("%.0f", minTempStr) : "18";
        
        // Convert High/Low
        todayMaxT = UpdateUI.convertTemp(todayMaxT, isFahrenheit);
        todayMinT = UpdateUI.convertTemp(todayMinT, isFahrenheit);
        
        binding.currentTempTv.setText(displayTemp + tempUnit);
        binding.minMaxTempTv.setText("H:" + todayMaxT + "° L:" + todayMinT + "°");
        
        // Preserve description if available
        if (currentFeelsLikeDesc != null && !currentFeelsLikeDesc.isEmpty()) {
             binding.feelsLikeTv.setText("Feels like " + displayFeels + tempUnit + ". " + currentFeelsLikeDesc);
        } else {
             binding.feelsLikeTv.setText("Feels like " + displayFeels + tempUnit); 
        }

        // Unit Indicator Styling
        android.text.SpannableString ss = new android.text.SpannableString("°C / °F");
        android.text.style.ForegroundColorSpan activeSpan = new android.text.style.ForegroundColorSpan(android.graphics.Color.WHITE);
        android.text.style.ForegroundColorSpan inactiveSpan = new android.text.style.ForegroundColorSpan(0x80FFFFFF); // 50% transparent white
        
        if (isFahrenheit) {
            ss.setSpan(inactiveSpan, 0, 2, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // °C
            ss.setSpan(activeSpan, 5, 7, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // °F
        } else {
            ss.setSpan(activeSpan, 0, 2, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // °C
            ss.setSpan(inactiveSpan, 5, 7, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // °F (assuming spacing)
        }
        binding.unitToggle.setText(ss);
        

        binding.statusPill.setText("Updated •");
        
        // Dynamic Background
        int bgRes = R.drawable.gradient_clear_day; // Default
        if (condition == 0 || condition == 1) { // Clear
            bgRes = (isDay == 1) ? R.drawable.gradient_clear_day : R.drawable.gradient_clear_night;
        } else if (condition >= 51 && condition <= 67) { // Rain
            bgRes = R.drawable.gradient_rain;
        } else if (condition >= 80 && condition <= 82) { // Rain showers
            bgRes = R.drawable.gradient_rain;
        } else if (condition >= 95) { // Thunderstorm
            bgRes = R.drawable.gradient_rain; // Or maybe a darker storm gradient? Reuse rain for now.
        } else { // Cloudy / Fog / Snow (Treat snow as cloudy/cold for now or add snow gradient)
            bgRes = R.drawable.gradient_clouds;
        }
        binding.mainLayout.setBackgroundResource(bgRes);
    }

    private void showSavedCitiesDialog() {
        java.util.List<com.weatherapp.app.db.CityEntity> cities = db.weatherDao().getAllCities();
        String[] cityNames = new String[cities.size()];
        for(int i=0; i<cities.size(); i++) cityNames[i] = cities.get(i).name;
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Saved Cities");
        builder.setItems(cityNames, (dialog, which) -> {
            searchCity(cityNames[which]);
        });
        builder.show();
    }



    private void hideProgressBar() {
        binding.progress.setVisibility(View.GONE);
        binding.swipeRefresh.setRefreshing(false);
        // binding.layout.mainLayout.setVisibility(View.VISIBLE); // No mainLayout wrapper
    }

    private void hideMainLayout() {
        binding.progress.setVisibility(View.VISIBLE);
        // binding.layout.mainLayout.setVisibility(View.GONE);
    }

    private void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) view.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void checkConnection() {
        if (!isInternetConnected(this)) {
            hideMainLayout();
            Toaster.errorToast(this, "Please check your internet connection");
        } else {
            hideProgressBar();
            getDataUsingNetwork();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toaster.successToast(this, "Permission Granted");
                getDataUsingNetwork();
            } else {
                Toaster.errorToast(this, "Permission Denied");
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Load user profile to sync preferences (C/F, Units) every time we return
        if (firestoreHelper != null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            loadUserProfile();
        }
        checkConnection();
    }

    private void checkUpdate() {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(HomeActivity.this);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, HomeActivity.this, WEATHER_FORECAST_APP_UPDATE_REQ_CODE);
                } catch (IntentSender.SendIntentException exception) {
                    Toaster.errorToast(this, "Update Failed");
                }

}
        });
    }

    private void setupHourlyRecyclerView(JSONObject hourly) {
        HourlyAdapter adapter = new HourlyAdapter(this, hourly);
        binding.hourlyRv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.hourlyRv.setAdapter(adapter);
        runLayoutAnimation(binding.hourlyRv);
    }
    
    private void runLayoutAnimation(final RecyclerView recyclerView) {
        final android.content.Context context = recyclerView.getContext();
        final LayoutAnimationController controller =
                new LayoutAnimationController(AnimationUtils.loadAnimation(context, R.anim.item_animation_slide_right));
        controller.setDelay(0.15f);
        controller.setOrder(LayoutAnimationController.ORDER_NORMAL);
        recyclerView.setLayoutAnimation(controller);
        recyclerView.getAdapter().notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();
    }

    private void setupCharts(JSONObject hourly) {
        try {
            ArrayList<Entry> tempEntries = new ArrayList<>();
            ArrayList<Entry> rainEntries = new ArrayList<>();
            
            JSONArray temps = hourly.getJSONArray("temperature_2m");
            JSONArray rainProbs = hourly.getJSONArray("precipitation_probability");
            JSONArray times = hourly.getJSONArray("time");
            ArrayList<String> xLabels = new ArrayList<>();

            for (int i = 0; i < 24; i++) {
                tempEntries.add(new Entry(i, (float) temps.getDouble(i)));
                rainEntries.add(new Entry(i, (float) rainProbs.getDouble(i)));
                long t = times.getLong(i);
                xLabels.add(new SimpleDateFormat("HH", Locale.ENGLISH).format(new Date(t * 1000)));
            }
            
            int primaryColor = getResources().getColor(R.color.textColorPrimary);
            int rainColor = 0xFF42A5F5; // Blue for rain

            LineDataSet tempSet = new LineDataSet(tempEntries, "Temperature");
            tempSet.setColor(primaryColor);
            tempSet.setCircleColor(primaryColor);
            tempSet.setLineWidth(2f);
            tempSet.setCircleRadius(4f);
            tempSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            tempSet.setDrawFilled(true);
            tempSet.setFillDrawable(ContextCompat.getDrawable(this, R.drawable.chart_fill_temp));
            tempSet.setDrawValues(false);
            tempSet.setAxisDependency(com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT);

            LineDataSet rainSet = new LineDataSet(rainEntries, "Rain Chance");
            rainSet.setColor(rainColor);
            rainSet.setCircleColor(rainColor);
            rainSet.setLineWidth(2f);
            rainSet.setCircleRadius(4f);
            rainSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            rainSet.setDrawFilled(true);
            rainSet.setFillDrawable(ContextCompat.getDrawable(this, R.drawable.chart_fill_rain));
            rainSet.setAxisDependency(com.github.mikephil.charting.components.YAxis.AxisDependency.RIGHT);
            rainSet.setDrawValues(false);

            LineData data = new LineData(tempSet, rainSet);
            binding.tempChart.setData(data);
            
            // X Axis Style
            XAxis xAxis = binding.tempChart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setTextColor(primaryColor);
            xAxis.setDrawGridLines(false);
            xAxis.setDrawAxisLine(false);

            // Left Axis (Temp) Style
            com.github.mikephil.charting.components.YAxis leftAxis = binding.tempChart.getAxisLeft();
            leftAxis.setTextColor(primaryColor);
            leftAxis.setDrawGridLines(false);
            leftAxis.setDrawAxisLine(false);

            // Right Axis (Rain) Style
            com.github.mikephil.charting.components.YAxis rightAxis = binding.tempChart.getAxisRight();
            rightAxis.setTextColor(rainColor);
            rightAxis.setDrawGridLines(false);
            rightAxis.setDrawAxisLine(false);
            rightAxis.setAxisMinimum(0f);
            rightAxis.setAxisMaximum(100f);
            
            binding.tempChart.getDescription().setEnabled(false);
            binding.tempChart.getLegend().setTextColor(primaryColor);
            binding.tempChart.setTouchEnabled(true);
            binding.tempChart.animateX(1000); // Animate chart
            binding.tempChart.invalidate();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private double maxTempStr = 0, minTempStr = 0, rainProb = 0;
    private int currentAqi = 0;

    private void updateSmartSummary() {
        try {
            double tempVal = Double.parseDouble(temperature);
            double windVal = (wind_speed.equals("-")) ? 0 : Double.parseDouble(wind_speed);

            // Default to local logic first (or use as placeholder)
            String localSummary = com.weatherapp.app.logic.SummaryGenerator.generateSummary(tempVal, maxTempStr, minTempStr, rainProb, currentAqi, description);
            binding.smartSummaryTv.setText("Generating smart summary... \n\n" + localSummary); // Temporary feedback
            
            // Call Hugging Face API
            com.weatherapp.app.logic.HuggingFaceService hfService = new com.weatherapp.app.logic.HuggingFaceService(this);
            hfService.generateWeatherSummary(description, tempVal, maxTempStr, minTempStr, rainProb, currentAqi, new com.weatherapp.app.logic.HuggingFaceService.SummaryCallback() {
                @Override
                public void onSuccess(String summary) {
                     // Add Alerts to the AI Summary
                    java.util.List<String> alerts = com.weatherapp.app.logic.WeatherAlerts.getAlerts(tempVal, rainProb, windVal, currentAqi);
                    StringBuilder finalSummary = new StringBuilder(summary);
                    if (!alerts.isEmpty()) {
                        finalSummary.append("\n\n⚠️ ").append(alerts.get(0));
                    }
                    
                     // Add Health Advisory
                    double uvVal = (uvIndexVal.equals("-")) ? 0 : Double.parseDouble(uvIndexVal);
                    String health = com.weatherapp.app.logic.HealthAdvisory.getAdvisory(currentAqi, uvVal);
                    if (!health.isEmpty()) {
                        finalSummary.append("\n\n💡 ").append(health);
                    }
                    
                    binding.smartSummaryTv.setText(finalSummary.toString());
                }

                @Override
                public void onError(String error) {
                     Log.e("HF_API", "Failed: " + error);
                     // Fallback to local logic entirely
                     String fallback = com.weatherapp.app.logic.SummaryGenerator.generateSummary(tempVal, maxTempStr, minTempStr, rainProb, currentAqi, description);
                     
                     // Add Alerts to Fallback
                    java.util.List<String> alerts = com.weatherapp.app.logic.WeatherAlerts.getAlerts(tempVal, rainProb, windVal, currentAqi);
                    if (!alerts.isEmpty()) {
                        fallback += "\n\n⚠️ " + alerts.get(0);
                    }
                     // Add Health Advisory
                    double uvVal = (uvIndexVal.equals("-")) ? 0 : Double.parseDouble(uvIndexVal);
                    String health = com.weatherapp.app.logic.HealthAdvisory.getAdvisory(currentAqi, uvVal);
                    if (!health.isEmpty()) {
                        fallback += "\n\n💡 " + health;
                    }
                    
                    binding.smartSummaryTv.setText(fallback);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getAirQuality() {
        String url = URL.getAirQualityUrl();
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            try {
                db.weatherDao().insertWeather(new WeatherCacheEntity(url, response.toString(), System.currentTimeMillis()));
            } catch (Exception e) {
                Log.e("DB", "Error caching AQI: " + e.getMessage());
            }
            parseAirQualityData(response);
        }, error -> {
            Log.e("API", "AQI Error: " + error.toString());
            try {
                WeatherCacheEntity cached = db.weatherDao().getWeather(url);
                if (cached != null) {
                    parseAirQualityData(new JSONObject(cached.jsonResponse));
                }
            } catch (Exception e) {
                Log.e("CACHE", "AQI Cache error: " + e.getMessage());
            }
        });
        requestQueue.add(request);
    }

    private void parseAirQualityData(JSONObject response) {
        try {
            if(response.has("current")) {
                double aqi = response.getJSONObject("current").getDouble("us_aqi");
                currentAqi = (int) aqi;
                
                String status = "Good";
                if (aqi > 50) { status = "Moderate"; }
                if (aqi > 100) { status = "Unhealthy"; }
                
                try {
                    String aqiText = String.format("%.0f", aqi) + " - " + status;
                    db.weatherDao().updateCityAQI(name, aqiText);
                } catch (Exception e) {
                   Log.e("DB", "Error updating city AQI: " + e.getMessage());
                }
                
                updateDetails(String.format("%.0f", aqi), uvIndexVal, humidity, wind_speed, 
                              " ", "", pressure, visibility, clouds, dailyRain);
                              
                updateSmartSummary();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    private void setupMap(String overlay) {
        android.webkit.WebView webView = binding.mapWebView;
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        
        // Use current location or default
        String lat = LocationCord.lat;
        String lon = LocationCord.lon;
        if(lat == null) lat = "51.5";
        if(lon == null) lon = "-0.12"; // London
        
        String url = "https://embed.windy.com/embed2.html?lat=" + lat + "&lon=" + lon + 
                     "&detailLat=" + lat + "&detailLon=" + lon + 
                     "&width=650&height=450&zoom=5&level=surface&overlay=" + overlay + 
                     "&product=ecmwf&menu=&message=&marker=&calendar=now&pressure=&type=map&location=coordinates&detail=&metricWind=default&metricTemp=default&radarRange=-1";
        
        webView.loadUrl(url);
    }

    private void setupMapListeners() {
        binding.mapChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String overlay = "wind";
            if (checkedId == R.id.chip_temp) overlay = "temp";
            else if (checkedId == R.id.chip_rain) overlay = "rain";
            else if (checkedId == R.id.chip_clouds) overlay = "clouds";
            
            setupMap(overlay);
        });
    }

    private void toggleDarkMode() {
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            Toaster.successToast(this, "Light mode enabled");
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            Toaster.successToast(this, "Dark mode enabled");
        }
    }

}