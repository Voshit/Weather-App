package com.weatherapp.app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.room.Room;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.weatherapp.app.adapter.CityAdapter;
import com.weatherapp.app.databinding.ActivityManageCitiesBinding;
import com.weatherapp.app.db.AppDatabase;
import com.weatherapp.app.db.CityEntity;
import com.weatherapp.app.toast.Toaster;
import com.weatherapp.app.url.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ManageCitiesActivity extends AppCompatActivity {

    private ActivityManageCitiesBinding binding;
    private AppDatabase db;
    private CityAdapter adapter;
    private List<CityEntity> cityList;
    private Handler handler;
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManageCitiesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "weather_db")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();

        setupRecyclerView();
        setupSearch();
        setupListeners();
    }

    private void setupRecyclerView() {
        refreshCityList();
        
        adapter = new CityAdapter(this, cityList, city -> {
            Intent intent = new Intent();
            intent.putExtra("selected_city", city.name);
            setResult(RESULT_OK, intent);
            finish();
        }, city -> {
            // Toggle Favorite
            city.isFavorite = !city.isFavorite;
            db.weatherDao().updateCityFavorite(city.name, city.isFavorite);
            refreshCityList();
            adapter.notifyDataSetChanged();
            if(city.isFavorite) Toaster.successToast(this, "Pinned " + city.name);
        }, city -> {
            // Delete City
            db.weatherDao().deleteCityByName(city.name);
            refreshCityList();
            adapter.notifyDataSetChanged();
            Toaster.successToast(ManageCitiesActivity.this, city.name + " removed");
        });
        
        binding.citiesRv.setLayoutManager(new LinearLayoutManager(this));
        binding.citiesRv.setAdapter(adapter);
        
        // Swipe to Delete
        androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback simpleCallback = new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@androidx.annotation.NonNull androidx.recyclerview.widget.RecyclerView recyclerView, @androidx.annotation.NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder, @androidx.annotation.NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@androidx.annotation.NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                CityEntity city = cityList.get(position);
                
                // Delete from DB
                db.weatherDao().deleteCityByName(city.name);
                
                // Remove from list
                cityList.remove(position);
                adapter.notifyItemRemoved(position);
                Toaster.successToast(ManageCitiesActivity.this, city.name + " removed");
            }
        };
        
        new androidx.recyclerview.widget.ItemTouchHelper(simpleCallback).attachToRecyclerView(binding.citiesRv);
    }
    
    private void refreshCityList() {
        if(cityList == null) cityList = new ArrayList<>();
        cityList.clear();
        cityList.addAll(db.weatherDao().getAllCitiesOrdered());
    }

    private com.weatherapp.app.adapter.SearchAdapter searchAdapter;
    private List<String> searchList;

    private void setupSearch() {
        handler = new Handler();
        searchList = new ArrayList<>();
        
        searchAdapter = new com.weatherapp.app.adapter.SearchAdapter(this, searchList, selected -> {
            addCity(selected);
            binding.searchEt.setText("");
            binding.searchResultsRv.setVisibility(View.GONE);
            hideKeyboard(binding.searchEt);
        });
        
        binding.searchResultsRv.setLayoutManager(new LinearLayoutManager(this));
        binding.searchResultsRv.setAdapter(searchAdapter);

        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }
                if (s.length() == 0) {
                    binding.searchResultsRv.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 2) {
                    searchRunnable = () -> fetchCitySuggestions(s.toString());
                    handler.postDelayed(searchRunnable, 500); // 500ms debounce
                }
            }
        });
    }

    private void fetchCitySuggestions(String query) {
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + query + "&count=5&language=en&format=json";
        
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            try {
                if (response.has("results")) {
                    JSONArray results = response.getJSONArray("results");
                    List<String> suggestions = new ArrayList<>();
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject obj = results.getJSONObject(i);
                        String name = obj.getString("name");
                        String country = "";
                        if(obj.has("country")) country = ", " + obj.getString("country");
                        
                        // Add Admin area for better context (e.g. State)
                        String admin = "";
                        if (obj.has("admin1")) admin = ", " + obj.getString("admin1");
                        
                        suggestions.add(name + admin + country);
                    }
                    
                    runOnUiThread(() -> {
                        searchAdapter.updateList(suggestions);
                        if (!suggestions.isEmpty()) {
                            binding.searchResultsRv.setVisibility(View.VISIBLE);
                        } else {
                            binding.searchResultsRv.setVisibility(View.GONE);
                        }
                    });
                } else {
                     runOnUiThread(() -> binding.searchResultsRv.setVisibility(View.GONE));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, error -> Log.e("Search", "Error fetching suggestions"));
        queue.add(request);
    }
    
    private void hideKeyboard(View view) {
        android.view.inputmethod.InputMethodManager inputMethodManager = (android.view.inputmethod.InputMethodManager) view.getContext().getSystemService(android.app.Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void addCity(String locationName) {
        // Extract city name (remove country if needed, or simple split)
        String cityName = locationName.split(",")[0];
        
        // Fetch Lat/Lon to save
        URL.setCity_url(cityName);
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, URL.getCity_url(), null, response -> {
            try {
                if(response.has("results")) {
                    JSONObject result = response.getJSONArray("results").getJSONObject(0);
                    double lat = result.getDouble("latitude");
                    double lon = result.getDouble("longitude");
                    
                    // Insert into DB if not exists
                    if (db.weatherDao().getCityByName(cityName) == null) {
                        db.weatherDao().insertCity(new CityEntity(cityName, lat, lon));
                        Toaster.successToast(this, cityName + " added!");
                    } else {
                        Toaster.successToast(this, cityName + " already exists!");
                    }
                    
                    // Refresh list
                    refreshCityList();
                    adapter.notifyDataSetChanged();
                    
                } else {
                    Toaster.errorToast(this, "City not found");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, error -> Toaster.errorToast(this, "Network Error"));
        queue.add(request);
    }

    private void setupListeners() {
        binding.toolbar.setOnClickListener(v -> finish());
    }
}
