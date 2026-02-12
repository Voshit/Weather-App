package com.weatherapp.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.weatherapp.app.logic.FirestoreHelper;
import com.weatherapp.app.model.User;
import com.weatherapp.app.toast.Toaster;

public class ProfileActivity extends AppCompatActivity {

    private TextView nameTv, emailTv;
    private RadioGroup tempRg, windRg;
    private RadioButton rbCelsius, rbFahrenheit, rbKmh, rbMs;
    private Switch aqiSwitch;
    private Button logoutBtn;
    private Toolbar toolbar;

    private FirebaseAuth auth;
    private FirestoreHelper firestoreHelper;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_profile);

            auth = FirebaseAuth.getInstance();
            firestoreHelper = new FirestoreHelper();

            if (auth.getCurrentUser() == null) {
                Log.e("ProfileActivity", "User not authenticated");
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }
            
            Log.i("ProfileActivity", "User authenticated: " + auth.getCurrentUser().getEmail());

            initViews();
            loadUserData();
        } catch (Exception e) {
            Log.e("ProfileActivity", "Error in onCreate: " + e.getMessage(), e);
            Toaster.errorToast(this, "Failed to load profile: " + e.getMessage());
            finish();
        }
    }

    private void initViews() {
        try {
            toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);
            toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
            toolbar.setNavigationOnClickListener(v -> finish());

            nameTv = findViewById(R.id.tv_name);
            emailTv = findViewById(R.id.tv_email);
        tempRg = findViewById(R.id.rg_temp_unit);
        windRg = findViewById(R.id.rg_wind_unit);
        rbCelsius = findViewById(R.id.rb_celsius);
        rbFahrenheit = findViewById(R.id.rb_fahrenheit);
        rbKmh = findViewById(R.id.rb_kmh);
        rbMs = findViewById(R.id.rb_ms);
        aqiSwitch = findViewById(R.id.switch_aqi);
        logoutBtn = findViewById(R.id.btn_logout);
        Button manageCitiesBtn = findViewById(R.id.btn_manage_cities);

        manageCitiesBtn.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, ManageCitiesActivity.class));
        });

        // Listeners for auto-save
        tempRg.setOnCheckedChangeListener((group, checkedId) -> {
            if (currentUser == null) return;
            String val = checkedId == R.id.rb_celsius ? "C" : "F";
            if (!val.equals(currentUser.getTempUnit())) {
                updateField("tempUnit", val);
                currentUser.setTempUnit(val);
            }
        });

        windRg.setOnCheckedChangeListener((group, checkedId) -> {
            if (currentUser == null) return;
            String val = checkedId == R.id.rb_kmh ? "km/h" : "m/s";
            if (!val.equals(currentUser.getWindUnit())) {
                updateField("windUnit", val);
                currentUser.setWindUnit(val);
            }
        });

        aqiSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentUser == null) return;
            if (isChecked != currentUser.isAqiVisible()) {
                updateField("aqiVisible", isChecked);
                currentUser.setAqiVisible(isChecked);
            }
        });

        logoutBtn.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
        
        Log.i("ProfileActivity", "Views initialized successfully");
        } catch (Exception e) {
            Log.e("ProfileActivity", "Error in initViews: " + e.getMessage(), e);
            Toaster.errorToast(this, "Failed to initialize views: " + e.getMessage());
            throw e;
        }
    }

    private void loadUserData() {
        try {
            String uid = auth.getCurrentUser().getUid();
            Log.i("ProfileActivity", "Loading user data for UID: " + uid);
            
            firestoreHelper.getUser(uid, new FirestoreHelper.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    Log.i("ProfileActivity", "User data loaded successfully: " + user.getName());
                    currentUser = user;
                    updateUI(user);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e("ProfileActivity", "Failed to load user data: " + e.getMessage(), e);
                    Toaster.errorToast(ProfileActivity.this, "Failed to load profile: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e("ProfileActivity", "Error in loadUserData: " + e.getMessage(), e);
            Toaster.errorToast(this, "Error loading user data: " + e.getMessage());
        }
    }

    private void updateUI(User user) {
        nameTv.setText(user.getName());
        emailTv.setText(user.getEmail());

        if ("C".equals(user.getTempUnit())) rbCelsius.setChecked(true);
        else rbFahrenheit.setChecked(true);

        if ("km/h".equals(user.getWindUnit())) rbKmh.setChecked(true);
        else rbMs.setChecked(true);

        aqiSwitch.setChecked(user.isAqiVisible());
    }

    private void updateField(String field, Object value) {
        firestoreHelper.updatePreference(currentUser.getUid(), field, value,
                unused -> { /* Success quietly */ },
                e -> Toaster.errorToast(ProfileActivity.this, "Failed to update " + field));
    }
}
