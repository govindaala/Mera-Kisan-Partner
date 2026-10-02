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
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private CardView cardGoogleLogin, cardRegisterDetails, cardPinLogin;
    private TextView txtGoogleAccountName, txtLocationStatus, txtWelcomeBack;
    private EditText edtVillage, edtWhatsAppNumber, edtPin, edtLoginPin;
    private Button btnGoogleSignIn, btnDetectVillage, btnSubmitRegister, btnLoginWithPin;
    private ProgressBar loginProgressBar;

    private static final String PREF_NAME = "MeraKisanPartnerPrefs";
    private static final int LOCATION_PERMISSION_REQ = 201;

    private double currentLat = 24.12;
    private double currentLng = 75.58;
    private String detectedVillageName = "";
    private String googleAccountEmail = "farmer.demo@gmail.com";
    private String googleAccountName = "किसान साथी";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        cardGoogleLogin = findViewById(R.id.cardGoogleLogin);
        cardRegisterDetails = findViewById(R.id.cardRegisterDetails);
        cardPinLogin = findViewById(R.id.cardPinLogin);

        txtGoogleAccountName = findViewById(R.id.txtGoogleAccountName);
        txtLocationStatus = findViewById(R.id.txtLocationStatus);
        txtWelcomeBack = findViewById(R.id.txtWelcomeBack);

        edtVillage = findViewById(R.id.edtVillage);
        edtWhatsAppNumber = findViewById(R.id.edtWhatsAppNumber);
        edtPin = findViewById(R.id.edtPin);
        edtLoginPin = findViewById(R.id.edtLoginPin);

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        btnDetectVillage = findViewById(R.id.btnDetectVillage);
        btnSubmitRegister = findViewById(R.id.btnSubmitRegister);
        btnLoginWithPin = findViewById(R.id.btnLoginWithPin);
        loginProgressBar = findViewById(R.id.loginProgressBar);

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isRegistered = prefs.getBoolean("is_registered", false);

        if (isRegistered) {
            cardGoogleLogin.setVisibility(View.GONE);
            cardRegisterDetails.setVisibility(View.GONE);
            cardPinLogin.setVisibility(View.VISIBLE);
            txtWelcomeBack.setText("नमस्ते " + prefs.getString("farmer_name", "किसान साथी") + " जी (" + prefs.getString("village", "") + ")\nअपना 6-अंकों का पिन दर्ज करें:");
        } else {
            cardGoogleLogin.setVisibility(View.VISIBLE);
        }

        // 1. Google लॉगिन
        btnGoogleSignIn.setOnClickListener(v -> {
            cardGoogleLogin.setVisibility(View.GONE);
            cardRegisterDetails.setVisibility(View.VISIBLE);
            txtGoogleAccountName.setText("Google खाता: " + googleAccountEmail);
            requestAndFetchPreciseLocation();
        });

        // 2. ऑटो GPS बटन
        btnDetectVillage.setOnClickListener(v -> requestAndFetchPreciseLocation());

        // 3. नया रजिस्ट्रेशन सबमिट
        btnSubmitRegister.setOnClickListener(v -> handleRegistration());

        // 4. पिन लॉगिन
        btnLoginWithPin.setOnClickListener(v -> handlePinLogin());
    }

    private void requestAndFetchPreciseLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQ);
        } else {
            fetchFineLocationAndGeocode();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQ && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchFineLocationAndGeocode();
        } else {
            txtLocationStatus.setText("⚠️ GPS परमिशन नहीं मिली। कृपया हाथ से गाँव लिखें।");
        }
    }

    private void fetchFineLocationAndGeocode() {
        txtLocationStatus.setText("📍 GPS से गाँव खोजा जा रहा है...");
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
                resolveVillageFromCoords(currentLat, currentLng);
            } else {
                txtLocationStatus.setText("GPS सिग्नल का इंतज़ार है... (या हाथ से लिखें)");
            }
        } catch (SecurityException ignored) {}
    }

    // सबसे बारीक स्तर पर गाँव का नाम निकालने वाला लॉजिक
    private void resolveVillageFromCoords(double lat, double lng) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, new Locale("hi", "IN"));
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);

                    // प्राथमिकता क्रम: SubLocality (गाँव) -> Locality (पंचायत/कस्बा) -> FeatureName
                    String village = addr.getSubLocality();
                    if (village == null || village.trim().isEmpty()) {
                        village = addr.getLocality();
                    }
                    if (village == null || village.trim().isEmpty()) {
                        village = addr.getFeatureName();
                    }

                    String district = addr.getSubAdminArea(); // जिला/तहसील
                    String finalAddress = (village != null ? village : "") + (district != null ? " (" + district + ")" : "");
                    detectedVillageName = finalAddress.trim();

                    new Handler(Looper.getMainLooper()).post(() -> {
                        edtVillage.setText(detectedVillageName);
                        txtLocationStatus.setText("✅ सटीक लोकेशन मिल गई: " + String.format("%.4f", lat) + ", " + String.format("%.4f", lng));
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() ->
                            txtLocationStatus.setText("गाँव का नाम नहीं मिला, कृपया स्वयं लिखें।"));
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        txtLocationStatus.setText("इंटरनेट धीमा है, कृपया स्वयं गाँव लिखें।"));
            }
        });
    }

    private void handleRegistration() {
        String village = edtVillage.getText().toString().trim();
        String phone = edtWhatsAppNumber.getText().toString().trim();
        String pin = edtPin.getText().toString().trim();

        if (village.isEmpty() || phone.length() != 10 || pin.length() != 6) {
            Toast.makeText(this, "कृपया गाँव, 10 अंकों का WhatsApp नंबर और 6-अंकों का PIN भरें!", Toast.LENGTH_LONG).show();
            return;
        }

        loginProgressBar.setVisibility(View.VISIBLE);

        // लोकल मेमोरी में सुरक्षित करें (Pending Status के साथ)
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean("is_registered", true)
                .putString("farmer_name", googleAccountName)
                .putString("farmer_phone", phone)
                .putString("village", village)
                .putString("secret_pin", pin)
                .putString("account_status", "pending")
                .putFloat("lat", (float) currentLat)
                .putFloat("lng", (float) currentLng)
                .apply();

        // बैकएंड पर नया किसान डेटा भेजना
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL("https://mera-kisan-backend.vercel.app/api/add-crop");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("farmer_name", googleAccountName);
                payload.put("farmer_phone", phone);
                payload.put("village", village);
                payload.put("lat", currentLat);
                payload.put("lng", currentLng);
                payload.put("account_status", "pending");

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.close();
                conn.getResponseCode();
            } catch (Exception ignored) {}

            new Handler(Looper.getMainLooper()).post(() -> {
                loginProgressBar.setVisibility(View.GONE);
                Toast.makeText(this, "खाता बन गया! एडमिन द्वारा कॉल सत्यापन प्रक्रिया जारी है।", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "गलत पिन! कृपया 6-अंकों का सही पिन डालें।", Toast.LENGTH_SHORT).show();
        }
    }
}
