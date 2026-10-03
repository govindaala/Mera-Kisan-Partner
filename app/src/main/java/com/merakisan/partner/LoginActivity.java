package com.merakisan.partner;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
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

    private static final String PREF_NAME = "MeraKisanPartnerPrefs";
    private static final String ADMIN_WHATSAPP_NUMBER = "918871291126";
    private static final int RC_GOOGLE_SIGN_UP = 9001;
    private static final int RC_GOOGLE_FORGOT_PIN = 9002;
    private static final int LOCATION_PERMISSION_REQ = 201;

    // Tab buttons & Containers
    private Button btnTabSwitchLogin, btnTabSwitchSignup;
    private CardView cardDirectLogin, cardRegister, cardResetPin;
    private ProgressBar loginProgressBar;

    // Direct Login Views
    private EditText edtDirectLoginPhone, edtDirectLoginPin;
    private Button btnSubmitDirectLogin;
    private TextView btnDirectForgotPin;

    // Signup Views
    private Button btnGoogleSignUp, btnGpsDetectVillage, btnSubmitRegister;
    private EditText edtRegName, edtRegVillage, edtRegPhone, edtRegPin;
    private TextView txtGoogleStatus;

    // PIN Reset Views
    private EditText edtNewResetPin;
    private Button btnSaveNewPin;

    private GoogleSignInClient mGoogleSignInClient;
    private double currentLat = 24.12;
    private double currentLng = 75.58;

    private String googleEmail = "";
    private String googlePhotoUrl = "";
    private String googleId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Agar kisan pehle se login hai toh seedhe MainActivity kholein
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean("is_registered", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);
        initViews();
        setupGoogleClient();
        setupTabSwitching();
        setupActions();
    }

    private void initViews() {
        btnTabSwitchLogin = findViewById(R.id.btnTabSwitchLogin);
        btnTabSwitchSignup = findViewById(R.id.btnTabSwitchSignup);
        cardDirectLogin = findViewById(R.id.cardDirectLogin);
        cardRegister = findViewById(R.id.cardRegister);
        cardResetPin = findViewById(R.id.cardResetPin);
        loginProgressBar = findViewById(R.id.loginProgressBar);

        edtDirectLoginPhone = findViewById(R.id.edtDirectLoginPhone);
        edtDirectLoginPin = findViewById(R.id.edtDirectLoginPin);
        btnSubmitDirectLogin = findViewById(R.id.btnSubmitDirectLogin);
        btnDirectForgotPin = findViewById(R.id.btnDirectForgotPin);

        btnGoogleSignUp = findViewById(R.id.btnGoogleSignUp);
        btnGpsDetectVillage = findViewById(R.id.btnGpsDetectVillage);
        btnSubmitRegister = findViewById(R.id.btnSubmitRegister);
        edtRegName = findViewById(R.id.edtRegName);
        edtRegVillage = findViewById(R.id.edtRegVillage);
        edtRegPhone = findViewById(R.id.edtRegPhone);
        edtRegPin = findViewById(R.id.edtRegPin);
        txtGoogleStatus = findViewById(R.id.txtGoogleStatus);

        edtNewResetPin = findViewById(R.id.edtNewResetPin);
        btnSaveNewPin = findViewById(R.id.btnSaveNewPin);
    }

    private void setupGoogleClient() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupTabSwitching() {
        btnTabSwitchLogin.setOnClickListener(v -> switchTab(true));
        btnTabSwitchSignup.setOnClickListener(v -> switchTab(false));
    }

    private void switchTab(boolean showLogin) {
        if (showLogin) {
            btnTabSwitchLogin.setBackgroundColor(Color.parseColor("#166534"));
            btnTabSwitchLogin.setTextColor(Color.WHITE);
            btnTabSwitchSignup.setBackgroundColor(Color.TRANSPARENT);
            btnTabSwitchSignup.setTextColor(Color.parseColor("#475569"));
            cardDirectLogin.setVisibility(View.VISIBLE);
            cardRegister.setVisibility(View.GONE);
            cardResetPin.setVisibility(View.GONE);
        } else {
            btnTabSwitchSignup.setBackgroundColor(Color.parseColor("#166534"));
            btnTabSwitchSignup.setTextColor(Color.WHITE);
            btnTabSwitchLogin.setBackgroundColor(Color.TRANSPARENT);
            btnTabSwitchLogin.setTextColor(Color.parseColor("#475569"));
            cardRegister.setVisibility(View.VISIBLE);
            cardDirectLogin.setVisibility(View.GONE);
            cardResetPin.setVisibility(View.GONE);
        }
    }

    private void setupActions() {
        btnSubmitDirectLogin.setOnClickListener(v -> handleDirectLogin());
        btnDirectForgotPin.setOnClickListener(v -> showForgotPinDialog());

        btnGoogleSignUp.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_UP);
        });

        btnGpsDetectVillage.setOnClickListener(v -> requestLocation());
        btnSubmitRegister.setOnClickListener(v -> handleRegistration());
        btnSaveNewPin.setOnClickListener(v -> handleSaveResetPin());
    }

    // ================= 1. Seedhe Phone + 6-digit PIN se Login =================
    private void handleDirectLogin() {
        String phone = edtDirectLoginPhone.getText().toString().trim();
        String pin = edtDirectLoginPin.getText().toString().trim();

        if (phone.length() != 10 || pin.length() != 6) {
            Toast.makeText(this, "Kripya 10-ankon ka phone number aur 6-ankon ka PIN darj karein!", Toast.LENGTH_SHORT).show();
            return;
        }

        loginProgressBar.setVisibility(View.VISIBLE);

        Executors.newSingleThreadExecutor().execute(() -> {
            boolean success = false;
            String errorMsg = "Login asafal raha";
            JSONObject farmerObj = null;

            try {
                URL url = new URL("https://mera-kisan-backend.vercel.app/api/admin");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("action", "verify_farmer_login");
                payload.put("phone", phone);
                payload.put("pin", pin);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.close();

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(new InputStreamReader(code == 200 ? conn.getInputStream() : conn.getErrorStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject res = new JSONObject(sb.toString());
                if (res.optBoolean("success", false)) {
                    success = true;
                    farmerObj = res.optJSONObject("farmer");
                } else {
                    errorMsg = res.optString("error", errorMsg);
                }
            } catch (Exception e) {
                errorMsg = "Server se connect nahi ho saka: " + e.getMessage();
            }

            final boolean isOk = success;
            final String msg = errorMsg;
            final JSONObject f = farmerObj;

            new Handler(Looper.getMainLooper()).post(() -> {
                loginProgressBar.setVisibility(View.GONE);
                if (isOk && f != null) {
                    SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                    prefs.edit()
                            .putBoolean("is_registered", true)
                            .putString("farmer_name", f.optString("name", "Kisan Sathi"))
                            .putString("farmer_phone", phone)
                            .putString("village", f.optString("village", ""))
                            .putString("secret_pin", pin)
                            .putString("farmer_upi", f.optString("farmer_upi", ""))
                            .putString("land_acres", f.optString("land_acres", ""))
                            .putString("organic_cert", f.optString("organic_cert", ""))
                            .putString("google_email", f.optString("google_email", ""))
                            .putString("google_photo_url", f.optString("google_photo_url", ""))
                            .apply();

                    Toast.makeText(this, "Swagat hai " + f.optString("name") + " ji!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    // ================= 2. Naya Khata Registration =================
    private void handleRegistration() {
        String name = edtRegName.getText().toString().trim();
        String village = edtRegVillage.getText().toString().trim();
        String phone = edtRegPhone.getText().toString().trim();
        String pin = edtRegPin.getText().toString().trim();

        if (name.isEmpty() || village.isEmpty() || phone.length() != 10 || pin.length() != 6) {
            Toast.makeText(this, "Kripya naam, gaon, 10-ankon ka phone aur 6-ankon ka PIN bharein!", Toast.LENGTH_SHORT).show();
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
                .putFloat("lat", (float) currentLat)
                .putFloat("lng", (float) currentLng)
                .apply();

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
                Toast.makeText(this, "Naya khata ban gaya!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
        });
    }

    // ================= 3. Forgot PIN Dialog =================
    private void showForgotPinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("PIN Reset Vikalp");
        builder.setMessage("Aap apna gupt PIN kaise reset karna chahte hain?");

        builder.setPositiveButton("Google se Reset Karein", (dialog, which) -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_GOOGLE_FORGOT_PIN);
        });

        builder.setNegativeButton("WhatsApp Support", (dialog, which) -> {
            String phone = edtDirectLoginPhone.getText().toString().trim();
            String msg = "Namaste Admin, main Mera Kisan ka kisan sathi hoon (Mobile: " + (phone.isEmpty() ? "Mera Number" : phone) + "). Main apna 6-digit PIN bhool gaya hoon, kripya reset kar dein.";
            try {
                Intent wa = new Intent(Intent.ACTION_VIEW);
                wa.setData(Uri.parse("https://wa.me/" + ADMIN_WHATSAPP_NUMBER + "?text=" + URLEncoder.encode(msg, "UTF-8")));
                startActivity(wa);
            } catch (Exception e) {
                Toast.makeText(this, "WhatsApp open nahi ho saka", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNeutralButton("Radd Karein", null);
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_SIGN_UP) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount acc = task.getResult(ApiException.class);
                if (acc != null) {
                    googleEmail = acc.getEmail() != null ? acc.getEmail() : "";
                    googlePhotoUrl = acc.getPhotoUrl() != null ? acc.getPhotoUrl().toString() : "";
                    googleId = acc.getId() != null ? acc.getId() : "";
                    if (acc.getDisplayName() != null) edtRegName.setText(acc.getDisplayName());
                    txtGoogleStatus.setText("Google khata link ho gaya: " + googleEmail);
                    requestLocation();
                }
            } catch (ApiException e) {
                requestLocation();
            }
        }

        if (requestCode == RC_GOOGLE_FORGOT_PIN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount acc = task.getResult(ApiException.class);
                if (acc != null) {
                    cardDirectLogin.setVisibility(View.GONE);
                    cardRegister.setVisibility(View.GONE);
                    cardResetPin.setVisibility(View.VISIBLE);
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google verification asafal raha", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ================= 4. PIN Reset Save =================
    private void handleSaveResetPin() {
        String newPin = edtNewResetPin.getText().toString().trim();
        String phone = edtDirectLoginPhone.getText().toString().trim();
        if (newPin.length() != 6 || phone.length() != 10) {
            Toast.makeText(this, "Kripya 10-ankon ka phone number aur theek 6 ankon ka PIN bharein!", Toast.LENGTH_SHORT).show();
            return;
        }

        loginProgressBar.setVisibility(View.VISIBLE);
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
                Toast.makeText(this, "Naya PIN set ho gaya! Ab login karein.", Toast.LENGTH_SHORT).show();
                edtDirectLoginPin.setText(newPin);
                switchTab(true);
            });
        });
    }

    // ================= 5. GPS Location & Village Auto-fill =================
    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQ);
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
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return;
        try {
            Location loc = null;
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc != null) {
                currentLat = loc.getLatitude();
                currentLng = loc.getLongitude();
                resolveAddress(currentLat, currentLng);
            }
        } catch (SecurityException ignored) {}
    }

    private void resolveAddress(double lat, double lng) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Geocoder g = new Geocoder(this, new Locale("hi", "IN"));
                List<Address> list = g.getFromLocation(lat, lng, 1);
                if (list != null && !list.isEmpty()) {
                    String v = list.get(0).getSubLocality();
                    if (v == null) v = list.get(0).getLocality();
                    final String res = (v != null ? v : "बर्दि‍या अमरा");
                    new Handler(Looper.getMainLooper()).post(() -> edtRegVillage.setText(res));
                }
            } catch (Exception ignored) {}
        });
    }
}
