package com.weatherapp.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.weatherapp.app.logic.FirestoreHelper;
import com.weatherapp.app.model.User;
import com.weatherapp.app.toast.Toaster;

import java.util.regex.Pattern;

public class SignupActivity extends AppCompatActivity {

    private TextInputEditText nameEt, emailEt, passwordEt;
    private TextInputLayout passwordLayout;
    private Button signupBtn;
    private TextView loginTv;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private FirestoreHelper firestoreHelper;

    // Regex for: At least 1 digit, 1 lower, 1 upper, 1 special char, no whitespace, min 6 length
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^" +
                    "(?=.*[0-9])" +         // at least 1 digit
                    "(?=.*[a-z])" +         // at least 1 lower case letter
                    "(?=.*[A-Z])" +         // at least 1 upper case letter
                    "(?=.*[@#$%^&+=!])" +    // at least 1 special character
                    "(?=\\S+$)" +           // no white spaces
                    ".{6,}" +               // at least 6 characters
                    "$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        firestoreHelper = new FirestoreHelper();

        nameEt = findViewById(R.id.et_name);
        emailEt = findViewById(R.id.et_email);
        passwordEt = findViewById(R.id.et_password);
        passwordLayout = findViewById(R.id.til_password);
        signupBtn = findViewById(R.id.btn_signup);
        loginTv = findViewById(R.id.tv_login);
        progressBar = findViewById(R.id.progressBar);

        signupBtn.setOnClickListener(v -> {
            String name = nameEt.getText().toString().trim();
            String email = emailEt.getText().toString().trim();
            String password = passwordEt.getText().toString().trim();

            if (!validateInputs(name, email, password)) {
                return;
            }

            signUp(name, email, password);
        });

        loginTv.setOnClickListener(v -> finish());
    }

    private boolean validateInputs(String name, String email, String password) {
        if (name.isEmpty()) {
            nameEt.setError("Name is required");
            nameEt.requestFocus();
            return false;
        }

        if (email.isEmpty()) {
            emailEt.setError("Email is required");
            emailEt.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            passwordLayout.setError("Password is required");
            passwordEt.requestFocus();
            return false;
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            passwordLayout.setError("Password too weak. Must be >6 chars, contain A-Z, a-z, 0-9, and special char (@#$%).");
            return false;
        } else {
            passwordLayout.setError(null);
        }

        return true;
    }

    private void signUp(final String name, final String email, String password) {
        showLoading(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    User user = new User(uid, name, email);

                    // Watchdog: If Firestore hangs for >3s, force navigation
                    android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                    Runnable watchdog = () -> {
                        showLoading(false);
                        android.widget.Toast.makeText(SignupActivity.this, "Profile saving took too long. Proceeding...", android.widget.Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignupActivity.this, HomeActivity.class));
                        finishAffinity();
                    };
                    handler.postDelayed(watchdog, 3500);

                    // Attempt Firestore Save
                    firestoreHelper.saveUser(user, unused -> {
                        handler.removeCallbacks(watchdog); // Cancel watchdog
                        try {
                            showLoading(false);
                            Toaster.successToast(SignupActivity.this, "Account Created Successfully");
                            startActivity(new Intent(SignupActivity.this, HomeActivity.class));
                            finishAffinity();
                        } catch (Exception e) {
                            startActivity(new Intent(SignupActivity.this, HomeActivity.class));
                            finishAffinity();
                        }
                    }, e -> {
                        handler.removeCallbacks(watchdog); // Cancel watchdog
                        showLoading(false);
                        // Even if profile save fails, Auth succeeded, so we might want to let them in or retry
                        Toaster.errorToast(SignupActivity.this, "Profile Init Failed: " + e.getMessage());
                        // Optional: Force navigation or stay?
                        // For now stay to let them verify credentials, but Auth is done.
                        // Better to navigate if it's just a db error.
                        startActivity(new Intent(SignupActivity.this, HomeActivity.class));
                        finishAffinity();
                    });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        Toaster.errorToast(SignupActivity.this, "Email already exists. Please Login.");
                    } else {
                        Toaster.errorToast(SignupActivity.this, "Signup Failed: " + e.getMessage());
                    }
                });
    }
    
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            signupBtn.setEnabled(false);
            signupBtn.setText("Creating Account...");
        } else {
            progressBar.setVisibility(View.GONE);
            signupBtn.setEnabled(true);
            signupBtn.setText("Sign Up");
        }
    }
}
