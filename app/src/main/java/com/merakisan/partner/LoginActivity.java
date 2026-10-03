// File Path: app/src/main/java/com/merakisan/partner/LoginActivity.java
package com.merakisan.partner;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private CardView cardGoogleLogin, cardRegisterDetails, cardPinLogin, cardResetPinSection;
    private TextView txtGoogleAccountEmail, txtLocationStatus, txtWelcomeBack, btnForgotPin;
    private EditText edtFarmerName, edtVillage, edtWhatsAppNumber, edtPin, edtLoginPin, edtNewResetPin;
    private Button btnGoogleSignIn, btnDetectVillage, btnSubmitRegister, btnLoginWithPin, btnSaveNewPin;
    private ProgressBar loginProgressBar;

    private static final String PREF_NAME = "MeraKisanPartnerPrefs";
    private static final String ADMIN_WHATSAPP_NUMBER = "919876543210"; // आपका एडमिन हेल्पलाइन नंबर
    private static final int RC_GOOGLE_SIGN_IN = 9001;
    private static final int RC_GOOGLE_FORGOT_PIN = 9002;
    private static final int LOCATION_PERMISSION_REQ = 201;

    private GoogleSignInClient mGoogleSignInClient;
    private double currentLat = 24.12;
    private double currentLng = 75.58;

    // Google से प्राप्त जानकारी
    private String googleEmail = "";
    private String googlePhotoUrl = "";
    private String googleId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setupGoogleClient();

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isRegistered = prefs.getBoolean("is_registered", false);

        if (isRegistered) {
            showPinLoginCard(prefs.getString("farmer_name", "किसान"), prefs.getString("village", ""));
        } else {
            cardGoogleLogin.setVisibility(View.VISIBLE);
        }

        setupClickListeners();
    }

    private void initViews() {
        cardGoogleLogin = findViewById(R.id.cardGoogleLogin);
        cardRegisterDetails = findViewById(R.id.cardRegisterDetails);
        cardPinLogin = findViewById(R.id.cardPinLogin);
        cardResetPinSection = findViewById(R.id.cardResetPinSection);

        txtGoogleAccountEmail = findViewById(R.id.txtGoogleAccountEmail);
        txtLocationStatus = findViewById(R.id.txtLocationStatus);
        txtWelcomeBack = findViewById(R.id.txtWelcomeBack);
        btnForgotPin = findViewById(R.id.btnForgotPin);

        edtFarmerName = findViewById(R.id.edtFarmerName);
        edtVillage = findViewById(R.id.edtVillage);
        edtWhatsAppNumber = findViewById(R.id.edtWhatsAppNumber);
        edtPin = findViewById(R.id.edtPin);
        edtLoginPin = findViewById(R.id.edtLoginPin);
        edtNewResetPin = findViewById(R.id.edtNewResetPin);

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        btnDetectVillage = findViewById(R.id.btnDetectVillage);
        btnSubmitRegister = findViewById(R.id.btnSubmitRegister);
        btnLoginWithPin = findViewById(R.id.btnLoginWithPin);
        btnSaveNewPin = findViewById(R.id.btnSaveNewPin);
        loginProgressBar = findViewById(R.id.loginProgressBar);
    }

    private void setupGoogleClient() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void showPinLoginCard(String name, String village) {
        cardGoogleLogin.setVisibility(View.GONE);
        cardRegisterDetails.setVisibility(View.GONE);
        cardResetPinSection.setVisibility(View.GONE);
        cardPinLogin.setVisibility(View.VISIBLE);
        txtWelcomeBack.setText("नमस्ते " + name + " जी" + (village.isEmpty() ? "" : " (" + village + ")") + "\nअपना 6-अंकों का गुप्त PIN डालें:");
    }

    private void setupClickListeners() {
        btnGoogleSignIn.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
        });

        btnDetectVillage.setOnClickListener(v -> requestLocation());
        btnSubmitRegister.setOnClickListener(v -> handleRegistration());
        btnLoginWithPin.setOnClickListener(v -> handlePinLogin());

        // पिन भूल गए पर दोहरे विकल्प: Google Re-auth या WhatsApp Admin
        btnForgotPin.setOnClickListener(v -> showForgotPinOptionsDialog());

        btnSaveNewPin.setOnClickListener(v -> handleResetPinSubmit());
    }

    private void showForgotPinOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔑 PIN रीसेट विकल्प");
        builder.setMessage("आप अपना सुरक्षा पिन कैसे रीसेट करना चाहते हैं?");
        
        builder.setPositiveButton("G  Google से रीसेट करें", (dialog, which) -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_GOOGLE_FORGOT_PIN);
        });

        builder.setNegativeButton("💬 एडमिन WhatsApp द्वारा", (dialog, which) -> {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String phone = prefs.getString("farmer_phone", "");
            String name = prefs.getString("farmer_name", "किसान साथी");
            String msg = "नमस्ते एडमिन, मैं " + name + " (मोबाइल: " + phone + ") अपना 6-अंकों का पिन भूल गया हूँ। कृपया एडमिन पैनल से मेरा पिन रीसेट कर दें।";
            try {
                Intent waIntent = new Intent(Intent.ACTION_VIEW);
                waIntent.setData(Uri.parse("https://wa.me/" + ADMIN_WHATSAPP_NUMBER + "?text=" + URLEncoder.encode(msg, "UTF-8")));
                startActivity(waIntent);
            } catch (Exception e) {
                Toast.makeText(this, "WhatsApp नहीं खुल सका", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNeutralButton("रद्द करें", null);
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 1. पहला Google साइन-इन
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    captureGoogleData(account);
                    checkIfFarmerAlreadyRegistered(googleEmail, googleId, account.getDisplayName());
                }
            } catch (ApiException e) {
                // ऑफ़लाइन या बिना Play Services फ़ॉलबैक
                cardGoogleLogin.setVisibility(View.GONE);
                cardRegisterDetails.setVisibility(View.VISIBLE);
                requestLocation();
            }
        }

        // 2. पिन भूल जाने पर Google सत्यापन
        if (requestCode == RC_GOOGLE_FORGOT_PIN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    Toast.makeText(this, "पहचान सत्यापित! नया PIN दर्ज करें।", Toast.LENGTH_SHORT).show();
                    cardPinLogin.setVisibility(View.GONE);
                    cardResetPinSection.setVisibility(View.VISIBLE);
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google सत्यापन असफल रहा।", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void captureGoogleData(GoogleSignInAccount account) {
        googleEmail = account.getEmail() != null ? account.getEmail() : "";
        googleId = account.getId() != null ? account.getId() : "";
        googlePhotoUrl = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "";

        txtGoogleAccountEmail.setText("Google: " + googleEmail);
        String name = account.getDisplayName();
        if (name != null && !name.isEmpty()) {
            edtFarmerName.setText(name);
        }
    }

    // नया फ़ोन / री-इंस्टॉल होने पर बैकएंड से पुराने किसान की पहचान करना
    private void checkIfFarmerAlreadyRegistered(String email, String gid, String displayName) {
        loginProgressBar.setVisibility(View.VISIBLE);

        Executors.newSingleThreadExecutor().execute(() -> {
            boolean exists = false;
            JSONObject farmerData = null;

            try {
                URL url = new URL("https://mera-kisan-backend.vercel.app/api/admin?action=check_farmer_by_google&google_email=" +
                        URLEncoder.encode(email, "UTF-8") + "&google_id=" + URLEncoder.encode(gid, "UTF-8"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();

                    JSONObject res = new JSONObject(sb.toString());
                    if (res.optBoolean("exists", false)) {
                        exists = true;
                        farmerData = res.optJSONObject("farmer");
                    }
                }
            } catch (Exception ignored) {}

            final boolean farmerExists = exists;
            final JSONObject finalFarmer = farmerData;

            new Handler(Looper.getMainLooper()).post(() -> {
                loginProgressBar.setVisibility(View.GONE);

                if (farmerExists && finalFarmer != null) {
                    // पुराना किसान मिल गया! स्थानीय मेमोरी में लोड करें
                    SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                    prefs.edit()
                            .putBoolean("is_registered", true)
                            .putString("farmer_name", finalFarmer.optString("name", displayName))
                            .putString("farmer_phone", finalFarmer.optString("phone", ""))
                            .putString("village", finalFarmer.optString("village", ""))
                            .putString("secret_pin", finalFarmer.optString("secret_pin", ""))
                            .putString("google_email", email)
                            .putString("google_photo_url", finalFarmer.optString("google_photo_url", googlePhotoUrl))
                            .apply();

                    Toast.makeText(LoginActivity.this, "वापसी पर स्वागत है! कृपया अपना पिन दर्ज करें।", Toast.LENGTH_SHORT).show();
                    showPinLoginCard(finalFarmer.optString("name", displayName), finalFarmer.optString("village", ""));
                } else {
                    // पहली बार आया नया किसान
                    cardGoogleLogin.setVisibility(View.GONE);
                    cardRegisterDetails.setVisibility(View.VISIBLE);
                    requestLocation();
                }
            });
        });
    }

    private void handleResetPinSubmit() {
        String newPin = edtNewResetPin.getText().toString().trim();
        if (newPin.length() != 6) {
            Toast.makeText(this, "कृपया ठीक 6 अंकों का नया PIN दर्ज करें!", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String phone = prefs.getString("farmer_phone", "");

        loginProgressBar.setVisibility(View.VISIBLE);
        prefs.edit().putString("secret_pin", newPin).apply();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL("https://mera-kisan-backend.vercel.app/api/admin");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("action", "user_reset_pin");
                payload.put("phone", phone);
                payload.put("new_pin", newPin);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.close();
                conn.getResponseCode();
            } catch (Exception ignored) {}

            new Handler(Looper.getMainLooper()).post(() -> {
                loginProgressBar.setVisibility(View.GONE);
                Toast.makeText(this, "नया PIN सेट हो गया!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
        });
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
                resolveVillage(currentLat, currentLng);
            } else {
                txtLocationStatus.setText("गाँव का नाम सीधे टाइप भी कर सकते हैं");
            }
        } catch (SecurityException ignored) {
            txtLocationStatus.setText("गाँव का नाम सीधे टाइप करें");
        }
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
                        txtLocationStatus.setText("✅ सटीक गाँव मिल गया");
                    });
                }
            } catch (Exception ignored) {}
        });
    }

    // नया किसान पंजीकरण (Google डेटा सहित बैकएंड में भेजना)
    private void handleRegistration() {
        String name = edtFarmerName.getText().toString().trim();
        String village = edtVillage.getText().toString().trim();
        String phone = edtWhatsAppNumber.getText().toString().trim();
        String pin = edtPin.getText().toString().trim();

        if (name.isEmpty() || village.isEmpty() || phone.length() != 10 || pin.length() != 6) {
            Toast.makeText(this, "कृपया नाम, गाँव, 10-अंकों का फ़ोन और 6-अंकों का PIN भरें!", Toast.LENGTH_LONG).show();
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
                .putString("google_email", googleEmail)
                .putString("google_photo_url", googlePhotoUrl)
                .putString("google_id", googleId)
                .putString("account_status", "approved")
                .putFloat("lat", (float) currentLat)
                .putFloat("lng", (float) currentLng)
                .apply();

        // बैकएंड में पूरा Google डेटा भेजना
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL("https://mera-kisan-backend.vercel.app/api/admin");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("action", "register_farmer");
                payload.put("name", name);
                payload.put("phone", phone);
                payload.put("village", village);
                payload.put("secret_pin", pin);
                payload.put("google_email", googleEmail);
                payload.put("google_photo_url", googlePhotoUrl);
                payload.put("google_id", googleId);
                payload.put("lat", currentLat);
                payload.put("lng", currentLng);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.close();
                conn.getResponseCode();
            } catch (Exception ignored) {}

            new Handler(Looper.getMainLooper()).post(() -> {
                loginProgressBar.setVisibility(View.GONE);
                Toast.makeText(this, "🎉 खाता बन गया! Mera Kisan में स्वागत है।", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "गलत PIN! सही 6-अंकों का PIN डालें।", Toast.LENGTH_SHORT).show();
        }
    }
}
