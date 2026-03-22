package com.example.transport1;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private FirebaseAuth mAuth;
    private ProgressBar progressBarLogin;
    private TextView tvForgotPassword;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean autoLoginAttempted = false;
    private boolean isWaitingForNetwork = false;
    private boolean isDestroyed = false;

    private ImageView ivTogglePassword;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_phone);

        initViews();
        setupNetworkCallback();
        attemptAutoLogin();
        setupClickListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
        unregisterNetworkCallback();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        progressBarLogin = findViewById(R.id.progressBarLogin);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);

        mAuth = FirebaseAuth.getInstance();
        connectivityManager = getSystemService(ConnectivityManager.class);
    }

    private void setupNetworkCallback() {
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> {
                    if (isDestroyed) return;
                    if (isWaitingForNetwork) {
                        isWaitingForNetwork = false;
                        unregisterNetworkCallback();
                        if (mAuth.getCurrentUser() != null && !autoLoginAttempted) {
                            autoLoginAttempted = true;
                            startAutoLogin();
                        }
                    }
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> {
                    if (isDestroyed) return;
                    isWaitingForNetwork = false;
                });
            }
        };
    }

    private void unregisterNetworkCallback() {
        if (networkCallback != null && connectivityManager != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {
            }
        }
    }

    private void attemptAutoLogin() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            if (!isNetworkAvailable()) {
                isWaitingForNetwork = true;
                showProgress(true);
                Toast.makeText(this, "इंटरनेट नाही आहे. कृपया कनेक्शन तपासा.", Toast.LENGTH_LONG).show();
                listenNetworkForAutoLogin();
            } else {
                autoLoginAttempted = true;
                startAutoLogin();
            }
        }
    }

    private void startAutoLogin() {
        showProgress(true);
        disableButtons();
        checkRoleAndGo(mAuth.getCurrentUser());
    }

    private void listenNetworkForAutoLogin() {
        if (connectivityManager != null && networkCallback != null) {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            try {
                connectivityManager.registerNetworkCallback(request, networkCallback);
            } catch (Exception ignored) {
            }
        }
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> performLogin());

        btnRegister.setOnClickListener(v -> performRegistration());

        tvForgotPassword.setOnClickListener(v -> performForgotPassword());

        ivTogglePassword.setOnClickListener(v -> {

            if (isPasswordVisible) {
                // Hide password
                etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivTogglePassword.setImageResource(android.R.drawable.ic_menu_view);
                isPasswordVisible = false;
            } else {
                // Show password
                etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivTogglePassword.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                isPasswordVisible = true;
            }

            // Cursor end la thevnyasathi
            etPassword.setSelection(etPassword.getText().length());
        });
    }

    private void performLogin() {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 1. Empty check
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter Email and Password", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Email format check
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "❌ Invalid Email Format", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Network check
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "🌐 Internet connection nahi aahe.", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);
        disableButtons();

        // 4. Direct login
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    showProgress(false);
                    enableButtons();

                    if (task.isSuccessful()) {

                        checkRoleAndGo(mAuth.getCurrentUser());

                    } else {

                        Exception e = task.getException();

                        // 🔥 IMPORTANT FIX
                        if (e != null && e.getMessage() != null) {

                            String error = e.getMessage().toLowerCase();

                            if (error.contains("no user record") || error.contains("user not found")) {

                                Toast.makeText(this,
                                        "❌ Email register nahi aahe.",
                                        Toast.LENGTH_LONG).show();

                            } else if (error.contains("password is invalid")) {

                                Toast.makeText(this,
                                        "❌ Password chukicha aahe.",
                                        Toast.LENGTH_LONG).show();

                            } else {

                                Toast.makeText(this,
                                        "❌ Email kiwa Password chukicha aahe.",
                                        Toast.LENGTH_LONG).show();
                            }

                        } else {
                            Toast.makeText(this,
                                    "❌ Login Failed",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void performRegistration() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter Email and Password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "इंटरनेट नाही आहे. कृपया कनेक्शन तपासा.", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);
        disableButtons();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        checkRoleAndGo(mAuth.getCurrentUser());
                    } else {
                        showProgress(false);
                        enableButtons();
                        String errorMessage = task.getException() != null ? 
                                task.getException().getMessage() : "Registration Failed";
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void performForgotPassword() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Please enter your email to reset password");
            etEmail.requestFocus();
            return;
        }

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "इंटरनेट नाही आहे. कृपया कनेक्शन तपासा.", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);
        disableButtons();

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(this, task -> {
                    showProgress(false);
                    enableButtons();
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "If this email is registered, a reset link has been sent.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        String errorMessage = task.getException() != null ? 
                                task.getException().getMessage() : "Unable to process request";
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkRoleAndGo(FirebaseUser user) {
        if (user == null) {
            showProgress(false);
            enableButtons();
            return;
        }

        FirebaseDatabase.getInstance().getReference("users")
                .child(user.getUid())
                .child("role")
                .get()
                .addOnCompleteListener(roleTask -> {
                    if (isDestroyed) return;

                    if (roleTask.isSuccessful()) {
                        String role = roleTask.getResult().getValue(String.class);
                        boolean isAdmin = "admin".equals(role);

                        saveLoginInfo();

                        showProgress(false);
                        enableButtons();

                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("isAdmin", isAdmin);
                        startActivity(intent);
                        finish();
                    } else {
                        showProgress(false);
                        enableButtons();
                        String errorMessage = roleTask.getException() != null ? 
                                roleTask.getException().getMessage() : "Unable to get role";
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveLoginInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        String uid = currentUser.getUid();

        LoginInfo info = new LoginInfo(deviceId, timestamp, uid);

        FirebaseDatabase.getInstance().getReference("login_info")
                .child(uid)
                .setValue(info);
    }

    private boolean isNetworkAvailable() {
        if (connectivityManager == null) return false;
        Network network = connectivityManager.getActiveNetwork();
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    private void showProgress(boolean show) {
        progressBarLogin.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void disableButtons() {
        btnLogin.setEnabled(false);
        btnRegister.setEnabled(false);
        if (tvForgotPassword != null) {
            tvForgotPassword.setEnabled(false);
        }
    }

    private void enableButtons() {
        btnLogin.setEnabled(true);
        btnRegister.setEnabled(true);
        if (tvForgotPassword != null) {
            tvForgotPassword.setEnabled(true);
        }
    }

    public static class LoginInfo {
        public String deviceId;
        public String timestamp;
        public String uid;

        public LoginInfo() {
        }

        public LoginInfo(String deviceId, String timestamp, String uid) {
            this.deviceId = deviceId;
            this.timestamp = timestamp;
            this.uid = uid;
        }
    }
}
