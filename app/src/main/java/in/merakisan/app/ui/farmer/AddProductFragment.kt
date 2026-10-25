// app/src/main/java/in/merakisan/app/ui/farmer/AddProductFragment.kt
package `in`.merakisan.app.ui.farmer

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import `in`.merakisan.app.core.config.FeatureManager
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.security.SessionManager
import `in`.merakisan.app.ui.main.MainActivity
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * MERA KISAN — ADD / EDIT PRODUCT & CROP SCREEN
 * Exact Repository Path: app/src/main/java/in/merakisan/app/ui/farmer/AddProductFragment.kt
 *
 * Implements (Master Spec Sec 6, 12, 13 & merged.txt Sec 8, 37):
 * - Complete 10-step listing & editing flow.
 * - Small quantity marketplace support (1-25 kg).
 * - Separate Seeds, Cold Pressed Oil, Rare/Desi & Traditional food categories.
 * - Auto Crop Photo matching by crop name via ApiClient.getCropImageByName.
 * - Fair Price Calculator integration (`/modules/fair-price`).
 * - Google Drive photo metadata & YouTube (6 videos/day + Firebase queue) video job registration.
 */
class AddProductFragment : Fragment() {

    private var editProductId: String? = null
    private var resolvedCropImageUrl: String? = null

    private lateinit var screenHeaderTitle: TextView
    private lateinit var inputProductName: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var inputVariety: EditText
    private lateinit var inputPriceRupees: EditText
    private lateinit var spinnerUnit: Spinner
    private lateinit var inputStockQty: EditText
    private lateinit var inputMinOrderQty: EditText
    private lateinit var inputHarvestDate: EditText
    private lateinit var checkFreshToday: CheckBox
    private lateinit var inputDescription: EditText
    private lateinit var inputVillage: EditText
    private lateinit var inputDistrict: EditText
    private lateinit var inputState: EditText
    private lateinit var tvPhotoStatus: TextView
    private lateinit var inputCustomImageUrl: EditText
    private lateinit var inputVideoTempPath: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var btnPublishProduct: Button

    private val categoryOptions = listOf(
        "grains" to "🌾 देशी अनाज व मिलेट्स (Grains & Millets)",
        "pulses_oilseeds" to "🥜 दाल व तिलहन (Pulses & Oilseeds)",
        "vegetables" to "🥬 ताज़ा सब्जियां (Fresh Vegetables)",
        "fruits" to "🍊 देशी फल (Fruits)",
        "cold_pressed_oil" to "🫙 कच्ची घानी तेल (Cold Pressed Oil)",
        "spices_jaggery" to "🍯 गुड़ व मसाले (Jaggery & Spices)",
        "seeds" to "🌱 देशी व प्रमाणित बीज (Seeds)",
        "traditional" to "🏺 दुर्लभ / पारंपरिक किस्म (Rare / Desi)",
        "other" to "📦 अन्य कृषि उत्पाद"
    )

