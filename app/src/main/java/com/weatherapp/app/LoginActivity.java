package com.weatherapp.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.weatherapp.app.logic.FirestoreHelper;
import com.weatherapp.app.model.User;
import com.weatherapp.app.toast.Toaster;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailEt, passwordEt;
    private Button loginBtn, googleSignInBtn;
    private TextView signupTv, forgotPasswordTv;
    private FirebaseAuth auth;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private FirestoreHelper firestoreHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        firestoreHelper = new FirestoreHelper();

        // Check if user is already logged in
        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        initViews();
    }

    private void initViews() {
        emailEt = findViewById(R.id.et_email);
        passwordEt = findViewById(R.id.et_password);
        loginBtn = findViewById(R.id.btn_login);
        googleSignInBtn = findViewById(R.id.btn_google_signin);
        signupTv = findViewById(R.id.tv_signup);
        forgotPasswordTv = findViewById(R.id.tv_forgot_password);

        loginBtn.setOnClickListener(v -> {
            String email = emailEt.getText().toString().trim();
            String password = passwordEt.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toaster.errorToast(LoginActivity.this, "Please fill all fields");
                return;
            }

            loginUser(email, password);
        });

        googleSignInBtn.setOnClickListener(v -> {
            // Show progress to prevent double-click
            googleSignInBtn.setEnabled(false);
            googleSignInBtn.setText("Signing in...");
            signInWithGoogle();
        });

        signupTv.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignupActivity.class)));
        
        forgotPasswordTv.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void showForgotPasswordDialog() {
        android.widget.EditText resetMail = new android.widget.EditText(this);
        resetMail.setHint("Enter your email");
        android.app.AlertDialog.Builder passwordResetDialog = new android.app.AlertDialog.Builder(this);
        passwordResetDialog.setTitle("Reset Password?");
        passwordResetDialog.setMessage("Enter the email to receive reset link.");
        passwordResetDialog.setView(resetMail);

        passwordResetDialog.setPositiveButton("Yes", (dialog, which) -> {
            String mail = resetMail.getText().toString();
            if(mail.isEmpty()){
                Toaster.errorToast(LoginActivity.this, "Please enter email");
                return;
            }
            auth.sendPasswordResetEmail(mail).addOnSuccessListener(unused -> 
                Toaster.successToast(LoginActivity.this, "Reset Link Sent To Your Email.")
            ).addOnFailureListener(e -> 
                Toaster.errorToast(LoginActivity.this, "Error! Reset Link Not Sent: " + e.getMessage())
            );
        });

        passwordResetDialog.setNegativeButton("No", (dialog, which) -> {
            // Close the dialog
        });

        passwordResetDialog.create().show();
    }

    private void loginUser(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    Toaster.successToast(LoginActivity.this, "Login Successful");
                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Toaster.errorToast(LoginActivity.this, "Login Failed: " + e.getMessage()));
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                resetGoogleSignInButton();
                Toaster.errorToast(this, "Google Sign In Failed: " + e.getMessage());
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        googleSignInBtn.setText("Authenticating...");
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("LoginActivity", "Firebase Auth Success");
                            googleSignInBtn.setText("Checking Profile...");
                            FirebaseUser user = auth.getCurrentUser();
                            checkAndCreateUser(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.e("LoginActivity", "Firebase Auth Failed", task.getException());
                            resetGoogleSignInButton();
                            Toaster.errorToast(LoginActivity.this, "Authentication Failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown Error"));
                        }
                    }
                });
    }

    private void checkAndCreateUser(FirebaseUser firebaseUser) {
        Log.d("LoginActivity", "Checking user profile for: " + firebaseUser.getUid());
        firestoreHelper.getUser(firebaseUser.getUid(), new FirestoreHelper.UserCallback() {
            @Override
            public void onSuccess(User user) {
                // User exists, proceed to home
                Log.d("LoginActivity", "User profile found");
                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                // User doesn't exist, create profile
                Log.d("LoginActivity", "User profile not found, creating new...");
                googleSignInBtn.setText("Creating Account...");
                
                String name = firebaseUser.getDisplayName();
                if (name == null || name.isEmpty()) name = "User";
                
                String email = firebaseUser.getEmail();
                if (email == null || email.isEmpty()) email = "No Email";

                User newUser = new User(firebaseUser.getUid(), name, email);
                
                // Add strict timeout - if Firestore doesn't respond in 5s, proceed anyway
                // This handles cases where offline persistence queues the write but callback waits for server
                android.os.Handler handler = new android.os.Handler();
                Runnable timeoutRunnable = () -> {
                    Log.w("LoginActivity", "Firestore write timed out, proceeding with cached auth");
                    if (auth.getCurrentUser() != null) {
                         Toaster.successToast(LoginActivity.this, "Welcome! (Offline mode)");
                         startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                         finish();
                    }
                };
                handler.postDelayed(timeoutRunnable, 5000); // 5 seconds

                firestoreHelper.saveUser(newUser, unused -> {
                     handler.removeCallbacks(timeoutRunnable); // Cancel timeout
                     Log.d("LoginActivity", "User profile created");
                     Toaster.successToast(LoginActivity.this, "Account Created via Google");
                     startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                     finish();
                }, e1 -> {
                     handler.removeCallbacks(timeoutRunnable); // Cancel timeout
                     Log.e("LoginActivity", "Failed to create profile", e1);
                     resetGoogleSignInButton();
                     Toaster.errorToast(LoginActivity.this, "Failed to create profile: " + e1.getMessage());
                });
            }
        });
    }

    private void resetGoogleSignInButton() {
        googleSignInBtn.setEnabled(true);
        googleSignInBtn.setText("Sign in with Google");
    }
}
