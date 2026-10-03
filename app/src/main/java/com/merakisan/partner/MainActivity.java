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

    private void loadMyCrops() {
        cropsProgressBar.setVisibility(View.VISIBLE);
        containerFarmerCrops.removeAllViews();

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String myPhone = prefs.getString("farmer_phone", "");

        Executors.newSingleThreadExecutor().execute(() -> {
            List<JSONObject> myList = new ArrayList<>();
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
                    JSONArray arr = res.optJSONArray("crops");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject c = arr.getJSONObject(i);
                            if (myPhone.equals(c.optString("farmer_phone", ""))) {
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
        String category = crop.optString("category", "अनाज व दालें");
        boolean isOrganic = "organic".equalsIgnoreCase(crop.optString("farming_type"));

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

        TextView details = new TextView(this);
        details.setText("💰 भाव: ₹" + (int)price + "/kg (₹" + (int)(price * 100) + "/क्विंटल)  |  📦 स्टॉक: " + (int)stock + " किलो");
        details.setTextSize(13);
        details.setTextColor(Color.parseColor("#334155"));
        details.setPadding(0, 4, 0, 10);
        box.addView(details);

        // 🎨 AI ग्राफ़िक पोस्टर शेयर बटन
        Button btnPoster = new Button(this);
        btnPoster.setText("🖼️ AI मंडी पोस्टर बनाएँ व शेयर करें (WhatsApp)");
        btnPoster.setBackgroundColor(Color.parseColor("#166534"));
        btnPoster.setTextColor(Color.WHITE);
        btnPoster.setTextSize(12);
        btnPoster.setOnClickListener(v -> generateAndSharePoster(name, price, stock, isOrganic, category));
        box.addView(btnPoster);

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
        String farmerPhone = prefs.getString("farmer_phone", "");
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


    private void saveFarmerProfile() {
        String land = edtProfileLand.getText().toString().trim();
        String upi = edtProfileUpi.getText().toString().trim();
        String cert = edtProfileCert.getText().toString().trim();

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String phone = prefs.getString("farmer_phone", "");

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
    // 🎨 AI ग्राफ़िक पोस्टर जनरेटर (इंटरनेट से असली फ़ोटो + मार्केटिंग मिर्च-मसाला)
    // =========================================================================
    private void generateAndSharePoster(String cropName, double price, double stock, boolean isOrganic, String category) {
        Toast.makeText(this, "🎨 इंटरनेट से '" + cropName + "' का AI बैकग्राउंड व पोस्टर तैयार हो रहा है...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            Bitmap finalPoster = null;
            try {
                // 1. फ़सल और श्रेणी के अनुसार AI सर्च प्रॉम्प्ट तैयार करना
                String searchKeyword = getSmartSearchKeyword(cropName, category);

                String aiImageUrl = "https://image.pollinations.ai/prompt/cinematic%20golden%20harvest%20field%20of%20" 
                        + URLEncoder.encode(searchKeyword, "UTF-8") 
                        + "%20fresh%20crop%20indian%20agriculture%20farm%20sunset?width=1080&height=1080&nologo=true";

                Bitmap bgBitmap = null;
                try {
                    URL url = new URL(aiImageUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(6500);
                    conn.setReadTimeout(6500);
                    conn.connect();
                    if (conn.getResponseCode() == 200) {
                        bgBitmap = BitmapFactory.decodeStream(conn.getInputStream());
                    }
                } catch (Exception e) {
                    bgBitmap = null; // इंटरनेट स्लो होने पर फॉलबैक
                }

                // 2. मुख्य 1080x1080 कैनवास तैयार करना
                finalPoster = Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(finalPoster);

                if (bgBitmap != null) {
                    Bitmap scaledBg = Bitmap.createScaledBitmap(bgBitmap, 1080, 1080, true);
                    canvas.drawBitmap(scaledBg, 0, 0, null);
                } else {
                    canvas.drawColor(Color.parseColor("#064E3B")); // समृद्ध गहरा हरा बैकग्राउंड
                }

                Paint paint = new Paint();
                paint.setAntiAlias(true);

                // 3. ग्लास शेड डार्क ओवरले (ताकि सारा टेक्स्ट एकदम साफ और उभर कर दिखे)
                paint.setColor(Color.argb(175, 15, 23, 42));
                canvas.drawRect(0, 0, 1080, 1080, paint);

                // 4. सुनहरा व हरा डबल शाही बॉर्डर
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(12);
                paint.setColor(Color.parseColor("#EAB308")); // चमकीला गोल्ड
                canvas.drawRect(20, 20, 1060, 1060, paint);

                paint.setStrokeWidth(4);
                paint.setColor(Color.parseColor("#22C55E")); // एमराल्ड ग्रीन
                canvas.drawRect(34, 34, 1046, 1046, paint);

                paint.setStyle(Paint.Style.FILL);

                // 5. हेडर बैनर (Mera Kisan Direct)
                paint.setColor(Color.parseColor("#15803D"));
                canvas.drawRoundRect(new RectF(50, 50, 1030, 170), 16, 16, paint);

                paint.setColor(Color.parseColor("#FEF08A"));
                paint.setTextSize(26);
                paint.setFakeBoldText(true);
                canvas.drawText("🌱 MERA KISAN DIRECT • सीधे खेत से शुद्ध पैदावार", 80, 95, paint);

                paint.setColor(Color.WHITE);
                paint.setTextSize(44);
                paint.setFakeBoldText(true);
                canvas.drawText("खेत से सीधी खरीद • 0% बिचौलिया दलाली", 80, 145, paint);

                // 6. मुख्य उत्पाद कार्ड (व्हाइट ग्लास इफ़ेक्ट)
                paint.setColor(Color.argb(240, 255, 255, 255));
                canvas.drawRoundRect(new RectF(50, 190, 1030, 440), 20, 20, paint);

                paint.setColor(Color.parseColor("#0F172A"));
                paint.setTextSize(50);
                paint.setFakeBoldText(true);
                canvas.drawText("🌾 " + cropName, 80, 258, paint);

                if (isOrganic) {
                    paint.setColor(Color.parseColor("#15803D"));
                    paint.setTextSize(24);
                    canvas.drawText("🛡️ 100% शुद्ध प्राकृतिक देशी खाद से तैयार जैविक उत्पाद", 80, 302, paint);
                } else {
                    paint.setColor(Color.parseColor("#B45309"));
                    paint.setTextSize(24);
                    canvas.drawText("⭐ सुपर प्रीमियम ग्रेड-1 • खेत का सीधा ताज़ा स्टॉक", 80, 302, paint);
                }

                // भाव व स्टॉक स्ट्रिप
                paint.setColor(Color.parseColor("#FEF3C7"));
                canvas.drawRoundRect(new RectF(80, 330, 1000, 415), 12, 12, paint);

                paint.setColor(Color.parseColor("#92400E"));
                paint.setTextSize(32);
                paint.setFakeBoldText(true);
                canvas.drawText("💰 भाव: ₹" + (int)(price * 100) + "/क्विंटल (₹" + (int)price + "/kg)  |  📦 स्टॉक: " + (int)stock + " किलो", 100, 385, paint);

                // 7. 🔥 मिर्च-मसाला सेक्शन: फ़सल विशेषताएँ, दाने की क्वालिटी और स्वास्थ्य लाभ
                paint.setColor(Color.argb(235, 241, 245, 249));
                canvas.drawRoundRect(new RectF(50, 460, 1030, 755), 20, 20, paint);

                paint.setColor(Color.parseColor("#1E293B"));
                paint.setTextSize(28);
                paint.setFakeBoldText(true);
                canvas.drawText("✨ इस माल की खासियत एवं स्वास्थ्य लाभ (Quality & Purity Guarantee):", 80, 505, paint);

                String[] benefits = generateMarketingHighlights(cropName, category, isOrganic);
                paint.setColor(Color.parseColor("#334155"));
                paint.setTextSize(24);
                paint.setFakeBoldText(false);
                canvas.drawText("• " + benefits[0], 80, 555, paint);
                canvas.drawText("• " + benefits[1], 80, 605, paint);
                canvas.drawText("• " + benefits[2], 80, 655, paint);
                canvas.drawText("• " + benefits[3], 80, 705, paint);

                // 8. किसान पहचान व सीधा संपर्क कार्ड
                SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                String farmerName = prefs.getString("farmer_name", "गोविंद पाटीदार");
                String village = prefs.getString("village", "बर्दि‍या अमरा (मंदसौर)");
                String phone = prefs.getString("farmer_phone", "8871291126");

                paint.setColor(Color.parseColor("#064E3B"));
                canvas.drawRoundRect(new RectF(50, 775, 1030, 945), 20, 20, paint);

                paint.setColor(Color.parseColor("#BBF7D0"));
                paint.setTextSize(28);
                paint.setFakeBoldText(true);
                canvas.drawText("👨‍🌾 उत्पादक किसान: " + farmerName + "  |  📍 " + village, 80, 830, paint);

                paint.setColor(Color.WHITE);
                paint.setTextSize(38);
                paint.setFakeBoldText(true);
                canvas.drawText("📞 सीधा कॉल / WhatsApp: +91 " + phone, 80, 900, paint);

                // 9. फ़ुटर गारंटी
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
                    sharePosterToWhatsApp(shareBmp, cropName, price, stock);
                } else {
                    Toast.makeText(this, "पोस्टर तैयार नहीं हो सका", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // इंटरनेट AI इमेज सर्च के लिए सटीक कीवर्ड
    private String getSmartSearchKeyword(String name, String cat) {
        String n = name.toLowerCase();
        if (n.contains("मिलेट") || n.contains("चीना") || n.contains("पोसो")) return "proso millet golden grains harvest";
        if (n.contains("लहसुन") || n.contains("garlic")) return "fresh white garlic bulbs harvest farm";
        if (n.contains("तेल") || cat.contains("तेल")) return "traditional cold pressed mustard groundnut oil farm";
        if (n.contains("गुड़") || cat.contains("गुड़")) return "traditional indian sugarcane jaggery gur making";
        if (n.contains("मसाले") || cat.contains("मसाले") || n.contains("धनिया") || n.contains("मेथी")) return "indian whole spices coriander seeds red chilli farm";
        if (n.contains("गेहूँ") || n.contains("wheat")) return "golden ripe wheat grains field harvest";
        if (n.contains("सोयाबीन") || n.contains("soyabean")) return "golden soybean farm harvest pods";
        if (n.contains("चना") || n.contains("दाल")) return "chickpea gram legumes harvest field";
        return "indian golden agriculture crops farm harvest";
    }

    // मार्केटिंग और मिर्च-मसाला कॉपीराइटिंग इंजन
    private String[] generateMarketingHighlights(String name, String cat, boolean isOrganic) {
        String n = name.toLowerCase();

        if (n.contains("मिलेट") || n.contains("चीना") || n.contains("पोसो") || n.contains("कोदो") || n.contains("रागी")) {
            return new String[]{
                    "दाना: प्राकृतिक धूप में सुखाया हुआ, बोल्ड दाना, कंकड़-मिट्टी व कचरा रहित साफ़।",
                    "पोषण: 100% ग्लूटेन-फ्री (Gluten-Free), हाई फ़ाइबर और प्रोटीन का प्राकृतिक खजाना।",
                    "स्वास्थ्य लाभ: शुगर (डायबिटीज) कंट्रोल, हृदय स्वास्थ्य और मोटापा घटाने में सर्वोत्तम सुपरफ़ूड।",
                    "गारंटी: मंदसौर के खेतों की शुद्ध देशी पैदावार, बिना किसी केमिकल पॉलिश के।"
            };
        } else if (n.contains("तेल") || cat.contains("तेल")) {
            return new String[]{
                    "प्रक्रिया: 100% शुद्ध लकड़ी/कच्ची घानी (Cold Pressed), बिना केमिकल, बिना किसी मिलावट।",
                    "ताज़गी: बीजों का मूल प्राकृतिक स्वाद, तेज़ सुगंध और असली पोषक तत्व पूरी तरह सुरक्षित।",
                    "स्वास्थ्य लाभ: ज़ीरो कोलेस्ट्रॉल, दिल के लिए अत्यंत लाभकारी और पाचन में हल्का।",
                    "शुद्धता: बाज़ार के रिफ़ाइंड तेल से 10 गुना बेहतर, सीधे गाँव के कोहलू से निकाला शुद्ध तेल।"
            };
        } else if (n.contains("गुड़") || cat.contains("गुड़") || cat.contains("मीठा")) {
            return new String[]{
                    "उत्पादन: पारंपरिक देशी भट्टी पर पकाया गया, बिना हाइड्रो/केमिकल रंग के शुद्ध काला-लाल गुड़।",
                    "पोषक तत्व: प्राकृतिक आयरन, कैल्शियम, पोटेशियम और मिनरल्स से परिपूर्ण देशी मिठास।",
                    "स्वास्थ्य लाभ: भोजन पचाने, खून की कमी दूर करने और फेफड़ों को साफ़ रखने में रामबाण।",
                    "स्वाद: असली देशी गन्ने का सोंधा स्वाद, चाय व दूध में डालने पर फटने की कोई शिकायत नहीं।"
            };
        } else if (n.contains("लहसुन") || n.contains("garlic")) {
            return new String[]{
                    "क्वालिटी: ठोस, सफ़ेद व वज़नदार कंद (जी-2 / देशी किस्म), बड़े पर्दे और बोल्ड कलियाँ।",
                    "सुगंध व तीखापन: तेज़ प्राकृतिक तीखापन और गाढ़ा औषधीय रस, लंबे समय तक भंडारण योग्य।",
                    "स्वास्थ्य लाभ: कोलेस्ट्रॉल कम करने, हाई बीपी नियंत्रित रखने और इम्युनिटी बढ़ाने में रामबाण।",
                    "सीधी खरीद: विश्वविख्यात मंदसौर लहसुन मंडी क्षेत्र के खेत से सीधा माल, बिना बिचौलिए के।"
            };
        } else if (n.contains("मसाले") || cat.contains("मसाले") || n.contains("धनिया") || n.contains("मेथी") || n.contains("मिर्च")) {
            return new String[]{
                    "गुणवत्ता: खेत से हाथ से चुने गए खड़े मसाले, बिना लकड़ी-डंठल या धूल के साफ़ ग्रेडिंग।",
                    "स्वाद व सुगंध: असली प्राकृतिक तेल और तीखी भीनी सुगंध, बाज़ार के पिसे मसालों से दोगुना स्वाद।",
                    "शुद्धता: बिना किसी कृत्रिम रंग, स्टार्च या मिलावट के सीधे किसान के खलिहान से।",
                    "सौदा: थोक भाव में उपलब्ध, चाहे घरेलू इस्तेमाल के लिए लें या व्यापार के लिए।"
            };
        } else if (n.contains("गेहूँ") || n.contains("wheat") || n.contains("शरबती")) {
            return new String[]{
                    "किस्म: सुनहरे चमकदार और वज़नदार बोल्ड दाने, बिना घुन या कीड़े के सूखा सुरक्षित माल।",
                    "रोटी की खूबी: रोटियां अत्यंत नरम, फूली हुई और प्राकृतिक देशी मिठास से भरपूर।",
                    "पोषण: रिच डाइटरी फ़ाइबर और विटामिन्स, जो पेट को रखे एकदम दुरुस्त।",
                    "भंडारण: सालभर घर में स्टोर करने हेतु प्राकृतिक धूप में सुखाया हुआ सूखा सुरक्षित अनाज।"
            };
        } else {
            return new String[]{
                    "ग्रेडिंग: किसान द्वारा हाथ से छँटाई किया हुआ उच्च गुणवत्ता वाला ए-ग्रेड माल।",
                    "स्वास्थ्य लाभ: प्राकृतिक स्वाद और ताज़गी, पोषक तत्वों और विटामिन्स से भरपूर।",
                    "शुद्धता: " + (isOrganic ? "100% देशी गोबर खाद से तैयार प्रमाणित जैविक उपज।" : "खेत की ताज़ा उपज, बिना किसी कृत्रिम स्प्रे या मिलावट के।"),
                    "भरोसा: खेत पर स्वयं आकर माल चेक करने और अपनी मौजूदगी में तौल कराने की पूरी आज़ादी।"
            };
        }
    }

    private void sharePosterToWhatsApp(Bitmap bitmap, String cropName, double price, double stock) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String farmerName = prefs.getString("farmer_name", "किसान साथी");
        String village = prefs.getString("village", "बर्दि‍या अमरा");
        String phone = prefs.getString("farmer_phone", "");

        try {
            String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "mandi_poster_" + System.currentTimeMillis(), "Mera Kisan Poster");
            Uri uri = Uri.parse(path);

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/*");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.putExtra(Intent.EXTRA_TEXT, "🌾 *Mera Kisan Direct - ताज़ा उपज उपलब्ध!*\n\n" +
                    "उत्पाद: *" + cropName + "*\n" +
                    "भाव: ₹" + (int)price + "/kg (₹" + (int)(price * 100) + "/क्विंटल)\n" +
                    "उपलब्ध: " + (int)stock + " किलो\n" +
                    "किसान: " + farmerName + " (" + village + ")\n" +
                    "📞 सीधा संपर्क करें: +91 " + phone + "\n\n" +
                    "खेत से सीधी खरीद, 0% बिचौलिया दलाली!");
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
                String phone = prefs.getString("farmer_phone", "");

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
