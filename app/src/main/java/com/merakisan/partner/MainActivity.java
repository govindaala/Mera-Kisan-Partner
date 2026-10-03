// File Path: app/src/main/java/com/merakisan/partner/MainActivity.java
package com.merakisan.partner;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String PREF_NAME = "MeraKisanPartnerPrefs";

    // हेडर व्यूज
    private TextView txtHeaderFarmerName, txtHeaderVillage;

    // टैब लेआउट्स
    private LinearLayout tabOverview, tabMyCrops, tabAddCrop, tabProfile;
    private Button btnNavOverview, btnNavMyCrops, btnNavAddCrop, btnNavProfile;

    // ओवरव्यू टैब
    private TextView txtOverviewProfileStatus, txtOverviewProfileTip;
    private ProgressBar pbOverviewProfile;
    private Button btnQuickAddCrop;

    // मेरी फ़सलें टैब
    private LinearLayout containerCropsList;
    private TextView txtNoCropsNotice;
    private Button btnRefreshCrops;

    // फ़सल जोड़ें टैब
    private EditText edtAddCropName, edtAddCropPrice, edtAddCropStock;
    private Spinner spnAddCropFarmingType;
    private Button btnSubmitNewCrop;

    // प्रोफ़ाइल टैब
    private TextView txtProfileScoreTitle, txtGoldBadgeNotice;
    private ProgressBar pbProfileFull;
    private EditText edtProfileLand, edtProfileUpi, edtProfileOrganicCert;
    private Button btnSaveProfileUpdates;

    // शिकायत व विवाद
    private Spinner spnDisputeIssueType;
    private EditText edtDisputeOrderId, edtDisputeMessage;
    private Button btnSubmitDispute, btnLogout;

    // डायनामिक सेटिंग्स
    private boolean isPaymentOnline = false;
    private String farmerApkLink = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupSpinners();
        loadLocalProfile();
        setupTabNavigation();
        fetchPublicConfig();
        loadFarmerCrops();
    }

    private void initViews() {
        txtHeaderFarmerName = findViewById(R.id.txtHeaderFarmerName);
        txtHeaderVillage = findViewById(R.id.txtHeaderVillage);

        tabOverview = findViewById(R.id.tabOverview);
        tabMyCrops = findViewById(R.id.tabMyCrops);
        tabAddCrop = findViewById(R.id.tabAddCrop);
        tabProfile = findViewById(R.id.tabProfile);

        btnNavOverview = findViewById(R.id.btnNavOverview);
        btnNavMyCrops = findViewById(R.id.btnNavMyCrops);
        btnNavAddCrop = findViewById(R.id.btnNavAddCrop);
        btnNavProfile = findViewById(R.id.btnNavProfile);

        txtOverviewProfileStatus = findViewById(R.id.txtOverviewProfileStatus);
        txtOverviewProfileTip = findViewById(R.id.txtOverviewProfileTip);
        pbOverviewProfile = findViewById(R.id.pbOverviewProfile);
        btnQuickAddCrop = findViewById(R.id.btnQuickAddCrop);

        containerCropsList = findViewById(R.id.containerCropsList);
        txtNoCropsNotice = findViewById(R.id.txtNoCropsNotice);
        btnRefreshCrops = findViewById(R.id.btnRefreshCrops);

        edtAddCropName = findViewById(R.id.edtAddCropName);
        edtAddCropPrice = findViewById(R.id.edtAddCropPrice);
        edtAddCropStock = findViewById(R.id.edtAddCropStock);
        spnAddCropFarmingType = findViewById(R.id.spnAddCropFarmingType);
        btnSubmitNewCrop = findViewById(R.id.btnSubmitNewCrop);

        txtProfileScoreTitle = findViewById(R.id.txtProfileScoreTitle);
        txtGoldBadgeNotice = findViewById(R.id.txtGoldBadgeNotice);
        pbProfileFull = findViewById(R.id.pbProfileFull);
        edtProfileLand = findViewById(R.id.edtProfileLand);
        edtProfileUpi = findViewById(R.id.edtProfileUpi);
        edtProfileOrganicCert = findViewById(R.id.edtProfileOrganicCert);
        btnSaveProfileUpdates = findViewById(R.id.btnSaveProfileUpdates);

        spnDisputeIssueType = findViewById(R.id.spnDisputeIssueType);
        edtDisputeOrderId = findViewById(R.id.edtDisputeOrderId);
        edtDisputeMessage = findViewById(R.id.edtDisputeMessage);
        btnSubmitDispute = findViewById(R.id.btnSubmitDispute);
        btnLogout = findViewById(R.id.btnLogout);

        btnQuickAddCrop.setOnClickListener(v -> switchTab(3));
        btnRefreshCrops.setOnClickListener(v -> loadFarmerCrops());
        btnSubmitNewCrop.setOnClickListener(v -> handleAddNewCrop());
        btnSaveProfileUpdates.setOnClickListener(v -> handleSaveProfile());
        btnSubmitDispute.setOnClickListener(v -> handleSubmitDispute());
        btnLogout.setOnClickListener(v -> handleLogout());
    }

    private void setupSpinners() {
        String[] farmingTypes = {"सामान्य रासायनिक (Traditional)", "100% प्राकृतिक / जैविक (Organic)"};
        ArrayAdapter<String> adapterFarming = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, farmingTypes);
        spnAddCropFarmingType.setAdapter(adapterFarming);

        String[] issues = {"पेमेंट नहीं मिला (Payment Issue)", "ग्राहक ने संपर्क नहीं किया", "फ़सल लिस्टिंग में समस्या", "अन्य सामान्य शिकायत"};
        ArrayAdapter<String> adapterIssues = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, issues);
        spnDisputeIssueType.setAdapter(adapterIssues);
    }

    private void setupTabNavigation() {
        btnNavOverview.setOnClickListener(v -> switchTab(1));
        btnNavMyCrops.setOnClickListener(v -> switchTab(2));
        btnNavAddCrop.setOnClickListener(v -> switchTab(3));
        btnNavProfile.setOnClickListener(v -> switchTab(4));
    }

    private void switchTab(int tabIndex) {
        tabOverview.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        tabMyCrops.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);
        tabAddCrop.setVisibility(tabIndex == 3 ? View.VISIBLE : View.GONE);
        tabProfile.setVisibility(tabIndex == 4 ? View.VISIBLE : View.GONE);

        btnNavOverview.setTextColor(tabIndex == 1 ? Color.parseColor("#166534") : Color.parseColor("#64748B"));
        btnNavMyCrops.setTextColor(tabIndex == 2 ? Color.parseColor("#166534") : Color.parseColor("#64748B"));
        btnNavAddCrop.setTextColor(tabIndex == 3 ? Color.parseColor("#166534") : Color.parseColor("#64748B"));
        btnNavProfile.setTextColor(tabIndex == 4 ? Color.parseColor("#166534") : Color.parseColor("#64748B"));
    }

    private void loadLocalProfile() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String name = prefs.getString("farmer_name", "किसान साथी");
        String village = prefs.getString("village", "गाँव");
        String land = prefs.getString("land_acres", "");
        String upi = prefs.getString("farmer_upi", "");
        String cert = prefs.getString("organic_cert", "");

        txtHeaderFarmerName.setText("किसान साथी: " + name);
        txtHeaderVillage.setText("📍 " + village);

        edtProfileLand.setText(land);
        edtProfileUpi.setText(upi);
        edtProfileOrganicCert.setText(cert);

        calculateProfilePercentage();
    }

    private void calculateProfilePercentage() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int score = 40; // Google (20) + Phone (20)

        if (!prefs.getString("village", "").isEmpty()) score += 15;
        if (!prefs.getString("farmer_upi", "").isEmpty()) score += 15;
        if (!prefs.getString("land_acres", "").isEmpty()) score += 10;
        if (!prefs.getString("organic_cert", "").isEmpty()) score += 10;
        if (prefs.getBoolean("has_added_crop", false)) score += 10;

        if (score > 100) score = 100;

        pbOverviewProfile.setProgress(score);
        pbProfileFull.setProgress(score);

        String badge = score >= 100 ? " (सत्यापित गोल्ड पार्टनर ⭐)" : " (सत्यापित किसान 🥈)";
        txtOverviewProfileStatus.setText("आपकी प्रोफ़ाइल: " + score + "% पूर्ण है" + badge);
        txtProfileScoreTitle.setText("आपकी प्रोफ़ाइल: " + score + "% पूर्ण है" + badge);

        if (score >= 100) {
            txtGoldBadgeNotice.setText("⭐ बधाई! आप Mera Kisan के सत्यापित गोल्ड पार्टनर हैं।");
            txtGoldBadgeNotice.setTextColor(Color.parseColor("#16A34A"));
        }
    }

    private void fetchPublicConfig() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL("https://mera-kisan-backend.vercel.app/api/admin?action=get_public_config");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();

                    JSONObject obj = new JSONObject(sb.toString());
                    isPaymentOnline = obj.optBoolean("payment_enabled", false);
                    farmerApkLink = obj.optString("farmer_apk_url", "");
                }
            } catch (Exception ignored) {}
        });
    }

    private void handleAddNewCrop() {
        String cropName = edtAddCropName.getText().toString().trim();
        String priceStr = edtAddCropPrice.getText().toString().trim();
        String stockStr = edtAddCropStock.getText().toString().trim();
        String fType = spnAddCropFarmingType.getSelectedItem().toString();

        if (cropName.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty()) {
            Toast.makeText(this, "कृपया फ़सल का नाम, भाव और स्टॉक भरें!", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String name = prefs.getString("farmer_name", "");
        String phone = prefs.getString("farmer_phone", "");
        String village = prefs.getString("village", "");
        double lat = prefs.getFloat("lat", 24.12f);
        double lng = prefs.getFloat("lng", 75.58f);

        Toast.makeText(this, "फ़सल मंडी में दर्ज की जा रही है...", Toast.LENGTH_SHORT).show();

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
                payload.put("lat", lat);
                payload.put("lng", lng);
                payload.put("crop_name", cropName);
                payload.put("price_per_kg", Double.parseDouble(priceStr));
                payload.put("stock_qty_kg", Double.parseDouble(stockStr));
                payload.put("farming_type", fType.contains("जैविक") ? "organic" : "traditional");

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.close();
                conn.getResponseCode();

                prefs.edit().putBoolean("has_added_crop", true).apply();
            } catch (Exception ignored) {}

            new Handler(Looper.getMainLooper()).post(() -> {
                edtAddCropName.setText("");
                edtAddCropPrice.setText("");
                edtAddCropStock.setText("");
                Toast.makeText(this, "🎉 फ़सल सफलतापूर्वक लाइव हो गई!", Toast.LENGTH_LONG).show();
                calculateProfilePercentage();
                switchTab(2);
                loadFarmerCrops();
            });
        });
    }

    private void loadFarmerCrops() {
        containerCropsList.removeAllViews();
        txtNoCropsNotice.setVisibility(View.VISIBLE);

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String myPhone = prefs.getString("farmer_phone", "");

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL("https://mera-kisan-backend.vercel.app/api/crops");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();

                    JSONObject res = new JSONObject(sb.toString());
                    JSONArray cropsArr = res.optJSONArray("crops");

                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (cropsArr != null && cropsArr.length() > 0) {
                            int count = 0;
                            for (int i = 0; i < cropsArr.length(); i++) {
                                JSONObject c = cropsArr.optJSONObject(i);
                                if (c != null && myPhone.equals(c.optString("farmer_phone"))) {
                                    count++;
                                    addCropCardToUi(c);
                                }
                            }
                            txtNoCropsNotice.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
                        }
                    });
                }
            } catch (Exception ignored) {}
        });
    }

    private void addCropCardToUi(JSONObject crop) {
        String cropId = crop.optString("crop_id");
        String name = crop.optString("crop_name");
        double price = crop.optDouble("price_per_kg");
        double stock = crop.optDouble("stock_qty_kg");
        String farmingType = crop.optString("farming_type");
        boolean isOrganic = "organic".equalsIgnoreCase(farmingType);
        boolean isPromoted = crop.optBoolean("is_promoted", false);

        CardView card = new CardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 16);
        card.setLayoutParams(lp);
        card.setRadius(12);
        card.setCardElevation(3);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        // शीर्षक व बैज
        TextView title = new TextView(this);
        title.setText("🌾 " + name + (isOrganic ? "  🛡️ प्रमाणित जैविक" : "") + (isPromoted ? "  🔥 प्रमोटेड" : ""));
        title.setTextSize(16);
        title.setTextColor(Color.parseColor("#166534"));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(title);

        // भाव व स्टॉक
        TextView details = new TextView(this);
        details.setText("💰 भाव: ₹" + (int)price + "/किलो (₹" + (int)(price * 100) + "/क्विंटल)\n📦 उपलब्ध स्टॉक: " + (int)stock + " किलो (लगभग " + (int)(stock / 100) + " क्विंटल)");
        details.setTextSize(13);
        details.setTextColor(Color.parseColor("#334155"));
        details.setPadding(0, 8, 0, 12);
        layout.addView(details);

        // बटन्स पंक्ति
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnPoster = new Button(this);
        btnPoster.setText("🖼️ मंडी पोस्टर शेयर");
        btnPoster.setBackgroundColor(Color.parseColor("#166534"));
        btnPoster.setTextColor(Color.WHITE);
        btnPoster.setTextSize(11);
        btnPoster.setOnClickListener(v -> generateAndSharePoster(name, price, stock, isOrganic));
        btnRow.addView(btnPoster);

        Button btnBoost = new Button(this);
        btnBoost.setText(isPromoted ? "🔥 प्रमोटेड" : "🚀 बूस्ट करें");
        btnBoost.setBackgroundColor(Color.parseColor("#0284C7"));
        btnBoost.setTextColor(Color.WHITE);
        btnBoost.setTextSize(11);
        LinearLayout.LayoutParams boostLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        boostLp.setMargins(8, 0, 0, 0);
        btnBoost.setLayoutParams(boostLp);
        btnBoost.setOnClickListener(v -> handleBoostCrop(cropId));
        btnRow.addView(btnBoost);

        layout.addView(btnRow);
        card.addView(layout);
        containerCropsList.addView(card);
    }

    private void handleBoostCrop(String cropId) {
        if (!isPaymentOnline) {
            Toast.makeText(this, "🚀 फ़सल प्रमोशन सुविधा जल्द शुरू होगी! अभी आपकी फ़सल सामान्य लिस्टिंग में सक्रिय है।", Toast.LENGTH_LONG).show();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL("https://mera-kisan-backend.vercel.app/api/crops");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("action", "boost_crop");
                payload.put("crop_id", cropId);
                payload.put("days", 7);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.close();
                conn.getResponseCode();
            } catch (Exception ignored) {}

            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(this, "फ़सल 7 दिनों के लिए टॉप पर प्रमोट कर दी गई!", Toast.LENGTH_SHORT).show();
                loadFarmerCrops();
            });
        });
    }

    // 🖼️ अल्ट्रा HD 1080×1080 पोस्टर जनरेटर (पेमेंट बंद रहने पर 10% नहीं दिखेगा)
    private void generateAndSharePoster(String cropName, double price, double stock, boolean isOrganic) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String farmerName = prefs.getString("farmer_name", "किसान");
        String village = prefs.getString("village", "गाँव");
        String phone = prefs.getString("farmer_phone", "");

        Bitmap bitmap = Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // बैकग्राउंड
        canvas.drawColor(Color.parseColor("#F8FAFC"));

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // हेडर पट्टी
        paint.setColor(Color.parseColor("#166534"));
        canvas.drawRect(0, 0, 1080, 180, paint);

        // हेडर टेक्स्ट
        paint.setColor(Color.WHITE);
        paint.setTextSize(54);
        paint.setFakeBoldText(true);
        canvas.drawText("🌱 Mera Kisan Direct", 60, 100, paint);

        paint.setTextSize(26);
        paint.setFakeBoldText(false);
        paint.setColor(Color.parseColor("#BBF7D0"));
        canvas.drawText("खेत से सीधे आपके घर | बिचौलिया-मुक्त सीधी खरीद", 60, 145, paint);

        // फ़सल कार्ड
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(60, 220, 1020, 720, 24, 24, paint);

        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(50);
        paint.setFakeBoldText(true);
        canvas.drawText("🌾 " + cropName, 100, 310, paint);

        if (isOrganic) {
            paint.setColor(Color.parseColor("#16A34A"));
            paint.setTextSize(32);
            canvas.drawText("🛡️ 100% शुद्ध देशी एवं प्रमाणित जैविक", 100, 370, paint);
        }

        // भाव व स्टॉक
        paint.setColor(Color.parseColor("#1E293B"));
        paint.setTextSize(42);
        canvas.drawText("💰 थोक भाव: ₹" + (int)(price * 100) + " / क्विंटल  (₹" + (int)price + "/kg)", 100, 460, paint);

        paint.setTextSize(36);
        paint.setColor(Color.parseColor("#475569"));
        canvas.drawText("📦 उपलब्ध स्टॉक: " + (int)(stock / 100) + " क्विंटल (" + (int)stock + " किलो)", 100, 530, paint);

        // किसान विवरण
        paint.setColor(Color.parseColor("#0284C7"));
        paint.setTextSize(36);
        paint.setFakeBoldText(true);
        canvas.drawText("👨‍🌾 किसान: " + farmerName + " | 📍 " + village, 100, 620, paint);

        paint.setColor(Color.parseColor("#166534"));
        paint.setTextSize(38);
        canvas.drawText("📞 सीधा संपर्क / WhatsApp: +91 " + phone, 100, 680, paint);

        // नीचे की गारंटी पट्टी (डायनामिक: पेमेंट ऑन होने पर 10% दिखेगा, बंद होने पर सिर्फ सीधा संपर्क)
        paint.setColor(Color.parseColor("#0F172A"));
        canvas.drawRect(0, 760, 1080, 1080, paint);

        paint.setColor(Color.parseColor("#FCD34D"));
        paint.setTextSize(34);
        paint.setFakeBoldText(true);

        if (isPaymentOnline) {
            canvas.drawText("🛡️ Mera Kisan 10% सुरक्षित एस्क्रो टोकन बुकिंग चालू है", 60, 840, paint);
        } else {
            canvas.drawText("🤝 खेत से सीधी खरीद • 0% बिचौलिया कमीशन • सीधा संपर्क", 60, 840, paint);
        }

        paint.setColor(Color.WHITE);
        paint.setTextSize(26);
        paint.setFakeBoldText(false);
        canvas.drawText("सीधे खेत पर जाकर माल देखें या कॉल करके सौदा तय करें।", 60, 890, paint);

        if (farmerApkLink != null && !farmerApkLink.isEmpty()) {
            paint.setColor(Color.parseColor("#38BDF8"));
            canvas.drawText("📲 ऐप डाउनलोड करें: " + farmerApkLink, 60, 950, paint);
        }

        // WhatsApp / सोशल मीडिया पर शेयर करना
        try {
            String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "mandi_poster_" + System.currentTimeMillis(), "Mera Kisan Poster");
            Uri uri = Uri.parse(path);

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/*");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.putExtra(Intent.EXTRA_TEXT, "🌾 *Mera Kisan - ताज़ा फ़सल उपलब्ध!*\n\nफसल: " + cropName + "\nभाव: ₹" + (int)price + "/kg\nकिसान: " + farmerName + " (" + village + ")\nकॉल / WhatsApp करें: +91 " + phone);
            startActivity(Intent.createChooser(share, "मंडी पोस्टर WhatsApp पर शेयर करें"));
        } catch (Exception e) {
            Toast.makeText(this, "पोस्टर साझा करने में त्रुटि", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSaveProfile() {
        String land = edtProfileLand.getText().toString().trim();
        String upi = edtProfileUpi.getText().toString().trim();
        String cert = edtProfileOrganicCert.getText().toString().trim();

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString("land_acres", land)
                .putString("farmer_upi", upi)
                .putString("organic_cert", cert)
                .apply();

        String phone = prefs.getString("farmer_phone", "");

        Toast.makeText(this, "प्रोफ़ाइल सुरक्षित की जा रही है...", Toast.LENGTH_SHORT).show();

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
                Toast.makeText(this, "✅ प्रोफ़ाइल सफलतापूर्वक अपडेट हो गई!", Toast.LENGTH_SHORT).show();
                calculateProfilePercentage();
            });
        });
    }

    private void handleSubmitDispute() {
        String issueType = spnDisputeIssueType.getSelectedItem().toString();
        String orderId = edtDisputeOrderId.getText().toString().trim();
        String message = edtDisputeMessage.getText().toString().trim();

        if (message.isEmpty()) {
            Toast.makeText(this, "कृपया शिकायत का विवरण दर्ज करें!", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String name = prefs.getString("farmer_name", "किसान");
        String phone = prefs.getString("farmer_phone", "");

        Toast.makeText(this, "शिकायत दर्ज की जा रही है...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL("https://mera-kisan-backend.vercel.app/api/admin");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("action", "create_dispute");
                payload.put("user_type", "farmer");
                payload.put("name", name);
                payload.put("phone", phone);
                payload.put("order_id", orderId.isEmpty() ? "N/A" : orderId);
                payload.put("issue_type", issueType);
                payload.put("message", message);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.close();
                conn.getResponseCode();
            } catch (Exception ignored) {}

            new Handler(Looper.getMainLooper()).post(() -> {
                edtDisputeOrderId.setText("");
                edtDisputeMessage.setText("");
                Toast.makeText(this, "✅ शिकायत सुपर एडमिन को भेज दी गई है!", Toast.LENGTH_LONG).show();
            });
        });
    }

    private void handleLogout() {
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
