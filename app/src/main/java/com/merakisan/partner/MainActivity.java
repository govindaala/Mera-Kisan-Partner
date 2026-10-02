// File Path: app/src/main/java/com/merakisan/partner/MainActivity.java
package com.merakisan.partner;

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
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String PREF_NAME = "MeraKisanPartnerPrefs";
    private static final int VOICE_REQUEST_CODE = 102;

    private TextView txtFarmerHeader, txtVoiceStatus;
    private CardView cardPendingAlert, cardPosterSection;
    private EditText edtCropName, edtPrice, edtQty, edtQualityTag, edtDescription;
    private Spinner spnCategory, spnFarmingType;
    private Button btnVoiceInput, btnSubmitCrop, btnShareWhatsApp, btnShareFacebook;
    private ImageView imgPosterPreview;
    private ProgressBar progressBar;

    private String farmerName = "किसान साथी";
    private String farmerPhone = "";
    private String farmerVillage = "";
    private Bitmap generatedPosterBitmap = null;
    private String generatedShareText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtFarmerHeader = findViewById(R.id.txtFarmerHeader);
        txtVoiceStatus = findViewById(R.id.txtVoiceStatus);
        cardPendingAlert = findViewById(R.id.cardPendingAlert);
        cardPosterSection = findViewById(R.id.cardPosterSection);

        edtCropName = findViewById(R.id.edtCropName);
        edtPrice = findViewById(R.id.edtPrice);
        edtQty = findViewById(R.id.edtQty);
        edtQualityTag = findViewById(R.id.edtQualityTag);
        edtDescription = findViewById(R.id.edtDescription);

        spnCategory = findViewById(R.id.spnCategory);
        spnFarmingType = findViewById(R.id.spnFarmingType);

        btnVoiceInput = findViewById(R.id.btnVoiceInput);
        btnSubmitCrop = findViewById(R.id.btnSubmitCrop);
        btnShareWhatsApp = findViewById(R.id.btnShareWhatsApp);
        btnShareFacebook = findViewById(R.id.btnShareFacebook);
        imgPosterPreview = findViewById(R.id.imgPosterPreview);
        progressBar = findViewById(R.id.progressBar);

        // प्रोफ़ाइल विवरण लोड करना
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        farmerName = prefs.getString("farmer_name", "किसान साथी");
        farmerPhone = prefs.getString("farmer_phone", "");
        farmerVillage = prefs.getString("village", "गाँव");
        String accountStatus = prefs.getString("account_status", "pending");

        txtFarmerHeader.setText("👨‍🌾 " + farmerName + " (" + farmerVillage + ")");

        // अगर एडमिन से अप्रूव हो चुका है, तो पीला अलर्ट छिपाएँ
        if ("approved".equalsIgnoreCase(accountStatus)) {
            cardPendingAlert.setVisibility(View.GONE);
        } else {
            cardPendingAlert.setVisibility(View.VISIBLE);
        }

        setupSpinners();

        // वॉइस इनपुट बटन
        btnVoiceInput.setOnClickListener(v -> startVoiceRecognition());

        // फ़सल सेव व पोस्टर जेनरेशन
        btnSubmitCrop.setOnClickListener(v -> submitCropAndGeneratePoster());

        // WhatsApp शेयर
        btnShareWhatsApp.setOnClickListener(v -> shareToSocial("com.whatsapp"));

        // Facebook शेयर
        btnShareFacebook.setOnClickListener(v -> shareToSocial("com.facebook.katana"));
    }

    private void setupSpinners() {
        String[] categories = {"अनाज (Grain)", "दालें (Pulses)", "तिलहन (Oilseeds)", "मसाले (Spices)", "फल (Fruits)", "सब्जियां (Vegetables)"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spnCategory.setAdapter(catAdapter);

        String[] types = {"100% जैविक (Organic)", "प्राकृतिक / बिना रासायनिक खाद", "सामान्य (कम केमिकल)"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spnFarmingType.setAdapter(typeAdapter);
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "फसल का नाम, भाव और मात्रा बोलें...");
        try {
            startActivityForResult(intent, VOICE_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "माइक सपोर्ट उपलब्ध नहीं है", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) {
                String spokenText = matches.get(0);
                txtVoiceStatus.setText("सुना गया: \"" + spokenText + "\"");
                parseSpokenText(spokenText);
            }
        }
    }

    // बोले गए वाक्य से भाव, मात्रा और फ़सल का नाम अलग करना
    private void parseSpokenText(String text) {
        // भाव ढूँढना (जैसे: 38 रुपये)
        Pattern pricePattern = Pattern.compile("(\\d+)\\s*(रुपये|रु|रूपए|भाव)");
        Matcher priceMatcher = pricePattern.matcher(text);
        if (priceMatcher.find()) {
            edtPrice.setText(priceMatcher.group(1));
        }

        // मात्रा ढूँढना (जैसे: 20 क्विंटल या 500 किलो)
        Pattern qtyPattern = Pattern.compile("(\\d+)\\s*(क्विंटल|बोरी|किलो|kg)");
        Matcher qtyMatcher = qtyPattern.matcher(text);
        if (qtyMatcher.find()) {
            String qty = qtyMatcher.group(1);
            if (text.contains("क्विंटल")) {
                try {
                    int inKg = Integer.parseInt(qty) * 100;
                    edtQty.setText(String.valueOf(inKg));
                } catch (Exception e) {
                    edtQty.setText(qty);
                }
            } else {
                edtQty.setText(qty);
            }
        }

        // फ़सल नाम सेट करना
        String cropGuess = text.replaceAll("(\\d+)\\s*(रुपये|रु|रूपए|भाव|क्विंटल|बोरी|किलो|kg)", "").trim();
        if (!cropGuess.isEmpty()) {
            edtCropName.setText(cropGuess);
        }
    }

    private void submitCropAndGeneratePoster() {
        String crop = edtCropName.getText().toString().trim();
        String price = edtPrice.getText().toString().trim();
        String qty = edtQty.getText().toString().trim();
        String qualityTag = edtQualityTag.getText().toString().trim();
        String desc = edtDescription.getText().toString().trim();
        String farmingType = spnFarmingType.getSelectedItem().toString();

        if (crop.isEmpty() || price.isEmpty()) {
            Toast.makeText(this, "कृपया फ़सल का नाम और भाव ज़रूर भरें!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (qualityTag.isEmpty()) qualityTag = "Grade A, सुपर बोल्ड दाना";
        if (desc.isEmpty()) desc = "खेत से ताज़ा कटाई, बिना किसी मिलावट के शुद्ध उत्पाद!";

        final String finalQuality = qualityTag;
        final String finalDesc = desc;

        progressBar.setVisibility(View.VISIBLE);
        btnSubmitCrop.setEnabled(false);

        // 1. नेटिव कैनवास से पोस्टर तैयार करना
        generatedPosterBitmap = drawNativePoster(crop, price, farmingType, finalQuality, finalDesc);
        imgPosterPreview.setImageBitmap(generatedPosterBitmap);

        // 2. बैकएंड API पर सेव करना
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
                payload.put("description", finalDesc);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.close();

                conn.getResponseCode();
            } catch (Exception ignored) {}

            generatedShareText = "🌾 *खेत से सीधे आपके घर — 100% शुद्ध!*\n\n"
                    + "🌱 *फ़सल:* " + crop + "\n"
                    + "🏷️ *प्रकार:* " + farmingType + "\n"
                    + "⭐ *गुणवत्ता:* " + finalQuality + "\n"
                    + "💰 *भाव:* मात्र ₹" + price + "/किलो\n"
                    + "👨‍🌾 *किसान:* " + farmerName + " (" + farmerVillage + ")\n\n"
                    + "📝 " + finalDesc + "\n\n"
                    + "👉 *Mera Kisan ऐप डाउनलोड करके सीधे आर्डर करें:*\n"
                    + "🔗 https://mera-kisan-backend.vercel.app\n"
                    + "📞 *सीधे कॉल/WhatsApp:* +91 " + farmerPhone;

            new Handler(Looper.getMainLooper()).post(() -> {
                progressBar.setVisibility(View.GONE);
                btnSubmitCrop.setEnabled(true);
                cardPosterSection.setVisibility(View.VISIBLE);
                Toast.makeText(this, "पोस्टर तैयार हो गया!", Toast.LENGTH_SHORT).show();
            });
        });
    }

    // Android Native Canvas: शुद्ध हिंदी व सटीक फ़ॉन्ट वाला पोस्टर जनरेटर
    private Bitmap drawNativePoster(String crop, String price, String type, String quality, String desc) {
        int width = 900;
        int height = 1200;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // 1. बैकग्राउंड
        canvas.drawColor(Color.parseColor("#F9FBF9"));

        // 2. टॉप हेडर बार
        Paint headerBar = new Paint();
        headerBar.setColor(Color.parseColor("#1B5E20"));
        canvas.drawRect(0, 0, width, 140, headerBar);

        Paint headerText = new Paint();
        headerText.setColor(Color.WHITE);
        headerText.setTextSize(42);
        headerText.setFakeBoldText(true);
        headerText.setAntiAlias(true);
        canvas.drawText("🌱 Mera Kisan | खेत से सीधे आपके घर", 60, 85, headerText);

        // 3. किसान जानकारी पट्टी
        Paint farmerBar = new Paint();
        farmerBar.setColor(Color.parseColor("#2E7D32"));
        RectF farmerRect = new RectF(50, 170, width - 50, 260);
        canvas.drawRoundRect(farmerRect, 20, 20, farmerBar);

        Paint farmerText = new Paint();
        farmerText.setColor(Color.WHITE);
        farmerText.setTextSize(36);
        farmerText.setFakeBoldText(true);
        farmerText.setAntiAlias(true);
        canvas.drawText("👨‍🌾 किसान: " + farmerName + " (" + farmerVillage + ")", 80, 228, farmerText);

        // 4. मुख्य फ़सल कार्ड
        Paint cardPaint = new Paint();
        cardPaint.setColor(Color.WHITE);
        cardPaint.setShadowLayer(10, 0, 4, Color.parseColor("#D0D0D0"));
        RectF cropCard = new RectF(50, 290, width - 50, 820);
        canvas.drawRoundRect(cropCard, 24, 24, cardPaint);

        // फ़सल का नाम
        Paint cropPaint = new Paint();
        cropPaint.setColor(Color.parseColor("#1B5E20"));
        cropPaint.setTextSize(54);
        cropPaint.setFakeBoldText(true);
        cropPaint.setAntiAlias(true);
        canvas.drawText("🌾 " + crop, 90, 380, cropPaint);

        // खेती का प्रकार (जैविक बैज)
        Paint badgePaint = new Paint();
        badgePaint.setColor(Color.parseColor("#E8F5E9"));
        RectF badgeRect = new RectF(90, 420, width - 90, 490);
        canvas.drawRoundRect(badgeRect, 16, 16, badgePaint);

        Paint badgeText = new Paint();
        badgeText.setColor(Color.parseColor("#2E7D32"));
        badgeText.setTextSize(32);
        badgeText.setFakeBoldText(true);
        badgeText.setAntiAlias(true);
        canvas.drawText("✓ " + type, 120, 468, badgeText);

        // भाव बॉक्स
        Paint priceBg = new Paint();
        priceBg.setColor(Color.parseColor("#FFF3E0"));
        RectF priceRect = new RectF(90, 520, width - 90, 630);
        canvas.drawRoundRect(priceRect, 20, 20, priceBg);

        Paint priceText = new Paint();
        priceText.setColor(Color.parseColor("#E65100"));
        priceText.setTextSize(52);
        priceText.setFakeBoldText(true);
        priceText.setAntiAlias(true);
        canvas.drawText("💰 मात्र ₹" + price + " / किलो", 130, 595, priceText);

        // गुणवत्ता विवरण
        Paint descPaint = new Paint();
        descPaint.setColor(Color.parseColor("#333333"));
        descPaint.setTextSize(30);
        descPaint.setAntiAlias(true);
        canvas.drawText("⭐ गुणवत्ता: " + quality, 90, 690, descPaint);

        // खासियत
        Paint notePaint = new Paint();
        notePaint.setColor(Color.parseColor("#555555"));
        notePaint.setTextSize(26);
        notePaint.setAntiAlias(true);
        String shortDesc = desc.length() > 45 ? desc.substring(0, 45) + "..." : desc;
        canvas.drawText("📝 खासियत: " + shortDesc, 90, 750, notePaint);

        // 5. बॉटम ऐप डाउनलोड कॉल टू एक्शन
        Paint ctaBg = new Paint();
        ctaBg.setColor(Color.parseColor("#1B5E20"));
        RectF ctaRect = new RectF(50, 860, width - 50, 1140);
        canvas.drawRoundRect(ctaRect, 24, 24, ctaBg);

        Paint ctaText1 = new Paint();
        ctaText1.setColor(Color.parseColor("#FFD54F"));
        ctaText1.setTextSize(38);
        ctaText1.setFakeBoldText(true);
        ctaText1.setAntiAlias(true);
        canvas.drawText("सीधे खेत से ताज़ा उत्पाद मंगवाने हेतु", 120, 930, ctaText1);

        Paint ctaText2 = new Paint();
        ctaText2.setColor(Color.WHITE);
        ctaText2.setTextSize(34);
        ctaText2.setFakeBoldText(true);
        ctaText2.setAntiAlias(true);
        canvas.drawText("आज ही 'Mera Kisan' ऐप डाउनलोड करें!", 120, 990, ctaText2);

        Paint ctaContact = new Paint();
        ctaContact.setColor(Color.WHITE);
        ctaContact.setTextSize(30);
        ctaContact.setAntiAlias(true);
        canvas.drawText("📞 किसान से संपर्क / ऑर्डर: +91 " + farmerPhone, 120, 1070, ctaContact);

        return bitmap;
    }

    private void shareToSocial(String targetPackage) {
        if (generatedPosterBitmap == null) {
            Toast.makeText(this, "पहले फ़सल सेव करके पोस्टर बनाएँ", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String path = MediaStore.Images.Media.insertImage(getContentResolver(), generatedPosterBitmap, "CropPoster", "Mera Kisan Poster");
            Uri imageUri = Uri.parse(path);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/jpeg");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, generatedShareText);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            shareIntent.setPackage(targetPackage);
            try {
                startActivity(shareIntent);
            } catch (Exception e) {
                // अगर डायरेक्ट ऐप इंस्टॉल न हो तो सिस्टम शेयर मेन्यू खोलें
                shareIntent.setPackage(null);
                startActivity(Intent.createChooser(shareIntent, "पोस्टर शेयर करें:"));
            }
        } catch (Exception e) {
            Toast.makeText(this, "शेयर करने में त्रुटि: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