    private val unitOptions = listOf("kg", "quintal", "litre", "packet", "piece", "box")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editProductId = arguments?.getString("edit_product_id")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val scroll = ScrollView(ctx).apply {
            isFillViewport = true
            setBackgroundColor(Color.parseColor("#F2F6F2"))
        }

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(30))
        }

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#FFFFFF", "#D4E2D4", dp(14))
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        screenHeaderTitle = TextView(ctx).apply {
            text = if (editProductId.isNullOrBlank()) {
                "➕ नई फसल / बीज / कृषि उत्पाद बेचें"
            } else {
                "✏️ फसल विवरण व स्टॉक एडिट करें"
            }
            setTextColor(Color.parseColor("#124116"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(6))
        }
        card.addView(screenHeaderTitle)

        card.addView(
            TextView(ctx).apply {
                text = "छोटी मात्रा (1–25 kg) या थोक मात्रा दोनों सीधे ग्राहकों को बेचें। बिना प्रमाण '100% केमिकल-मुक्त' या '10 गुना शुद्ध' जैसे दावे न लिखें।"
                setTextColor(Color.parseColor("#4A5A4A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, 0, 0, dp(10))
            }
        )

        inputProductName = addLabeledInput(
            parent = card,
            label = "1. फसल या उत्पाद का नाम (Product Name) *",
            hint = "उदा. शरबती गेहूं, ऊटी लहसुन, देशी चीना, मूंगफली तेल...",
            inputType = InputType.TYPE_CLASS_TEXT,
            maxLen = 100
        )

        card.addView(createLabel("2. श्रेणी चुनें (Category) *"))
        spinnerCategory = Spinner(ctx).apply {
            minimumHeight = dp(46)
            adapter = ArrayAdapter(
                ctx,
                android.R.layout.simple_spinner_dropdown_item,
                categoryOptions.map { it.second }
            )
        }
        card.addView(spinnerCategory)

        inputVariety = addLabeledInput(
            parent = card,
            label = "3. किस्म / वैरायटी (Variety)",
            hint = "उदा. शरबती, देशी, लोकवन, ऊटी, पीली सरसों...",
            inputType = InputType.TYPE_CLASS_TEXT,
            maxLen = 80
        )

        inputPriceRupees = addLabeledInput(
            parent = card,
            label = "4. विक्रय मूल्य (₹ प्रति इकाई) *",
            hint = "उदा. 45",
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
            maxLen = 10
        )

        if (FeatureManager.isEnabled("fair_price", ctx)) {
            card.addView(
                createButton("🧮 उचित दाम कैलकुलेटर (लागत व भाड़े से सही भाव निकालें)", "#0D47A1") {
                    showFairPriceCalculatorDialog()
                }
            )
        }

        card.addView(createLabel("5. माप की इकाई (Unit) *"))
        spinnerUnit = Spinner(ctx).apply {
            minimumHeight = dp(46)
            adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, unitOptions)
        }
        card.addView(spinnerUnit)

        inputStockQty = addLabeledInput(
            parent = card,
            label = "6. कुल उपलब्ध मात्रा (Available Stock) *",
            hint = "उदा. 25 (छोटी मात्रा भी डाल सकते हैं)",
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
            maxLen = 10
        )

        inputMinOrderQty = addLabeledInput(
            parent = card,
            label = "7. ग्राहक के लिए न्यूनतम ऑर्डर मात्रा (Minimum Order) *",
            hint = "उदा. 2 (1 से 25 रखने पर 'छोटी मात्रा' बैज मिलेगा)",
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
            maxLen = 10
        ).apply {
            setText("1")
        }

        inputHarvestDate = addLabeledInput(
            parent = card,
            label = "8. कटाई / तुड़ाई तिथि (Harvest Date)",
            hint = "उदा. आज, कल, या अक्टूबर 2026",
            inputType = InputType.TYPE_CLASS_TEXT,
            maxLen = 40
        )

        checkFreshToday = CheckBox(ctx).apply {
            text = "🥬 यह आज खेत से तोड़ी गई ताज़ा फसल है (Fresh Today)"
            setTextColor(Color.parseColor("#1B5E20"))
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(6), 0, dp(6))
        }
        card.addView(checkFreshToday)

        inputDescription = addLabeledInput(
            parent = card,
            label = "9. उत्पाद विवरण व खेती की जानकारी (Description)",
            hint = "स्वाद, गुणवत्ता, बुवाई/खाद की वास्तविक जानकारी लिखें...",
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            maxLen = 500
        )

        card.addView(createLabel("10. फसल की फोटो व वीडियो (Auto Crop Photo / Google Drive / YouTube Queue)"))
        val btnAutoPhoto = createButton("🖼️ फसल के नाम से असली फोटो अपने-आप जोड़ें", "#2E7D32") {
            autoMatchCropPhotoByName()
        }
        card.addView(btnAutoPhoto)

        tvPhotoStatus = TextView(ctx).apply {
            text = "फोटो स्थिति: फसल का नाम लिखकर ऊपर बटन दबाएँ (या नीचे लिंक दें)"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, dp(4), 0, dp(4))
        }
        card.addView(tvPhotoStatus)

        inputCustomImageUrl = addLabeledInput(
            parent = card,
            label = "Google Drive / कस्टम फोटो HTTPS लिंक (वैकल्पिक)",
            hint = "खाली छोड़ने पर फसल के नाम से ऑटो-फोटो जुड़ जाएगी",
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI,
            maxLen = 400
        )

        inputVideoTempPath = addLabeledInput(
            parent = card,
            label = "15–30 सेकंड फसल वीडियो संदर्भ (YouTube 6/दिन कोटा कतार - वैकल्पिक)",
            hint = "वीडियो पाथ या संदर्भ (वैकल्पिक)",
            inputType = InputType.TYPE_CLASS_TEXT,
            maxLen = 250
        )

        inputVillage = addLabeledInput(
            parent = card,
            label = "गाँव का नाम",
            hint = "गाँव",
            inputType = InputType.TYPE_CLASS_TEXT,
            maxLen = 80
        ).apply {
            setText(SessionManager.getUserVillage(ctx))
        }

        inputDistrict = addLabeledInput(
            parent = card,
            label = "ज़िला (District) *",
            hint = "ज़िला",
            inputType = InputType.TYPE_CLASS_TEXT,
            maxLen = 80
        ).apply {
            setText(SessionManager.getUserDistrict(ctx))
        }

        inputState = addLabeledInput(
            parent = card,
            label = "राज्य (State)",
            hint = "राज्य",
            inputType = InputType.TYPE_CLASS_TEXT,
            maxLen = 80
        ).apply {
            setText(SessionManager.getUserState(ctx))
        }

        progressBar = ProgressBar(ctx).apply {
            visibility = View.GONE
            setPadding(0, dp(10), 0, dp(10))
        }
        card.addView(progressBar)

        btnPublishProduct = createButton(
            if (editProductId.isNullOrBlank()) "✅ फसल प्रकाशित करें और पोस्टर बनाएँ" else "💾 बदलाव सुरक्षित करें",
            "#1B5E20"
        ) {
            submitProductListing()
        }.apply {
            minHeight = dp(52)
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(12)
            layoutParams = lp
        }
        card.addView(btnPublishProduct)

        root.addView(card)
        scroll.addView(root)
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!editProductId.isNullOrBlank()) {
            loadExistingProductForEdit(editProductId!!)
        }
    }

    private fun loadExistingProductForEdit(productId: String) {
        val ctx = context ?: return
        progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.getProductById(ctx, productId)
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val d = res.dataObject ?: return@launch
            inputProductName.setText(d.optString("name", ""))
            inputVariety.setText(d.optString("variety", ""))
            inputPriceRupees.setText(d.optDouble("price_rupees", d.optDouble("price", 0.0)).toString())
            inputStockQty.setText(d.optDouble("stock_quantity", 0.0).toString())
            inputMinOrderQty.setText(d.optDouble("min_order_quantity", 1.0).toString())
            inputHarvestDate.setText(d.optString("harvest_date", "").takeIf { it != "null" } ?: "")
            checkFreshToday.isChecked = d.optBoolean("is_fresh_today", false)
            inputDescription.setText(d.optString("description", ""))
            inputVillage.setText(d.optString("village", "").takeIf { it != "null" } ?: "")
            inputDistrict.setText(d.optString("district", "").takeIf { it != "null" } ?: "")
            inputState.setText(d.optString("state", "").takeIf { it != "null" } ?: "")

            val existingImg = d.optString("image_url", "").takeIf { it != "null" } ?: ""
            if (existingImg.isNotBlank()) {
                resolvedCropImageUrl = existingImg
                inputCustomImageUrl.setText(existingImg)
                tvPhotoStatus.text = "✅ फोटो पहले से जुड़ी हुई है"
            }

            val catCode = d.optString("category", "grains")
            val catIdx = categoryOptions.indexOfFirst { it.first == catCode }
            if (catIdx >= 0) spinnerCategory.setSelection(catIdx)

            val unitVal = d.optString("unit", "kg")
            val uIdx = unitOptions.indexOf(unitVal)
            if (uIdx >= 0) spinnerUnit.setSelection(uIdx)
        }
    }

    private fun autoMatchCropPhotoByName() {
        val ctx = context ?: return
        val name = inputProductName.text?.toString()?.trim() ?: ""
        if (name.length < 2) {
            Toast.makeText(ctx, "कृपया पहले फसल का नाम लिखें।", Toast.LENGTH_SHORT).show()
            return
        }
        val catCode = categoryOptions.getOrNull(spinnerCategory.selectedItemPosition)?.first ?: "grains"

        progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.getCropImageByName(ctx, name, catCode)
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val imgUrl = res.dataObject?.optString("image_url", "") ?: ""
            val titleHi = res.dataObject?.optString("title_hi", name) ?: name
            if (imgUrl.isNotBlank()) {
                resolvedCropImageUrl = imgUrl
                inputCustomImageUrl.setText(imgUrl)
                tvPhotoStatus.text = "✅ '$titleHi' की उच्च-गुणवत्ता वाली असली फसल फोटो जुड़ गई!"
            } else {
                tvPhotoStatus.text = "⚠️ फोटो स्वतः नहीं मिली, प्रकाशन पर डिफ़ॉल्ट कृषि फोटो जुड़ जाएगी।"
            }
        }
    }

    private fun showFairPriceCalculatorDialog() {
        val ctx = context ?: return
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }

        val qtyInput = EditText(ctx).apply {
            hint = "कुल मात्रा (kg में)"
            setText(inputStockQty.text?.toString()?.ifBlank { "100" } ?: "100")
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val prodCostInput = EditText(ctx).apply {
            hint = "कुल उत्पादन लागत (₹)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val transportInput = EditText(ctx).apply {
            hint = "परिवहन / भाड़ा खर्च (₹)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val packInput = EditText(ctx).apply {
            hint = "पैकेजिंग खर्च (₹)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val marginInput = EditText(ctx).apply {
            hint = "अपेक्षित मुनाफा % (उदा. 15)"
            setText("15")
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        layout.addView(qtyInput)
        layout.addView(prodCostInput)
        layout.addView(transportInput)
        layout.addView(packInput)
        layout.addView(marginInput)

        AlertDialog.Builder(ctx)
            .setTitle("🧮 उचित दाम कैलकुलेटर (Fair Price)")
            .setView(layout)
            .setPositiveButton("भाव निकालें") { _, _ ->
                val body = JSONObject().apply {
                    put("quantity_kg", qtyInput.text?.toString()?.toDoubleOrNull() ?: 100.0)
                    put("production_cost_rupees", prodCostInput.text?.toString()?.toDoubleOrNull() ?: 0.0)
                    put("transport_cost_rupees", transportInput.text?.toString()?.toDoubleOrNull() ?: 0.0)
                    put("packaging_cost_rupees", packInput.text?.toString()?.toDoubleOrNull() ?: 0.0)
                    put("desired_margin_percent", marginInput.text?.toString()?.toDoubleOrNull() ?: 15.0)
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val res = ApiClient.request(ctx, "/modules/fair-price", method = "POST", bodyJson = body)
                    val d = res.dataObject
                    if (res.isSuccess && d != null) {
                        val suggestedKg = d.optDouble("suggested_direct_price_kg_rupees", 0.0)
                        val minSustKg = d.optDouble("minimum_sustainable_price_kg_rupees", 0.0)
                        if (suggestedKg > 0) {
                            inputPriceRupees.setText(suggestedKg.toString())
                        }
                        Toast.makeText(
                            ctx,
                            "न्यूनतम लागत मूल्य: ₹$minSustKg/kg • सुझाया गया प्रत्यक्ष भाव: ₹$suggestedKg/kg",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("बंद करें", null)
            .show()
    }

    private fun submitProductListing() {
        val ctx = context ?: return
        val name = inputProductName.text?.toString()?.trim() ?: ""
        if (name.length < 2) {
            inputProductName.error = "फसल का नाम लिखें"
            Toast.makeText(ctx, "कृपया फसल या उत्पाद का नाम दर्ज करें।", Toast.LENGTH_SHORT).show()
            return
        }

        val priceRupees = inputPriceRupees.text?.toString()?.toDoubleOrNull() ?: 0.0
        if (priceRupees < 1.0) {
            inputPriceRupees.error = "मान्य कीमत लिखें"
            Toast.makeText(ctx, "कृपया कम से कम ₹1 या अधिक कीमत दर्ज करें।", Toast.LENGTH_SHORT).show()
            return
        }

        val stockQty = inputStockQty.text?.toString()?.toDoubleOrNull() ?: 0.0
        if (stockQty <= 0.0) {
            inputStockQty.error = "उपलब्ध मात्रा लिखें"
            Toast.makeText(ctx, "कृपया उपलब्ध स्टॉक मात्रा दर्ज करें।", Toast.LENGTH_SHORT).show()
            return
        }

        val minOrderQty = inputMinOrderQty.text?.toString()?.toDoubleOrNull() ?: 1.0
        if (minOrderQty <= 0.0 || minOrderQty > stockQty) {
            inputMinOrderQty.error = "न्यूनतम ऑर्डर कुल स्टॉक से अधिक नहीं हो सकता"
            Toast.makeText(ctx, "न्यूनतम ऑर्डर मात्रा कुल स्टॉक के बराबर या कम होनी चाहिए।", Toast.LENGTH_SHORT).show()
            return
        }

        val district = inputDistrict.text?.toString()?.trim() ?: ""
        if (district.length < 2) {
            inputDistrict.error = "ज़िला अनिवार्य है"
            Toast.makeText(ctx, "कृपया अपना ज़िला दर्ज करें।", Toast.LENGTH_SHORT).show()
            return
        }

        val catCode = categoryOptions.getOrNull(spinnerCategory.selectedItemPosition)?.first ?: "grains"
        val unitVal = unitOptions.getOrNull(spinnerUnit.selectedItemPosition) ?: "kg"
        val pricePaise = Math.round(priceRupees * 100)

        progressBar.visibility = View.VISIBLE
        btnPublishProduct.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            var finalImageUrl = inputCustomImageUrl.text?.toString()?.trim().takeIf { !it.isNullOrBlank() }
                ?: resolvedCropImageUrl

            if (finalImageUrl.isNullOrBlank()) {
                val imgRes = ApiClient.getCropImageByName(ctx, name, catCode)
                finalImageUrl = imgRes.dataObject?.optString("image_url", null)
            }

            val payload = JSONObject().apply {
                put("name", name)
                put("category", catCode)
                put("variety", inputVariety.text?.toString()?.trim() ?: "")
                put("price_paise", pricePaise)
                put("price", priceRupees)
                put("unit", unitVal)
                put("stock_quantity", stockQty)
                put("min_order_quantity", minOrderQty)
                put("harvest_date", inputHarvestDate.text?.toString()?.trim() ?: "")
                put("is_fresh_today", checkFreshToday.isChecked)
                put("description", inputDescription.text?.toString()?.trim() ?: "")
                put("village", inputVillage.text?.toString()?.trim() ?: "")
                put("district", district)
                put("state", inputState.text?.toString()?.trim() ?: "")
                if (!finalImageUrl.isNullOrBlank()) {
                    put("image_url", finalImageUrl)
                }
            }

            val res = if (editProductId.isNullOrBlank()) {
                ApiClient.createProduct(ctx, payload)
            } else {
                ApiClient.updateProduct(ctx, editProductId!!, payload)
            }

            if (!isAdded) return@launch

            if (res.isSuccess && res.dataObject != null) {
                val savedId = res.dataObject.optString("id", editProductId ?: "")

                val videoPath = inputVideoTempPath.text?.toString()?.trim() ?: ""
                if (videoPath.isNotBlank() && savedId.isNotBlank()) {
                    val vidBody = JSONObject().apply {
                        put("product_id", savedId)
                        put("firebase_path", videoPath)
                    }
                    ApiClient.request(ctx, "/media/videos/enqueue", method = "POST", bodyJson = vidBody)
                }

                progressBar.visibility = View.GONE
                btnPublishProduct.isEnabled = true

                Toast.makeText(
                    ctx,
                    "आपकी फसल सफलतापूर्वक बाज़ार में प्रकाशित हो गई! अब आप इसका .JPG पोस्टर शेयर कर सकते हैं।",
                    Toast.LENGTH_LONG
                ).show()

                if (savedId.isNotBlank()) {
                    (activity as? MainActivity)?.openProductDetailScreen(savedId)
                }
            } else {
                progressBar.visibility = View.GONE
                btnPublishProduct.isEnabled = true
                Toast.makeText(
                    ctx,
                    res.errorMessage ?: "फसल सेव करने में त्रुटि हुई। कृपया पुनः प्रयास करें।",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun createLabel(textStr: String): TextView =
        TextView(requireContext()).apply {
            text = textStr
            setTextColor(Color.parseColor("#1A241A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(10), 0, dp(4))
        }

    private fun addLabeledInput(
        parent: LinearLayout,
        label: String,
        hint: String,
        inputType: Int,
        maxLen: Int
    ): EditText {
        val ctx = requireContext()
        parent.addView(createLabel(label))
        val edit = EditText(ctx).apply {
            this.hint = hint
            this.inputType = inputType
            filters = arrayOf(InputFilter.LengthFilter(maxLen))
            background = roundedBg("#FAFAFA", "#B8CBB8", dp(8))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            minHeight = dp(46)
        }
        parent.addView(edit)
        return edit
    }

    private fun createButton(label: String, bgHex: String, onClick: () -> Unit): Button =
        Button(requireContext()).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            background = roundedBg(bgHex, bgHex, dp(10))
            minHeight = dp(44)
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(6)
            layoutParams = lp
            setOnClickListener { onClick() }
        }

    private fun roundedBg(fillHex: String, strokeHex: String, radiusPx: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx.toFloat()
            setColor(Color.parseColor(fillHex))
            setStroke(dp(1), Color.parseColor(strokeHex))
        }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
}
