// File Path: app/src/main/java/com/merakisan/partner/MainActivity.java
package com.merakisan.partner;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String PREF_NAME = "MeraKisanPartnerPrefs";

    // 4 मुख्य नेविगेशन टैब बटन्स
    private Button btnNavOverview, btnNavCrops, btnNavAddCrop, btnNavProfile;
    private View viewOverview, viewCrops, viewAddCrop, viewProfile;

    // ओवरव्यू स्क्रीन व्यूज
    private TextView txtWelcomeFarmer, txtActiveCropsCount, txtProfileCompletePercent;
    private ProgressBar progressProfileCompletion;

    // मंडी फ़सलें सूची
    private LinearLayout containerFarmerCrops;
    private ProgressBar cropsProgressBar;

    // नई फ़सल/उत्पाद जोड़ने का फ़ॉर्म
    private EditText edtCropName, edtCropPrice, edtCropStock, edtCropDesc;
    private Spinner spnCategory;
    private CheckBox chkIsOrganic;
    private Button btnSubmitCrop;
    private ProgressBar addCropProgressBar;

    // प्रोफ़ाइल व्यूज
    private EditText edtProfileLand, edtProfileUpi, edtProfileCert;
    private Button btnSaveProfile, btnLogout;

    // सहायता व विवाद टिकट
    private Button btnHelpDispute;

    // भविष्य का बूस्ट अर्निंग खाका (डिफ़ॉल्ट बंद - 100% मुफ़्त ऐप)
    private boolean isBoostSystemActive = false;
    private int boostPrice = 99;
    private int boostDays = 3;

    // 7 प्रामाणिक श्रेणियाँ
    private final String[] CATEGORIES = {
            "🌾 अनाज, दालें व मिलेट्स",
            "🛢️ कच्ची घानी शुद्ध तेल",
            "🌶️ शुद्ध देशी मसाले",
            "🍯 देशी गुड़ व प्राकृतिक मीठा",
            "🥬 सूखी सब्जियाँ व पारंपरिक बड़ियाँ",
            "🥛 A2 बिलौना घी व डेयरी उत्पाद",
            "🥦 ताज़ा फल व मौसमी सब्जियाँ"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupTabs();
        setupCategorySpinner();
        loadFarmerProfileData();
        checkSystemConfig();
        loadMyCrops();

        btnSubmitCrop.setOnClickListener(v -> submitNewCrop());
        btnSaveProfile.setOnClickListener(v -> saveFarmerProfile());
        btnLogout.setOnClickListener(v -> handleLogout());
        btnHelpDispute.setOnClickListener(v -> showDisputeDialog());
    }

    private void initViews() {
        btnNavOverview = findViewById(R.id.btnNavOverview);
        btnNavCrops = findViewById(R.id.btnNavCrops);
        btnNavAddCrop = findViewById(R.id.btnNavAddCrop);
        btnNavProfile = findViewById(R.id.btnNavProfile);

        viewOverview = findViewById(R.id.viewOverview);
        viewCrops = findViewById(R.id.viewCrops);
        viewAddCrop = findViewById(R.id.viewAddCrop);
        viewProfile = findViewById(R.id.viewProfile);

        txtWelcomeFarmer = findViewById(R.id.txtWelcomeFarmer);
        txtActiveCropsCount = findViewById(R.id.txtActiveCropsCount);
        txtProfileCompletePercent = findViewById(R.id.txtProfileCompletePercent);
        progressProfileCompletion = findViewById(R.id.progressProfileCompletion);

        containerFarmerCrops = findViewById(R.id.containerFarmerCrops);
        cropsProgressBar = findViewById(R.id.cropsProgressBar);

        edtCropName = findViewById(R.id.edtCropName);
        edtCropPrice = findViewById(R.id.edtCropPrice);
        edtCropStock = findViewById(R.id.edtCropStock);
        edtCropDesc = findViewById(R.id.edtCropDesc);
        spnCategory = findViewById(R.id.spnCategory);
        chkIsOrganic = findViewById(R.id.chkIsOrganic);
        btnSubmitCrop = findViewById(R.id.btnSubmitCrop);
        addCropProgressBar = findViewById(R.id.addCropProgressBar);

        edtProfileLand = findViewById(R.id.edtProfileLand);
        edtProfileUpi = findViewById(R.id.edtProfileUpi);
        edtProfileCert = findViewById(R.id.edtProfileCert);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnLogout = findViewById(R.id.btnLogout);
        btnHelpDispute = findViewById(R.id.btnHelpDispute);
    }

    private void setupTabs() {
        btnNavOverview.setOnClickListener(v -> switchTab(viewOverview, btnNavOverview));
        btnNavCrops.setOnClickListener(v -> {
            switchTab(viewCrops, btnNavCrops);
            loadMyCrops();
        });
        btnNavAddCrop.setOnClickListener(v -> switchTab(viewAddCrop, btnNavAddCrop));
        btnNavProfile.setOnClickListener(v -> switchTab(viewProfile, btnNavProfile));
    }

    private void switchTab(View activeView, Button activeBtn) {
        viewOverview.setVisibility(View.GONE);
        viewCrops.setVisibility(View.GONE);
        viewAddCrop.setVisibility(View.GONE);
        viewProfile.setVisibility(View.GONE);
        activeView.setVisibility(View.VISIBLE);

        Button[] btns = {btnNavOverview, btnNavCrops, btnNavAddCrop, btnNavProfile};
        for (Button b : btns) {
            b.setBackgroundColor(Color.parseColor("#14532D"));
            b.setTextColor(Color.parseColor("#BBF7D0"));
        }
        activeBtn.setBackgroundColor(Color.parseColor("#22C55E"));
        activeBtn.setTextColor(Color.parseColor("#052E16"));
    }

    private void setupCategorySpinner() {
        if (spnCategory != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, CATEGORIES);
            spnCategory.setAdapter(adapter);
        }
    }

    private void loadFarmerProfileData() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String name = prefs.getString("farmer_name", "किसान साथी");
        String village = prefs.getString("village", "बर्दि‍या अमरा");
        String land = prefs.getString("land_acres", "");
        String upi = prefs.getString("farmer_upi", "");
        String cert = prefs.getString("organic_cert", "");

        txtWelcomeFarmer.setText("नमस्ते, " + name + " जी!\n📍 गाँव: " + village);
        edtProfileLand.setText(land);
        edtProfileUpi.setText(upi);
        edtProfileCert.setText(cert);

        int score = 40;
        if (!land.isEmpty()) score += 20;
        if (!upi.isEmpty()) score += 20;
        if (!cert.isEmpty()) score += 20;

        txtProfileCompletePercent.setText("प्रोफ़ाइल पूर्णता: " + score + "% " + (score == 100 ? "⭐ गोल्ड पार्टनर बैज" : ""));
        progressProfileCompletion.setProgress(score);
    }

    private void checkSystemConfig() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL("https://mera-kisan-backend.vercel.app/api/admin?action=get_public_config");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();

                    JSONObject res = new JSONObject(sb.toString());
                    JSONObject bf = res.optJSONObject("boost_feature");
                    if (bf != null) {
                        isBoostSystemActive = bf.optBoolean("is_active", false);
                        boostPrice = bf.optInt("price", 99);
                        boostDays = bf.optInt("days", 3);
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    private boolean isMatchingPhone(String p1, String p2) {
        if (p1 == null || p2 == null) return false;
        String s1 = p1.replaceAll("\\D+", "");
        String s2 = p2.replaceAll("\\D+", "");
        if (s1.isEmpty() || s2.isEmpty()) return true;
        if (s1.length() >= 10) s1 = s1.substring(s1.length() - 10);
        if (s2.length() >= 10) s2 = s2.substring(s2.length() - 10);
        return s1.equals(s2);
    }

    private void loadMyCrops() {
        cropsProgressBar.setVisibility(View.VISIBLE);
        containerFarmerCrops.removeAllViews();

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String myPhone = prefs.getString("farmer_phone", prefs.getString("phone", "8871291126"));

        Executors.newSingleThreadExecutor().execute(() -> {
            List<JSONObject> myList = new ArrayList<>();
            try {
                URL url = new URL("https://mera-kisan-backend.vercel.app/api/crops");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();

                    JSONObject res = new JSONObject(sb.toString());
                    JSONArray arr = res.optJSONArray("crops");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject c = arr.getJSONObject(i);
                            String cPhone = c.optString("farmer_phone", "");
                            if (isMatchingPhone(myPhone, cPhone)) {
                                myList.add(c);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            new Handler(Looper.getMainLooper()).post(() -> {
                cropsProgressBar.setVisibility(View.GONE);
                txtActiveCropsCount.setText(String.valueOf(myList.size()));
                for (JSONObject crop : myList) {
                    addFarmerCropCard(crop);
                }
            });
        });
    }

    private void addFarmerCropCard(JSONObject crop) {
        String name = crop.optString("crop_name", "");
        double price = crop.optDouble("price_per_kg", 0);
        double stock = crop.optDouble("stock_qty_kg", 0);
        String category = crop.optString("category", "🌾 अनाज, दालें व मिलेट्स");
        boolean isOrganic = "organic".equalsIgnoreCase(crop.optString("farming_type"));
        String desc = crop.optString("description", "");

        CardView card = new CardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 16);
        card.setLayoutParams(lp);
        card.setRadius(12);
        card.setCardElevation(3);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(16, 16, 16, 16);

        TextView title = new TextView(this);
        title.setText("🌾 " + name + (isOrganic ? " 🛡️ (100% जैविक)" : ""));
        title.setTextSize(16);
        title.setTextColor(Color.parseColor("#166534"));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        box.addView(title);

        TextView catView = new TextView(this);
        catView.setText("📂 श्रेणी: " + category);
        catView.setTextSize(12);
        catView.setTextColor(Color.parseColor("#0284C7"));
        box.addView(catView);

        // शुद्ध 'रुपये प्रति किलो' भाव (क्विंटल पूरी तरह हटाया गया)
        TextView details = new TextView(this);
        details.setText("💰 भाव: ₹" + (int)price + "/किलो  |  📦 उपलब्ध स्टॉक: " + (int)stock + " किलो");
        details.setTextSize(13);
        details.setTextColor(Color.parseColor("#334155"));
        details.setPadding(0, 4, 0, 6);
        box.addView(details);

        // 📝 फ़सल का विवरण (Description)
        if (!desc.isEmpty()) {
            TextView descView = new TextView(this);
            descView.setText("📝 विवरण: " + desc);
            descView.setTextSize(12);
            descView.setTextColor(Color.parseColor("#475569"));
            descView.setPadding(0, 0, 0, 8);
            box.addView(descView);
        }

        // 🎨 AI ग्राफ़िक पोस्टर शेयर बटन
        Button btnPoster = new Button(this);
        btnPoster.setText("🖼️ AI मंडी पोस्टर बनाएँ व शेयर करें (WhatsApp)");
        btnPoster.setBackgroundColor(Color.parseColor("#166534"));
        btnPoster.setTextColor(Color.WHITE);
        btnPoster.setTextSize(12);
        btnPoster.setOnClickListener(v -> generateAndSharePoster(name, price, stock, isOrganic, category, desc));
        box.addView(btnPoster);

        // 🔥 बूस्ट स्थिति
        boolean isPromoted = crop.optBoolean("is_promoted", false);
        int rank = crop.optInt("boost_priority", 1);
        String cropId = crop.optString("crop_id", crop.optString("id", ""));

        if (isPromoted) {
            TextView promotedBadge = new TextView(this);
            promotedBadge.setText("🔥 टॉप मंडी प्रमोटेड (रैंक #" + (rank <= 4 ? rank : "टॉप") + ")");
            promotedBadge.setTextSize(12);
            promotedBadge.setTextColor(Color.parseColor("#92400E"));
            promotedBadge.setBackgroundColor(Color.parseColor("#FEF3C7"));
            promotedBadge.setPadding(12, 6, 12, 6);
            promotedBadge.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            pLp.setMargins(0, 8, 0, 0);
            promotedBadge.setLayoutParams(pLp);
            box.addView(promotedBadge);
        } else if (isBoostSystemActive) {
            Button btnBoost = new Button(this);
            btnBoost.setText("🚀 फ़सल टॉप बूस्ट करें (₹" + boostPrice + " / " + boostDays + " दिन)");
            btnBoost.setBackgroundColor(Color.parseColor("#F59E0B"));
            btnBoost.setTextColor(Color.WHITE);
            btnBoost.setTextSize(12);
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            bLp.setMargins(0, 8, 0, 0);
            btnBoost.setLayoutParams(bLp);
            btnBoost.setOnClickListener(v -> handleBoostCrop(cropId, name));
            box.addView(btnBoost);
        }

        card.addView(box);
        containerFarmerCrops.addView(card);
    }

    private void submitNewCrop() {
        String name = edtCropName.getText().toString().trim();
        String pStr = edtCropPrice.getText().toString().trim();
        String sStr = edtCropStock.getText().toString().trim();
        String desc = edtCropDesc.getText().toString().trim();
        String cat = spnCategory != null ? spnCategory.getSelectedItem().toString() : CATEGORIES[0];
        boolean isOrg = chkIsOrganic.isChecked();

        if (name.isEmpty() || pStr.isEmpty() || sStr.isEmpty()) {
            Toast.makeText(this, "कृपया नाम, भाव और स्टॉक भरें!", Toast.LENGTH_SHORT).show();
            return;
        }

        addCropProgressBar.setVisibility(View.VISIBLE);
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String farmerName = prefs.getString("farmer_name", "किसान साथी");
        String farmerPhone = prefs.getString("farmer_phone", prefs.getString("phone", "8871291126"));
        String village = prefs.getString("village", "बर्दि‍या अमरा");
        double lat = prefs.getFloat("lat", 24.12f);
        double lng = prefs.getFloat("lng", 75.58f);

        Executors.newSingleThreadExecutor().execute(() -> {
            boolean success = false;
            try {
                URL url = new URL("https://mera-kisan-backend.vercel.app/api/admin");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(8000);
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("action", "add_crop");
                payload.put("farmer_phone", farmerPhone);
                payload.put("farmer_name", farmerName);
                payload.put("crop_name", name);
                payload.put("category", cat);
                payload.put("description", desc);
                payload.put("price_per_kg", Double.parseDouble(pStr));
                payload.put("stock_qty_kg", Double.parseDouble(sStr));
                payload.put("village", village);
                payload.put("lat", lat);
                payload.put("lng", lng);
                payload.put("farming_type", isOrg ? "organic" : "standard");

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.close();

                if (conn.getResponseCode() == 200) {
                    success = true;
                }
            } catch (Exception ignored) {}

            final boolean isOk = success;
            new Handler(Looper.getMainLooper()).post(() -> {
                addCropProgressBar.setVisibility(View.GONE);
                if (isOk) {
                    Toast.makeText(this, "✅ उत्पाद मंडी में लाइव हो गया!", Toast.LENGTH_SHORT).show();
                    edtCropName.setText("");
                    edtCropPrice.setText("");
                    edtCropStock.setText("");
                    edtCropDesc.setText("");
                    switchTab(viewCrops, btnNavCrops);
                    loadMyCrops();
                } else {
                    Toast.makeText(this, "फ़सल सेव नहीं हो सकी, इंटरनेट चेक करें", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void handleBoostCrop(String cropId, String cropName) {
        new AlertDialog.Builder(this)
                .setTitle("🚀 फ़सल बूस्ट अनुरोध")
                .setMessage("फ़सल: " + cropName + "\nशुल्क: ₹" + boostPrice + " (" + boostDays + " दिन के लिए)\n\nभुगतान सत्यापन के बाद यह मंडी में सबसे ऊपर रैंक में दिखेगी।")
                .setPositiveButton("आगे बढ़ें 💳", (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        try {
                            URL url = new URL("https://mera-kisan-backend.vercel.app/api/admin");
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("POST");
                            conn.setRequestProperty("Content-Type", "application/json");
                            conn.setDoOutput(true);

                            SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                            String phone = prefs.getString("farmer_phone", prefs.getString("phone", "8871291126"));

                            JSONObject payload = new JSONObject();
                            payload.put("action", "request_crop_boost");
                            payload.put("crop_id", cropId);
                            payload.put("farmer_phone", phone);

                            OutputStream os = conn.getOutputStream();
                            os.write(payload.toString().getBytes("UTF-8"));
                            os.close();
                            conn.getResponseCode();
                        } catch (Exception ignored) {}

                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(this, "⏳ बूस्ट अनुरोध दर्ज हो गया है।", Toast.LENGTH_LONG).show();
                            loadMyCrops();
                        });
                    });
                })
                .setNegativeButton("रद्द करें", null)
                .show();
    }

    private void saveFarmerProfile() {
        String land = edtProfileLand.getText().toString().trim();
        String upi = edtProfileUpi.getText().toString().trim();
        String cert = edtProfileCert.getText().toString().trim();

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String phone = prefs.getString("farmer_phone", prefs.getString("phone", "8871291126"));

        prefs.edit().putString("land_acres", land).putString("farmer_upi", upi).putString("organic_cert", cert).apply();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL("https://mera-kisan-backend.vercel.app/api/admin");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("action", "update_farmer_profile");
                payload.put("phone", phone);
                payload.put("land_acres", land);
                payload.put("farmer_upi", upi);
                payload.put("organic_cert", cert);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.close();
                conn.getResponseCode();
            } catch (Exception ignored) {}

            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(this, "💾 प्रोफ़ाइल अपडेट हो गई!", Toast.LENGTH_SHORT).show();
                loadFarmerProfileData();
            });
        });
    }

    private void handleLogout() {
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    // =========================================================================
    // 🎨 HIGH-DEFINITION AI पोस्टर जनरेटर (12s टाइमआउट + विवरण सपोर्ट)
    // =========================================================================
    private void generateAndSharePoster(String cropName, double price, double stock, boolean isOrganic, String category, String desc) {
        Toast.makeText(this, "🎨 '" + cropName + "' का HD बैकग्राउंड व पोस्टर तैयार हो रहा है (कृपया 5 सेकंड रुकें)...", Toast.LENGTH_LONG).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            Bitmap finalPoster = null;
            try {
                String searchKeyword = getSmartSearchKeyword(cropName, category, desc);

                String aiImageUrl = "https://image.pollinations.ai/prompt/cinematic%20photorealistic%20close%20up%20of%20" 
                        + URLEncoder.encode(searchKeyword, "UTF-8") 
                        + "%20golden%20lighting%20rural%20indian%20farm%20harvest%20hyperrealistic%204k?width=1080&height=1080&nologo=true";

                Bitmap bgBitmap = null;
                try {
                    URL url = new URL(aiImageUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(12000); // 12 सेकंड का टाइमआउट HD इमेज के लिए
                    conn.setReadTimeout(12000);
                    conn.connect();
                    if (conn.getResponseCode() == 200) {
                        bgBitmap = BitmapFactory.decodeStream(conn.getInputStream());
                    }
                } catch (Exception e) {
                    bgBitmap = null;
                }

                finalPoster = Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(finalPoster);

                if (bgBitmap != null) {
                    Bitmap scaledBg = Bitmap.createScaledBitmap(bgBitmap, 1080, 1080, true);
                    canvas.drawBitmap(scaledBg, 0, 0, null);
                } else {
                    canvas.drawColor(Color.parseColor("#064E3B"));
                }

                Paint paint = new Paint();
                paint.setAntiAlias(true);

                paint.setColor(Color.argb(175, 15, 23, 42));
                canvas.drawRect(0, 0, 1080, 1080, paint);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(12);
                paint.setColor(Color.parseColor("#EAB308"));
                canvas.drawRect(20, 20, 1060, 1060, paint);

                paint.setStrokeWidth(4);
                paint.setColor(Color.parseColor("#22C55E"));
                canvas.drawRect(34, 34, 1046, 1046, paint);

                paint.setStyle(Paint.Style.FILL);

                // हेडर बैनर
                paint.setColor(Color.parseColor("#15803D"));
                canvas.drawRoundRect(new RectF(50, 50, 1030, 170), 16, 16, paint);

                paint.setColor(Color.parseColor("#FEF08A"));
                paint.setTextSize(26);
                paint.setFakeBoldText(true);
                canvas.drawText("🌱 MERA KISAN DIRECT • सीधे खेत से शुद्ध पैदावार", 80, 95, paint);

                paint.setColor(Color.WHITE);
                paint.setTextSize(42);
                paint.setFakeBoldText(true);
                canvas.drawText("खेत से सीधी खरीद • 0% बिचौलिया दलाली", 80, 145, paint);

                // मुख्य उत्पाद कार्ड
                paint.setColor(Color.argb(245, 255, 255, 255));
                canvas.drawRoundRect(new RectF(50, 190, 1030, 440), 20, 20, paint);

                paint.setColor(Color.parseColor("#0F172A"));
                paint.setTextSize(48);
                paint.setFakeBoldText(true);
                canvas.drawText("🌾 " + cropName, 80, 255, paint);

                if (isOrganic) {
                    paint.setColor(Color.parseColor("#15803D"));
                    paint.setTextSize(24);
                    canvas.drawText("🛡️ 100% शुद्ध प्राकृतिक देशी खाद से तैयार जैविक उत्पाद", 80, 300, paint);
                } else {
                    paint.setColor(Color.parseColor("#B45309"));
                    paint.setTextSize(24);
                    canvas.drawText("⭐ सुपर प्रीमियम ग्रेड-1 • खेत का सीधा ताज़ा स्टॉक", 80, 300, paint);
                }

                // भाव व स्टॉक स्ट्रिप (शुद्ध प्रति किलो भाव)
                paint.setColor(Color.parseColor("#FEF3C7"));
                canvas.drawRoundRect(new RectF(80, 330, 1000, 415), 12, 12, paint);

                paint.setColor(Color.parseColor("#92400E"));
                paint.setTextSize(32);
                paint.setFakeBoldText(true);
                canvas.drawText("💰 भाव: ₹" + (int)price + " प्रति किलो  |  📦 कुल स्टॉक: " + (int)stock + " किलो", 100, 385, paint);

                // फ़सल विशेषताएँ
                paint.setColor(Color.argb(240, 241, 245, 249));
                canvas.drawRoundRect(new RectF(50, 460, 1030, 755), 20, 20, paint);

                paint.setColor(Color.parseColor("#1E293B"));
                paint.setTextSize(26);
                paint.setFakeBoldText(true);
                canvas.drawText("✨ उत्पाद विशेषताएँ व गुणवत्ता (Quality & Purity Guarantee):", 80, 505, paint);

                String[] benefits = generateMarketingHighlights(cropName, category, desc, isOrganic);
                paint.setColor(Color.parseColor("#334155"));
                paint.setTextSize(23);
                paint.setFakeBoldText(false);
                canvas.drawText("• " + benefits[0], 80, 555, paint);
                canvas.drawText("• " + benefits[1], 80, 605, paint);
                canvas.drawText("• " + benefits[2], 80, 655, paint);
                canvas.drawText("• " + benefits[3], 80, 705, paint);

                // किसान संपर्क कार्ड
                SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                String farmerName = prefs.getString("farmer_name", "किसान साथी");
                String village = prefs.getString("village", "बर्दि‍या अमरा (मंदसौर)");
                String phone = prefs.getString("farmer_phone", prefs.getString("phone", "8871291126"));

                paint.setColor(Color.parseColor("#064E3B"));
                canvas.drawRoundRect(new RectF(50, 775, 1030, 945), 20, 20, paint);

                paint.setColor(Color.parseColor("#BBF7D0"));
                paint.setTextSize(28);
                paint.setFakeBoldText(true);
                canvas.drawText("👨‍🌾 उत्पादक किसान: " + farmerName + "  |  📍 गाँव: " + village, 80, 830, paint);

                paint.setColor(Color.WHITE);
                paint.setTextSize(38);
                paint.setFakeBoldText(true);
                canvas.drawText("📞 सीधा कॉल / WhatsApp: +91 " + phone, 80, 900, paint);

                // फ़ुटर गारंटी
                paint.setColor(Color.parseColor("#D97706"));
                canvas.drawRoundRect(new RectF(50, 965, 1030, 1025), 12, 12, paint);

                paint.setColor(Color.WHITE);
                paint.setTextSize(22);
                paint.setFakeBoldText(true);
                canvas.drawText("🤝 खेत पर आकर खुद माल परखें और तौल कराएं • सीधा किसान से पक्का सौदा", 100, 1003, paint);

            } catch (Exception ignored) {}

            final Bitmap shareBmp = finalPoster;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (shareBmp != null) {
                    sharePosterToWhatsApp(shareBmp, cropName, price, stock, desc);
                } else {
                    Toast.makeText(this, "पोस्टर तैयार नहीं हो सका", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private String getSmartSearchKeyword(String name, String cat, String desc) {
        String combined = (name + " " + cat + " " + desc).toLowerCase();
        if (combined.contains("मिलेट") || combined.contains("चीना") || combined.contains("पोसो")) return "golden proso millet grains harvest bowl";
        if (combined.contains("लहसुन") || combined.contains("garlic")) return "fresh dried white garlic bulbs harvest";
        if (combined.contains("तेल") || cat.contains("तेल")) return "cold pressed mustard seed oil bottle traditional kohlu";
        if (combined.contains("गुड़") || cat.contains("गुड़")) return "traditional natural sugarcane jaggery block gur";
        if (combined.contains("मसाले") || combined.contains("धनिया") || combined.contains("मेथी")) return "whole dried coriander seeds fenugreek indian spices";
        if (combined.contains("गेहूँ") || combined.contains("wheat")) return "golden ripe wheat grains field harvest";
        if (combined.contains("सोयाबीन") || combined.contains("soyabean")) return "yellow soybean grains harvest bowl";
        if (combined.contains("चना") || combined.contains("दाल")) return "desi chickpea legumes harvest";
        if (combined.contains("घी") || combined.contains("डेयरी")) return "traditional earthen bilona pure desi ghee pot";
        return "pure natural indian agriculture farm harvest crop";
    }

    // =========================================================================
    // 🌟 बुद्धिमान कॉपीराइटिंग इंजन (किसान का विवरण + श्रेणी के वास्तविक लाभ)
    // =========================================================================
    private String[] generateMarketingHighlights(String name, String cat, String desc, boolean isOrganic) {
        String n = name.toLowerCase();
        
        // पॉइंट 1: हमेशा किसान द्वारा डाला गया विवरण प्राथमिकता पर रहेगा
        String customPoint = desc.trim().isEmpty() 
                ? "दाने की छँटाई: हाथ से साफ किया हुआ बोल्ड दाना, कंकड़-मिट्टी व कचरा रहित ए-ग्रेड माल।" 
                : "विवरण: " + desc.trim();

        if (n.contains("मिलेट") || n.contains("चीना") || n.contains("पोसो") || n.contains("कोदो") || n.contains("रागी")) {
            return new String[]{
                    customPoint,
                    "पोषण: 100% ग्लूटेन-फ्री (Gluten-Free), हाई फ़ाइबर और प्रोटीन से भरपूर सुपरफ़ूड।",
                    "स्वास्थ्य लाभ: शुगर (डायबिटीज) व कोलेस्ट्रॉल नियंत्रण में अत्यंत गुणकारी।",
                    "गारंटी: सीधे मंदसौर के खेत की शुद्ध देशी पैदावार, बिना किसी केमिकल पॉलिश के।"
            };
        } else if (n.contains("तेल") || cat.contains("तेल")) {
            return new String[]{
                    customPoint,
                    "प्रक्रिया: 100% शुद्ध लकड़ी/कच्ची घानी (Cold Pressed), बिना केमिकल, बिना मिलावट।",
                    "ताज़गी: बीजों का मूल प्राकृतिक स्वाद, तेज़ भीनी सुगंध व पौष्टिकता पूरी तरह सुरक्षित।",
                    "स्वास्थ्य लाभ: ज़ीरो कोलेस्ट्रॉल, दिल के लिए लाभकारी और बाज़ार के तेल से 10 गुना शुद्ध।"
            };
        } else if (n.contains("गुड़") || cat.contains("गुड़") || cat.contains("मीठा")) {
            return new String[]{
                    customPoint,
                    "उत्पादन: पारंपरिक देशी भट्टी पर पकाया गया, बिना हाइड्रो/केमिकल रंग का शुद्ध देशी गुड़।",
                    "पोषक तत्व: प्राकृतिक आयरन, कैल्शियम व मिनरल्स से परिपूर्ण देशी मिठास।",
                    "स्वाद: असली देशी गन्ने का सोंधा स्वाद, दूध व चाय में डालने पर फटने की कोई शिकायत नहीं।"
            };
        } else if (n.contains("लहसुन") || n.contains("garlic")) {
            return new String[]{
                    customPoint,
                    "क्वालिटी: ठोस, सफ़ेद कंद (देशी/G-2 किस्म), बड़े कड़क पर्दे और मोटे बोल्ड दाने।",
                    "सुगंध व रस: गाढ़ा औषधीय रस, तेज़ तीखापन व लंबे समय तक सुरक्षित रहने वाला माल।",
                    "सीधी खरीद: विश्वविख्यात मंदसौर लहसुन मंडी क्षेत्र के खेत से सीधी सप्लाई।"
            };
        } else if (n.contains("मसाले") || cat.contains("मसाले") || n.contains("धनिया") || n.contains("मेथी") || n.contains("मिर्च")) {
            return new String[]{
                    customPoint,
                    "गुणवत्ता: खेत से हाथ से चुने गए खड़े मसाले, बिना लकड़ी-डंठल या धूल के साफ़ ग्रेडिंग।",
                    "स्वाद व सुगंध: प्राकृतिक तेल और भीनी तेज़ सुगंध, पिसे बाज़ारी मसालों से दोगुना स्वाद।",
                    "शुद्धता: बिना किसी कृत्रिम रंग, स्टार्च या मिलावट के सीधे किसान के खलिहान से।"
            };
        } else if (n.contains("गेहूँ") || n.contains("wheat") || n.contains("शरबती")) {
            return new String[]{
                    customPoint,
                    "किस्म: सुनहरे चमकदार और वज़नदार बोल्ड दाने, बिना घुन या कीड़े के सूखा सुरक्षित माल।",
                    "रोटी की खूबी: रोटियां अत्यंत नरम, फूली हुई और प्राकृतिक देशी मिठास से भरपूर।",
                    "भंडारण: सालभर घर में स्टोर करने हेतु प्राकृतिक धूप में सुखाया हुआ सूखा सुरक्षित अनाज।"
            };
        } else {
            return new String[]{
                    customPoint,
                    "ग्रेडिंग: किसान द्वारा स्वयं तैयार किया हुआ उच्च गुणवत्ता वाला ताज़ा माल।",
                    "शुद्धता: " + (isOrganic ? "100% देशी गोबर खाद से तैयार प्रमाणित जैविक उपज।" : "खेत की सीधी ताज़ा उपज, बिना किसी कृत्रिम स्प्रे या मिलावट के।"),
                    "भरोसा: खेत पर स्वयं आकर माल चेक करने और अपनी मौजूदगी में तौल कराने की पूरी आज़ादी।"
            };
        }
    }

    private void sharePosterToWhatsApp(Bitmap bitmap, String cropName, double price, double stock, String desc) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String farmerName = prefs.getString("farmer_name", "किसान साथी");
        String village = prefs.getString("village", "बर्दि‍या अमरा");
        String phone = prefs.getString("farmer_phone", prefs.getString("phone", "8871291126"));

        try {
            String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "mandi_poster_" + System.currentTimeMillis(), "Mera Kisan Poster");
            Uri uri = Uri.parse(path);

            StringBuilder msg = new StringBuilder();
            msg.append("🌾 *Mera Kisan Direct - ताज़ा उपज उपलब्ध!*\n\n")
               .append("उत्पाद: *").append(cropName).append("*\n")
               .append("भाव: *₹").append((int)price).append("/किलो*\n")
               .append("उपलब्ध स्टॉक: *").append((int)stock).append(" किलो*\n");

            if (!desc.trim().isEmpty()) {
                msg.append("📝 विवरण: ").append(desc.trim()).append("\n");
            }

            msg.append("किसान: ").append(farmerName).append(" (").append(village).append(")\n")
               .append("📞 सीधा संपर्क करें: +91 ").append(phone).append("\n\n")
               .append("खेत से सीधी खरीद • 0% बिचौलिया दलाली!");

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/*");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.putExtra(Intent.EXTRA_TEXT, msg.toString());
            startActivity(Intent.createChooser(share, "मंडी पोस्टर WhatsApp पर शेयर करें"));
        } catch (Exception e) {
            Toast.makeText(this, "पोस्टर शेयर नहीं हो सका", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDisputeDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("📢 किसान सहायता व शिकायत डेस्क");
        final EditText input = new EditText(this);
        input.setHint("फ़सल, भुगतान या किसी समस्या के बारे में लिखें...");
        input.setMinLines(3);
        b.setView(input);

        b.setPositiveButton("भेजें ✉️", (d, w) -> {
            String msg = input.getText().toString().trim();
            if (!msg.isEmpty()) {
                SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                String name = prefs.getString("farmer_name", "किसान साथी");
                String phone = prefs.getString("farmer_phone", prefs.getString("phone", "8871291126"));

                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        URL url = new URL("https://mera-kisan-backend.vercel.app/api/admin");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setDoOutput(true);

                        JSONObject p = new JSONObject();
                        p.put("action", "create_dispute");
                        p.put("user_type", "farmer");
                        p.put("name", name);
                        p.put("phone", phone);
                        p.put("message", msg);

                        OutputStream os = conn.getOutputStream();
                        os.write(p.toString().getBytes("UTF-8"));
                        os.close();
                        conn.getResponseCode();
                    } catch (Exception ignored) {}
                });
                Toast.makeText(this, "✅ आपकी बात एडमिन तक पहुँच गई!", Toast.LENGTH_SHORT).show();
            }
        });
        b.setNegativeButton("रद्द करें", null);
        b.show();
    }
}
