// File Path: app/src/main/java/com/merakisan/partner/MainActivity.java
package com.merakisan.partner;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String PREF_NAME = "MeraKisanPartnerPrefs";
    private static final int VOICE_REQUEST_CODE = 102;

    // Tabs
    private ScrollView viewTabHome, viewTabCrops, viewTabOrders, viewTabProfile;
    private TextView txtNavHome, txtNavCrops, txtNavOrders, txtNavProfile;
    private TextView txtHeaderVillage;

    // Tab 1 (Home)
    private EditText edtCropName, edtPrice, edtQty, edtQualityTag;
    private Spinner spnFarmingType;
    private Button btnVoiceInput, btnSubmitCrop, btnShareWhatsApp, btnShareFacebook;
    private TextView txtVoiceStatus;
    private CardView cardPosterSection;
    private ImageView imgPosterPreview;
    private ProgressBar progressBar;

    // Tab 2 (My Crops)
    private TextView txtMyCropsCount;
    private LinearLayout layoutMyCropsList;
    private Button btnRefreshMyCrops;

    // Tab 3 (Orders)
    private TextView txtOrdersCount;
    private LinearLayout layoutOrdersList;
    private Button btnRefreshOrders;

    // Tab 4 (Profile)
    private TextView txtProfileName, txtProfileVillage, txtProfilePhone;
    private EditText edtFarmerUpi;
    private Button btnSaveUpi, btnShareStore;

    // Variables
    private String farmerName = "Kisan Sathi";
    private String farmerPhone = "";
    private String farmerVillage = "Gaon";
    private String farmerUpi = "";
    private Bitmap generatedPosterBitmap = null;
    private String generatedShareText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        loadLocalFarmerProfile();
        setupNavigation();
        setupSpinner();

        btnVoiceInput.setOnClickListener(v -> startVoiceRecognition());
        btnSubmitCrop.setOnClickListener(v -> submitCropAndGeneratePoster());
        btnShareWhatsApp.setOnClickListener(v -> sharePosterToApp("com.whatsapp"));
        btnShareFacebook.setOnClickListener(v -> sharePosterToApp("com.facebook.katana"));
        btnRefreshMyCrops.setOnClickListener(v -> loadMyCrops());
        btnRefreshOrders.setOnClickListener(v -> loadFarmerOrders());
        btnSaveUpi.setOnClickListener(v -> saveUpiId());
        btnShareStore.setOnClickListener(v -> shareDigitalStore());

        loadMyCrops();
        loadFarmerOrders();
    }

    private void initViews() {
        viewTabHome = findViewById(R.id.viewTabHome);
        viewTabCrops = findViewById(R.id.viewTabCrops);
        viewTabOrders = findViewById(R.id.viewTabOrders);
        viewTabProfile = findViewById(R.id.viewTabProfile);

        txtNavHome = findViewById(R.id.txtNavHome);
        txtNavCrops = findViewById(R.id.txtNavCrops);
        txtNavOrders = findViewById(R.id.txtNavOrders);
        txtNavProfile = findViewById(R.id.txtNavProfile);
        txtHeaderVillage = findViewById(R.id.txtHeaderVillage);

        // Tab 1
        edtCropName = findViewById(R.id.edtCropName);
        edtPrice = findViewById(R.id.edtPrice);
        edtQty = findViewById(R.id.edtQty);
        edtQualityTag = findViewById(R.id.edtQualityTag);
        spnFarmingType = findViewById(R.id.spnFarmingType);
        btnVoiceInput = findViewById(R.id.btnVoiceInput);
        btnSubmitCrop = findViewById(R.id.btnSubmitCrop);
        btnShareWhatsApp = findViewById(R.id.btnShareWhatsApp);
        btnShareFacebook = findViewById(R.id.btnShareFacebook);
        txtVoiceStatus = findViewById(R.id.txtVoiceStatus);
        cardPosterSection = findViewById(R.id.cardPosterSection);
        imgPosterPreview = findViewById(R.id.imgPosterPreview);
        progressBar = findViewById(R.id.progressBar);

        // Tab 2
        txtMyCropsCount = findViewById(R.id.txtMyCropsCount);
        layoutMyCropsList = findViewById(R.id.layoutMyCropsList);
        btnRefreshMyCrops = findViewById(R.id.btnRefreshMyCrops);

        // Tab 3
        txtOrdersCount = findViewById(R.id.txtOrdersCount);
        layoutOrdersList = findViewById(R.id.layoutOrdersList);
        btnRefreshOrders = findViewById(R.id.btnRefreshOrders);

        // Tab 4
        txtProfileName = findViewById(R.id.txtProfileName);
        txtProfileVillage = findViewById(R.id.txtProfileVillage);
        txtProfilePhone = findViewById(R.id.txtProfilePhone);
        edtFarmerUpi = findViewById(R.id.edtFarmerUpi);
        btnSaveUpi = findViewById(R.id.btnSaveUpi);
        btnShareStore = findViewById(R.id.btnShareStore);
    }

    private void loadLocalFarmerProfile() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        farmerName = prefs.getString("farmer_name", "Kisan Sathi");
        farmerPhone = prefs.getString("farmer_phone", "");
        farmerVillage = prefs.getString("village", "Gaon");
        farmerUpi = prefs.getString("farmer_upi", "");

        txtHeaderVillage.setText("📍 " + farmerVillage);
        txtProfileName.setText(farmerName);
        txtProfileVillage.setText("📍 " + farmerVillage);
        txtProfilePhone.setText("📱 +91 " + farmerPhone);
        edtFarmerUpi.setText(farmerUpi);
    }

    private void setupNavigation() {
        findViewById(R.id.navHome).setOnClickListener(v -> switchTab(0));
        findViewById(R.id.navCrops).setOnClickListener(v -> switchTab(1));
        findViewById(R.id.navOrders).setOnClickListener(v -> switchTab(2));
        findViewById(R.id.navProfile).setOnClickListener(v -> switchTab(3));
    }

    private void switchTab(int index) {
        viewTabHome.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        viewTabCrops.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        viewTabOrders.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        viewTabProfile.setVisibility(index == 3 ? View.VISIBLE : View.GONE);

        int active = Color.parseColor("#166534");
        int inactive = Color.parseColor("#64748B");

        txtNavHome.setTextColor(index == 0 ? active : inactive);
        txtNavCrops.setTextColor(index == 1 ? active : inactive);
        txtNavOrders.setTextColor(index == 2 ? active : inactive);
        txtNavProfile.setTextColor(index == 3 ? active : inactive);

        if (index == 1) loadMyCrops();
        if (index == 2) loadFarmerOrders();
    }

    private void setupSpinner() {
        String[] types = {"100% Jaivik (Organic)", "Prakritik Desi Kheti", "Samanya (Kam Chemical)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spnFarmingType.setAdapter(adapter);
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Fasal ka naam, bhav aur matra bolein...");
        try {
            startActivityForResult(intent, VOICE_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Mic support uplabdh nahi hai", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) {
                String spoken = matches.get(0);
                txtVoiceStatus.setText("Suna gaya: \"" + spoken + "\"");
                parseSpokenText(spoken);
            }
        }
    }

    private void parseSpokenText(String text) {
        Pattern pricePattern = Pattern.compile("(\\d+)\\s*(रुपये|रु|रूपए|भाव)");
        Matcher priceMatcher = pricePattern.matcher(text);
        if (priceMatcher.find()) edtPrice.setText(priceMatcher.group(1));

        Pattern qtyPattern = Pattern.compile("(\\d+)\\s*(क्विंटल|बोरी|किलो|kg)");
        Matcher qtyMatcher = qtyPattern.matcher(text);
        if (qtyMatcher.find()) {
            String qty = qtyMatcher.group(1);
            if (text.contains("क्विंटल")) {
                try { edtQty.setText(String.valueOf(Integer.parseInt(qty) * 100)); } catch (Exception ignored) {}
            } else {
                edtQty.setText(qty);
            }
        }

        String cropGuess = text.replaceAll("(\\d+)\\s*(रुपये|रु|रूपए|भाव|क्विंटल|बोरी|किलो|kg)", "").trim();
        if (!cropGuess.isEmpty()) edtCropName.setText(cropGuess);
    }

    private void submitCropAndGeneratePoster() {
        String crop = edtCropName.getText().toString().trim();
        String price = edtPrice.getText().toString().trim();
        String qty = edtQty.getText().toString().trim();
        String quality = edtQualityTag.getText().toString().trim();
        String farmingType = spnFarmingType.getSelectedItem().toString();

        if (crop.isEmpty() || price.isEmpty()) {
            Toast.makeText(this, "Kripya fasal ka naam aur bhav bharein!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (quality.isEmpty()) quality = "Grade-A Super Bold Dana";
        final String finalQuality = quality;

        progressBar.setVisibility(View.VISIBLE);
        btnSubmitCrop.setEnabled(false);

        generatedPosterBitmap = drawUltraHDPoster(crop, price, farmingType, finalQuality, qty);
        imgPosterPreview.setImageBitmap(generatedPosterBitmap);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL("https://mera-kisan-backend.vercel.app/api/add-crop");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("farmer_name", farmerName);
                payload.put("farmer_phone", farmerPhone);
                payload.put("village", farmerVillage);
                payload.put("crop_name", crop);
                payload.put("price_per_kg", price);
                payload.put("stock_qty_kg", qty.isEmpty() ? "100" : qty);
                payload.put("farming_type", farmingType);
                payload.put("quality_tag", finalQuality);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.close();
                conn.getResponseCode();
            } catch (Exception ignored) {}

            generatedShareText = "🌾 *Khet se seedhe aapke ghar — 100% Shuddh!*\n\n"
                    + "🌱 *Fasal:* " + crop + "\n"
                    + "⭐ *Quality:* " + finalQuality + " (" + farmingType + ")\n"
                    + "💰 *Bhav:* Matra ₹" + price + "/kilo\n"
                    + "👨‍🌾 *Kisan:* " + farmerName + " (" + farmerVillage + ")\n\n"
                    + "📞 *Seedha Sampark / WhatsApp:* +91 " + farmerPhone;

            new Handler(Looper.getMainLooper()).post(() -> {
                progressBar.setVisibility(View.GONE);
                btnSubmitCrop.setEnabled(true);
                cardPosterSection.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Fasal live ho gayi aur HD Poster taiyar!", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private Bitmap drawUltraHDPoster(String crop, String price, String type, String quality, String qty) {
        int w = 1080;
        int h = 1440;
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        canvas.drawColor(Color.parseColor("#F8FAF8"));

        Paint pHeader = new Paint();
        pHeader.setColor(Color.parseColor("#0F3E1B"));
        canvas.drawRect(0, 0, w, 190, pHeader);

        Paint pTitle = new Paint(Paint.ANTI_ALIAS_FLAG);
        pTitle.setColor(Color.WHITE);
        pTitle.setTextSize(54);
        pTitle.setFakeBoldText(true);
        canvas.drawText("🌱 Mera Kisan Direct", 60, 115, pTitle);

        Paint pSub = new Paint(Paint.ANTI_ALIAS_FLAG);
        pSub.setColor(Color.parseColor("#A3E635"));
        pSub.setTextSize(26);
        canvas.drawText("Khet se seedhe ghar tak • 100% Shuddh • Bina Bicholiye", 60, 160, pSub);

        Paint pFarmerBg = new Paint();
        pFarmerBg.setColor(Color.parseColor("#15803D"));
        RectF rFarmer = new RectF(50, 220, w - 50, 320);
        canvas.drawRoundRect(rFarmer, 20, 20, pFarmerBg);

        Paint pFarmerTxt = new Paint(Paint.ANTI_ALIAS_FLAG);
        pFarmerTxt.setColor(Color.WHITE);
        pFarmerTxt.setTextSize(38);
        pFarmerTxt.setFakeBoldText(true);
        canvas.drawText("👨‍🌾 Kisan: " + farmerName + "  ✓ Verified", 80, 285, pFarmerTxt);

        Paint pLocTxt = new Paint(Paint.ANTI_ALIAS_FLAG);
        pLocTxt.setColor(Color.parseColor("#FEF08A"));
        pLocTxt.setTextSize(28);
        canvas.drawText("📍 " + farmerVillage, w - 440, 285, pLocTxt);

        Paint pCardBg = new Paint();
        pCardBg.setColor(Color.WHITE);
        pCardBg.setShadowLayer(16, 0, 6, Color.parseColor("#D1D5DB"));
        RectF rCrop = new RectF(50, 350, w - 50, 960);
        canvas.drawRoundRect(rCrop, 26, 26, pCardBg);

        Paint pCropName = new Paint(Paint.ANTI_ALIAS_FLAG);
        pCropName.setColor(Color.parseColor("#0F3E1B"));
        pCropName.setTextSize(64);
        pCropName.setFakeBoldText(true);
        canvas.drawText("🌾 " + crop, 90, 450, pCropName);

        Paint pRibbonBg = new Paint();
        pRibbonBg.setColor(Color.parseColor("#ECFDF5"));
        RectF rRibbon = new RectF(90, 485, w - 90, 560);
        canvas.drawRoundRect(rRibbon, 14, 14, pRibbonBg);

        Paint pRibbonTxt = new Paint(Paint.ANTI_ALIAS_FLAG);
        pRibbonTxt.setColor(Color.parseColor("#047857"));
        pRibbonTxt.setTextSize(32);
        pRibbonTxt.setFakeBoldText(true);
        canvas.drawText("✓ " + type + "  |  " + quality, 120, 535, pRibbonTxt);

        Paint pGoldCard = new Paint();
        pGoldCard.setColor(Color.parseColor("#FEF3C7"));
        RectF rGold = new RectF(90, 590, w - 90, 740);
        canvas.drawRoundRect(rGold, 22, 22, pGoldCard);

        Paint pPriceLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
        pPriceLabel.setColor(Color.parseColor("#B45309"));
        pPriceLabel.setTextSize(30);
        canvas.drawText("Mandi se seedha bhav:", 130, 645, pPriceLabel);

        Paint pPriceVal = new Paint(Paint.ANTI_ALIAS_FLAG);
        pPriceVal.setColor(Color.parseColor("#D97706"));
        pPriceVal.setTextSize(72);
        pPriceVal.setFakeBoldText(true);
        canvas.drawText("₹ " + price + " / kilo", 130, 715, pPriceVal);

        Paint pStock = new Paint(Paint.ANTI_ALIAS_FLAG);
        pStock.setColor(Color.parseColor("#334155"));
        pStock.setTextSize(34);
        String qtyStr = qty.isEmpty() ? "100" : qty;
        canvas.drawText("📦 Stock: " + qtyStr + " kg (Taaza Lot)", 90, 810, pStock);

        Paint pStamp = new Paint(Paint.ANTI_ALIAS_FLAG);
        pStamp.setColor(Color.parseColor("#15803D"));
        pStamp.setTextSize(32);
        pStamp.setFakeBoldText(true);
        canvas.drawText("⭐ 100% Shuddhata aur Sahi Vajan Guarantee", 90, 880, pStamp);

        Paint pCtaBg = new Paint();
        pCtaBg.setColor(Color.parseColor("#0F3E1B"));
        RectF rCta = new RectF(50, 1000, w - 50, 1370);
        canvas.drawRoundRect(rCta, 26, 26, pCtaBg);

        Paint pCtaHead = new Paint(Paint.ANTI_ALIAS_FLAG);
        pCtaHead.setColor(Color.parseColor("#FDE047"));
        pCtaHead.setTextSize(44);
        pCtaHead.setFakeBoldText(true);
        canvas.drawText("Seedhe khet se mangwane hetu:", 100, 1080, pCtaHead);

        Paint pCtaSub = new Paint(Paint.ANTI_ALIAS_FLAG);
        pCtaSub.setColor(Color.WHITE);
        pCtaSub.setTextSize(32);
        canvas.drawText("Ghar pahunch delivery aur khet se uthan uplabdh.", 100, 1140, pCtaSub);

        Paint pBtn = new Paint();
        pBtn.setColor(Color.parseColor("#22C55E"));
        RectF rBtn = new RectF(100, 1180, w - 100, 1300);
        canvas.drawRoundRect(rBtn, 18, 18, pBtn);

        Paint pBtnTxt = new Paint(Paint.ANTI_ALIAS_FLAG);
        pBtnTxt.setColor(Color.WHITE);
        pBtnTxt.setTextSize(44);
        pBtnTxt.setFakeBoldText(true);
        canvas.drawText("📞 WhatsApp / Call: +91 " + farmerPhone, 140, 1255, pBtnTxt);

        return bitmap;
    }

    private void loadMyCrops() {
        txtMyCropsCount.setText("Faslein load ho rahi hain...");
        layoutMyCropsList.removeAllViews();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL("https://mera-kisan-backend.vercel.app/api/crops");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject res = new JSONObject(sb.toString());
                JSONArray arr = res.optJSONArray("crops");
                ArrayList<JSONObject> list = new ArrayList<>();
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject c = arr.getJSONObject(i);
                        if (c.optString("farmer_phone").equals(farmerPhone)) {
                            list.add(c);
                        }
                    }
                }

                new Handler(Looper.getMainLooper()).post(() -> renderMyCropsUI(list));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> txtMyCropsCount.setText("Load nahi ho saka."));
            }
        });
    }

    private void renderMyCropsUI(ArrayList<JSONObject> list) {
        layoutMyCropsList.removeAllViews();
        txtMyCropsCount.setText("Kul sakriya faslein: " + list.size());

        if (list.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Abhi koi fasal list nahi hai. Home tab se nayi fasal jodein.");
            empty.setTextColor(Color.GRAY);
            empty.setPadding(10, 20, 10, 20);
            layoutMyCropsList.addView(empty);
            return;
        }

        for (JSONObject c : list) {
            String cropId = c.optString("crop_id");
            String cropName = c.optString("crop_name");
            String price = c.optString("price_per_kg");
            String qty = c.optString("stock_qty_kg");

            CardView card = new CardView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 10, 0, 10);
            card.setLayoutParams(lp);
            card.setRadius(14);
            card.setCardElevation(3);
            card.setCardBackgroundColor(Color.WHITE);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(20, 20, 20, 20);

            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            details.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tName = new TextView(this);
            tName.setText("🌾 " + cropName);
            tName.setTextSize(16);
            tName.setTextColor(Color.parseColor("#166534"));
            tName.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tInfo = new TextView(this);
            tInfo.setText("Bhav: ₹" + price + "/kg | Stock: " + qty + " kg");
            tInfo.setTextSize(13);
            tInfo.setTextColor(Color.DKGRAY);
            tInfo.setPadding(0, 4, 0, 0);

            details.addView(tName);
            details.addView(tInfo);

            Button btnDel = new Button(this);
            btnDel.setText("🗑️ Hatayein");
            btnDel.setTextSize(12);
            btnDel.setTextColor(Color.WHITE);
            btnDel.setBackgroundColor(Color.parseColor("#DC2626"));
            btnDel.setOnClickListener(v -> confirmDeleteCrop(cropId, cropName));

            row.addView(details);
            row.addView(btnDel);
            card.addView(row);
            layoutMyCropsList.addView(card);
        }
    }

    private void confirmDeleteCrop(String cropId, String cropName) {
        new AlertDialog.Builder(this)
                .setTitle("Fasal Hatayein")
                .setMessage("Kya aap sachmuch '" + cropName + "' ko listing se hatana chahte hain?")
                .setPositiveButton("Haan", (d, w) -> deleteCropFromBackend(cropId))
                .setNegativeButton("Radd", null)
                .show();
    }

    private void deleteCropFromBackend(String cropId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL("https://mera-kisan-backend.vercel.app/api/add-crop");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("action", "delete");
                payload.put("crop_id", cropId);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.close();
                conn.getResponseCode();
            } catch (Exception ignored) {}

            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(this, "Fasal hata di gayi!", Toast.LENGTH_SHORT).show();
                loadMyCrops();
            });
        });
    }

    private void loadFarmerOrders() {
        txtOrdersCount.setText("Order load ho rahe hain...");
        layoutOrdersList.removeAllViews();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL("https://mera-kisan-backend.vercel.app/api/orders?farmer_phone=" + farmerPhone);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject res = new JSONObject(sb.toString());
                JSONArray arr = res.optJSONArray("orders");
                ArrayList<JSONObject> list = new ArrayList<>();
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) list.add(arr.getJSONObject(i));
                }

                new Handler(Looper.getMainLooper()).post(() -> renderOrdersUI(list));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> txtOrdersCount.setText("Order load nahi ho sake."));
            }
        });
    }

    private void renderOrdersUI(ArrayList<JSONObject> list) {
        layoutOrdersList.removeAllViews();
        txtOrdersCount.setText("Kul praapt order: " + list.size());

        if (list.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Abhi koi naya order nahi aaya hai.");
            empty.setTextColor(Color.GRAY);
            empty.setPadding(10, 20, 10, 20);
            layoutOrdersList.addView(empty);
            return;
        }

        for (JSONObject o : list) {
            String orderId = o.optString("order_id");
            String crop = o.optString("crop_name");
            String qty = o.optString("order_qty_kg");
            String buyerName = o.optString("buyer_name");
            String buyerPhone = o.optString("buyer_phone");
            String total = o.optString("total_amount");
            String escrow = o.optString("farmer_escrow_hold");
            boolean farmerConfirmed = o.optBoolean("farmer_confirmed", false);

            CardView card = new CardView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 10, 0, 10);
            card.setLayoutParams(lp);
            card.setRadius(14);
            card.setCardElevation(3);
            card.setCardBackgroundColor(Color.WHITE);

            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(20, 20, 20, 20);

            TextView tTitle = new TextView(this);
            tTitle.setText("🌾 " + crop + " (" + qty + " kg)");
            tTitle.setTextSize(16);
            tTitle.setTextColor(Color.parseColor("#0F172A"));
            tTitle.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tBuyer = new TextView(this);
            tBuyer.setText("👤 Khareedaar: " + buyerName + " | Kul sauda: ₹" + total + "\n🔒 8% Token Hold: ₹" + escrow);
            tBuyer.setTextSize(13);
            tBuyer.setTextColor(Color.DKGRAY);
            tBuyer.setPadding(0, 6, 0, 8);

            LinearLayout btnRow = new LinearLayout(this);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);

            Button btnCall = new Button(this);
            btnCall.setText("📞 Call");
            btnCall.setBackgroundColor(Color.parseColor("#0284C7"));
            btnCall.setTextColor(Color.WHITE);
            btnCall.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            btnCall.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+91" + buyerPhone))));

            Button btnWa = new Button(this);
            btnWa.setText("💬 WhatsApp");
            btnWa.setBackgroundColor(Color.parseColor("#25D366"));
            btnWa.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams waLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            waLp.setMargins(8, 0, 0, 0);
            btnWa.setLayoutParams(waLp);
            btnWa.setOnClickListener(v -> {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://wa.me/91" + buyerPhone + "?text=Namaste, order ke bare me baat karni hai."));
                startActivity(i);
            });

            btnRow.addView(btnCall);
            btnRow.addView(btnWa);

            Button btnConfirmDelivery = new Button(this);
            btnConfirmDelivery.setText(farmerConfirmed ? "✓ Maal saunp diya gaya hai" : "✅ Maal saunp diya (Delivery Pushthi)");
            btnConfirmDelivery.setEnabled(!farmerConfirmed);
            btnConfirmDelivery.setBackgroundColor(farmerConfirmed ? Color.GRAY : Color.parseColor("#166534"));
            btnConfirmDelivery.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cLp.setMargins(0, 10, 0, 0);
            btnConfirmDelivery.setLayoutParams(cLp);
            btnConfirmDelivery.setOnClickListener(v -> confirmDeliveryAction(orderId));

            box.addView(tTitle);
            box.addView(tBuyer);
            box.addView(btnRow);
            box.addView(btnConfirmDelivery);
            card.addView(box);
            layoutOrdersList.addView(card);
        }
    }

    private void confirmDeliveryAction(String orderId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL("https://mera-kisan-backend.vercel.app/api/orders");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("action", "confirm_delivery");
                payload.put("order_id", orderId);
                payload.put("confirmed_by", "farmer");

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.close();
                conn.getResponseCode();
            } catch (Exception ignored) {}

            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(this, "Delivery pushthi darj ho gayi!", Toast.LENGTH_SHORT).show();
                loadFarmerOrders();
            });
        });
    }

    private void saveUpiId() {
        String upi = edtFarmerUpi.getText().toString().trim();
        if (upi.isEmpty() || !upi.contains("@")) {
            Toast.makeText(this, "Valid UPI ID darj karein (jaise 9876543210@upi)", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString("farmer_upi", upi).apply();
        farmerUpi = upi;
        Toast.makeText(this, "Bank UPI ID surakshit ho gayi!", Toast.LENGTH_SHORT).show();
    }

    private void shareDigitalStore() {
        String storeText = "🌱 *Namaste! Meri digital kisan dukaan par padharein:*\n\n"
                + "👨‍🌾 *Kisan:* " + farmerName + "\n"
                + "📍 *Sthan:* " + farmerVillage + "\n"
                + "🌾 *Khet se seedhe 100% shuddh faslein uplabdh hain!*\n\n"
                + "👉 *Live faslein dekhein:* https://mera-kisan-backend.vercel.app/api/crops\n\n"
                + "📞 Seedha Sampark / WhatsApp: +91 " + farmerPhone;

        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, storeText);
        i.setPackage("com.whatsapp");
        try {
            startActivity(i);
        } catch (Exception e) {
            startActivity(Intent.createChooser(i, "Dukaan link share karein:"));
        }
    }

    private void sharePosterToApp(String pkg) {
        if (generatedPosterBitmap == null) {
            Toast.makeText(this, "Pehle fasal save karke poster banayein", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String path = MediaStore.Images.Media.insertImage(getContentResolver(), generatedPosterBitmap, "CropPoster", "Mera Kisan Poster");
            Uri imageUri = Uri.parse(path);

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/jpeg");
            share.putExtra(Intent.EXTRA_STREAM, imageUri);
            share.putExtra(Intent.EXTRA_TEXT, generatedShareText);
            share.setPackage(pkg);
            try {
                startActivity(share);
            } catch (Exception e) {
                startActivity(Intent.createChooser(share, "Poster share karein:"));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Share truti: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
