// File Path: app/src/main/java/com/merakisan/partner/LoginActivity.java
package com.merakisan.partner;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private CardView cardGoogleLogin, cardRegisterDetails, cardPinLogin;
    private TextView txtGoogleAccountEmail, txtLocationStatus, txtWelcomeBack;
    private EditText edtFarmerName, edtVillage, edtWhatsAppNumber, edtPin, edtLoginPin;
    private Button btnGoogleSignIn, btnDetectVillage, btnSubmitRegister, btnLoginWithPin;
    private ProgressBar loginProgressBar;

    private static final String PREF_NAME = "MeraKisanPartnerPrefs";
    private static final int RC_GOOGLE_SIGN_IN = 9001;
    private static final int LOCATION_PERMISSION_REQ = 201;

    private GoogleSignInClient mGoogleSignInClient;
    private double currentLat = 24.12;
    private double currentLng = 75.58;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        cardGoogleLogin = findViewById(R.id.cardGoogleLogin);
        cardRegisterDetails = findViewById(R.id.cardRegisterDetails);
        cardPinLogin = findViewById(R.id.cardPinLogin);

        txtGoogleAccountEmail = findViewById(R.id.txtGoogleAccountEmail);
        txtLocationStatus = findViewById(R.id.txtLocationStatus);
        txtWelcomeBack = findViewById(R.id.txtWelcomeBack);

        edtFarmerName = findViewById(R.id.edtFarmerName);
        edtVillage = findViewById(R.id.edtVillage);
        edtWhatsAppNumber = findViewById(R.id.edtWhatsAppNumber);
        edtPin = findViewById(R.id.edtPin);
        edtLoginPin = findViewById(R.id.edtLoginPin);

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        btnDetectVillage = findViewById(R.id.btnDetectVillage);
        btnSubmitRegister = findViewById(R.id.btnSubmitRegister);
        btnLoginWithPin = findViewById(R.id.btnLoginWithPin);
        loginProgressBar = findViewById(R.id.loginProgressBar);

        // Real Google Sign-In Setup
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isRegistered = prefs.getBoolean("is_registered", false);

        if (isRegistered) {
            cardGoogleLogin.setVisibility(View.GONE);
            cardRegisterDetails.setVisibility(View.GONE);
            cardPinLogin.setVisibility(View.VISIBLE);
            txtWelcomeBack.setText("Namaste " + prefs.getString("farmer_name", "Kisan") + " ji (" + prefs.getString("village", "") + ")\nApna 6-ankon ka PIN daalein:");
        } else {
            cardGoogleLogin.setVisibility(View.VISIBLE);
        }

        btnGoogleSignIn.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
        });

        btnDetectVillage.setOnClickListener(v -> requestLocation());
        btnSubmitRegister.setOnClickListener(v -> handleRegistration());
        btnLoginWithPin.setOnClickListener(v -> handlePinLogin());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    // Phone ke real Google account se data fetch hua
                    String realName = account.getDisplayName();
                    String realEmail = account.getEmail();

                    cardGoogleLogin.setVisibility(View.GONE);
                    cardRegisterDetails.setVisibility(View.VISIBLE);

                    txtGoogleAccountEmail.setText("Google Khata: " + realEmail);
                    if (realName != null && !realName.isEmpty()) {
                        edtFarmerName.setText(realName); // Auto-fill real name
                    }

                    Toast.makeText(this, "Google khata jud gaya! Naam zaroorat anusaar badal sakte hain.", Toast.LENGTH_SHORT).show();
                    requestLocation();
                }
            } catch (ApiException e) {
                // Agar user cancel kare ya play services na ho
                Toast.makeText(this, "Google sign-in radd hua. Vivran manually bharein.", Toast.LENGTH_SHORT).show();
                cardGoogleLogin.setVisibility(View.GONE);
                cardRegisterDetails.setVisibility(View.VISIBLE);
                requestLocation();
            }
        }
    }

    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQ);
        } else {
            fetchLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQ && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        }
    }

    private void fetchLocation() {
        txtLocationStatus.setText("📍 GPS se gaon khoja ja raha hai...");
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return;

        try {
            Location loc = null;
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (loc == null && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (loc != null) {
                currentLat = loc.getLatitude();
                currentLng = loc.getLongitude();
                resolveVillage(currentLat, currentLng);
            }
        } catch (SecurityException ignored) {}
    }

    private void resolveVillage(double lat, double lng) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, new Locale("hi", "IN"));
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    String village = addr.getSubLocality();
                    if (village == null || village.trim().isEmpty()) village = addr.getLocality();
                    if (village == null || village.trim().isEmpty()) village = addr.getFeatureName();

                    String district = addr.getSubAdminArea();
                    String finalAddress = (village != null ? village : "") + (district != null ? " (" + district + ")" : "");

                    new Handler(Looper.getMainLooper()).post(() -> {
                        edtVillage.setText(finalAddress.trim());
                        txtLocationStatus.setText("✅ Sateek gaon mil gaya");
                    });
                }
            } catch (Exception ignored) {}
        });
    }

    private void handleRegistration() {
        String name = edtFarmerName.getText().toString().trim();
        String village = edtVillage.getText().toString().trim();
        String phone = edtWhatsAppNumber.getText().toString().trim();
        String pin = edtPin.getText().toString().trim();

        if (name.isEmpty() || village.isEmpty() || phone.length() != 10 || pin.length() != 6) {
            Toast.makeText(this, "Kripya Naam, Gaon, 10-digit Phone aur 6-digit PIN bharein!", Toast.LENGTH_LONG).show();
            return;
        }

        loginProgressBar.setVisibility(View.VISIBLE);

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean("is_registered", true)
                .putString("farmer_name", name)
                .putString("farmer_phone", phone)
                .putString("village", village)
                .putString("secret_pin", pin)
                .putString("account_status", "approved")
                .putFloat("lat", (float) currentLat)
                .putFloat("lng", (float) currentLng)
                .apply();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL("https://mera-kisan-backend.vercel.app/api/add-crop");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("farmer_name", name);
                payload.put("farmer_phone", phone);
                payload.put("village", village);
                payload.put("lat", currentLat);
                payload.put("lng", currentLng);
                payload.put("account_status", "approved");

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.close();
                conn.getResponseCode();
            } catch (Exception ignored) {}

            new Handler(Looper.getMainLooper()).post(() -> {
                loginProgressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Khata safaltapoorvak ban gaya!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
        });
    }

    private void handlePinLogin() {
        String enteredPin = edtLoginPin.getText().toString().trim();
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedPin = prefs.getString("secret_pin", "");

        if (enteredPin.equals(savedPin)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Galat PIN! Sahi 6-ankon ka PIN daalein.", Toast.LENGTH_SHORT).show();
        }
    }
}
