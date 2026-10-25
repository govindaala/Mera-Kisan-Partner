// app/src/main/java/in/merakisan/app/ui/search/SmartSearchFilterSheet.kt
package `in`.merakisan.app.ui.search

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import `in`.merakisan.app.core.config.FeatureManager
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * MERA KISAN — SMART SEARCH & MULTI-PARAMETER FILTER BOTTOM SHEET
 * Exact Repository Path: app/src/main/java/in/merakisan/app/ui/search/SmartSearchFilterSheet.kt
 *
 * Implements (Master Spec Sec 6, 16 & merged.txt Sec 11, 12, 49, 52):
 * - Category filter (including dedicated Seeds and Cold Pressed Oil).
 * - Small Quantity marketplace toggle (1-25 kg).
 * - Harvest Freshness (Fresh Today).
 * - Distance Radius filter (0-5 km, 5-10 km, 10-25 km, 25-50 km, All).
 * - Verification filter (Verified Farmers Only / Organic Certified Only).
 * - Save Search Alert CTA (`POST /modules/saved-searches`).
 */
class SmartSearchFilterSheet : BottomSheetDialogFragment() {

    interface FilterApplyListener {
        fun onFiltersApplied(
            query: String?,
            category: String?,
            minPrice: Double?,
            maxPrice: Double?,
            radiusKm: Int?,
            smallQuantityOnly: Boolean,
            freshTodayOnly: Boolean,
            verifiedOnly: Boolean,
            sortBy: String
        )
    }

    private var listener: FilterApplyListener? = null

    private lateinit var inputQuery: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var inputMinPrice: EditText
    private lateinit var inputMaxPrice: EditText
    private lateinit var spinnerRadius: Spinner
    private lateinit var checkSmallQty: CheckBox
    private lateinit var checkFreshToday: CheckBox
    private lateinit var checkVerifiedOnly: CheckBox
    private lateinit var rgSortBy: RadioGroup
    private lateinit var btnApplyFilters: Button
    private lateinit var btnSaveSearchAlert: Button

    private val categories = listOf(
        "all" to "सभी श्रेणियाँ (All Categories)",
        "grains" to "🌾 देशी अनाज व मिलेट्स (Grains & Millets)",
        "pulses_oilseeds" to "🥜 दाल व तिलहन (Pulses & Oilseeds)",
        "vegetables" to "🥬 ताज़ा सब्जियां (Fresh Vegetables)",
        "fruits" to "🍊 देशी फल (Fruits)",
        "cold_pressed_oil" to "🫙 कच्ची घानी तेल (Cold Pressed Oil)",
        "spices_jaggery" to "🍯 गुड़ व मसाले (Jaggery & Spices)",
        "seeds" to "🌱 देशी व प्रमाणित बीज (Seeds)",
        "traditional" to "🏺 दुर्लभ / पारंपरिक किस्म (Rare / Desi)"
    )

    private val radiusOptions = listOf(
        0 to "सभी क्षेत्र (All Distance)",
        5 to "5 km के अंदर (Nearby Farm)",
        10 to "10 km के अंदर",
        25 to "25 km के अंदर (तहसील / आसपास)",
        50 to "50 km के अंदर (पूरा ज़िला)"
    )

    fun setFilterApplyListener(listener: FilterApplyListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val scroll = ScrollView(ctx).apply {
            isFillViewport = true
            setBackgroundColor(Color.WHITE)
        }

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(28))
        }

        root.addView(
            TextView(ctx).apply {
                text = "🔍 उन्नत खोज व फ़िल्टर (Search & Filter)"
                setTextColor(Color.parseColor("#124116"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )

        root.addView(
            TextView(ctx).apply {
                text = "अपनी पसंद के अनुसार देशी उपज, भाव दायरा, दूरी और सत्यापन स्तर चुनें:"
                setTextColor(Color.parseColor("#4A5A4A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(2), 0, dp(10))
            }
        )

        // 1. Keyword Input
        root.addView(createLabel("1. खोज शब्द (फसल का नाम / किस्म / स्थान):"))
        inputQuery = EditText(ctx).apply {
            hint = "उदा. शरबती गेहूं 10 kg, ऊटी लहसुन..."
            background = roundedBg("#FAFAFA", "#B8CBB8", dp(8))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            minHeight = dp(44)
        }
        root.addView(inputQuery)

        // 2. Category Dropdown
        root.addView(createLabel("2. फसल श्रेणी चुनें:"))
        spinnerCategory = Spinner(ctx).apply {
            minimumHeight = dp(46)
            adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, categories.map { it.second })
        }
        root.addView(spinnerCategory)

        // 3. Price Range (Min - Max)
        root.addView(createLabel("3. भाव दायरा (₹ प्रति इकाई):"))
        val priceRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }

        inputMinPrice = EditText(ctx).apply {
            hint = "न्यूनतम भाव (₹)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            background = roundedBg("#FAFAFA", "#B8CBB8", dp(8))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            minHeight = dp(44)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(6)
            }
        }

        inputMaxPrice = EditText(ctx).apply {
            hint = "अधिकतम भाव (₹)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            background = roundedBg("#FAFAFA", "#B8CBB8", dp(8))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            minHeight = dp(44)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(6)
            }
        }
        priceRow.addView(inputMinPrice)
        priceRow.addView(inputMaxPrice)
        root.addView(priceRow)

        // 4. Distance Radius
        root.addView(createLabel("4. दूरी सीमा (Distance Radius):"))
        spinnerRadius = Spinner(ctx).apply {
            minimumHeight = dp(46)
            adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, radiusOptions.map { it.second })
        }
        root.addView(spinnerRadius)

        // 5. Special Marketplace Badges Checkboxes
        root.addView(createLabel("5. विशेष सुविधाएँ:"))
        checkSmallQty = CheckBox(ctx).apply {
            text = "⚖️ केवल छोटी मात्रा (1–25 kg) उपलब्ध उत्पाद"
            setTextColor(Color.parseColor("#1B5E20"))
            setTypeface(typeface, Typeface.BOLD)
        }
        checkFreshToday = CheckBox(ctx).apply {
            text = "🥬 केवल आज खेत से ताज़ा तोड़ी गई फसल (Fresh Today)"
            setTextColor(Color.parseColor("#1B5E20"))
            setTypeface(typeface, Typeface.BOLD)
        }
        checkVerifiedOnly = CheckBox(ctx).apply {
            text = "✔ केवल सत्यापित / प्रमाणित किसान (Verified Only)"
            setTextColor(Color.parseColor("#1B5E20"))
            setTypeface(typeface, Typeface.BOLD)
        }
        root.addView(checkSmallQty)
        root.addView(checkFreshToday)
        root.addView(checkVerifiedOnly)

        // 6. Sort By Options
        root.addView(createLabel("6. क्रमबद्ध करें (Sort By):"))
        rgSortBy = RadioGroup(ctx).apply { orientation = LinearLayout.VERTICAL }
        val rbRecent = RadioButton(ctx).apply { text = "नया पहला (हाल ही में जोड़ा गया)"; isChecked = true; tag = "NEWEST" }
        val rbPriceAsc = RadioButton(ctx).apply { text = "कम भाव से अधिक (Low to High)"; tag = "PRICE_ASC" }
        val rbPriceDesc = RadioButton(ctx).apply { text = "अधिक भाव से कम (High to Low)"; tag = "PRICE_DESC" }
        rgSortBy.addView(rbRecent)
        rgSortBy.addView(rbPriceAsc)
        rgSortBy.addView(rbPriceDesc)
        root.addView(rgSortBy)

        // Action Buttons: Apply & Save Search Alert
        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(14), 0, 0)
        }

        btnApplyFilters = Button(ctx).apply {
            text = "✅ फ़िल्टर लागू करें (Apply Filters)"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            background = roundedBg("#1B5E20", "#1B5E20", dp(10))
            minHeight = dp(48)
            setOnClickListener { applyAndDismiss() }
        }

        btnSaveSearchAlert = Button(ctx).apply {
            text = "🔔 इस खोज का अलर्ट सेव करें (Save Search Alert)"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            background = roundedBg("#0D47A1", "#0D47A1", dp(10))
            minHeight = dp(44)
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
            layoutParams = lp
            visibility = if (FeatureManager.isEnabled("saved_search", ctx)) View.VISIBLE else View.GONE
            setOnClickListener { saveSearchAlert() }
        }

        btnRow.addView(btnApplyFilters)
        btnRow.addView(btnSaveSearchAlert)
        root.addView(btnRow)

        scroll.addView(root)
        return scroll
    }

    private fun applyAndDismiss() {
        val q = inputQuery.text?.toString()?.trim()?.ifBlank { null }
        val catCode = categories.getOrNull(spinnerCategory.selectedItemPosition)?.first?.let {
            if (it == "all") null else it
        }
        val minP = inputMinPrice.text?.toString()?.toDoubleOrNull()
        val maxP = inputMaxPrice.text?.toString()?.toDoubleOrNull()
        val radiusKm = radiusOptions.getOrNull(spinnerRadius.selectedItemPosition)?.first?.let {
            if (it == 0) null else it
        }
        val isSmall = checkSmallQty.isChecked
        val isFresh = checkFreshToday.isChecked
        val isVerif = checkVerifiedOnly.isChecked

        val selectedRbId = rgSortBy.checkedRadioButtonId
        val sortBy = view?.findViewById<RadioButton>(selectedRbId)?.tag?.toString() ?: "NEWEST"

        listener?.onFiltersApplied(
            query = q,
            category = catCode,
            minPrice = minP,
            maxPrice = maxP,
            radiusKm = radiusKm,
            smallQuantityOnly = isSmall,
            freshTodayOnly = isFresh,
            verifiedOnly = isVerif,
            sortBy = sortBy
        )
        dismissAllowingStateLoss()
    }

    private fun saveSearchAlert() {
        val ctx = context ?: return
        val q = inputQuery.text?.toString()?.trim() ?: ""
        val catCode = categories.getOrNull(spinnerCategory.selectedItemPosition)?.first ?: "all"

        if (q.isBlank() && catCode == "all") {
            Toast.makeText(ctx, "सर्च अलर्ट सेव करने के लिए फसल का नाम या श्रेणी चुनें।", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val body = JSONObject().apply {
                put("query", q.ifBlank { catCode })
                put("category", if (catCode == "all") null else catCode)
                put("district", SessionManager.getUserDistrict(ctx))
                put("radius_km", radiusOptions.getOrNull(spinnerRadius.selectedItemPosition)?.first ?: 25)
            }
            val res = ApiClient.request(ctx, "/modules/saved-searches", method = "POST", bodyJson = body)
            if (res.isSuccess) {
                Toast.makeText(ctx, "🔔 आपकी खोज का अलर्ट सेव हो गया! नई फसल आने पर सूचना मिलेगी।", Toast.LENGTH_LONG).show()
                dismissAllowingStateLoss()
            } else {
                Toast.makeText(ctx, res.errorMessage ?: "अलर्ट सेव करने में समस्या आई।", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createLabel(textStr: String): TextView =
        TextView(requireContext()).apply {
            text = textStr
            setTextColor(Color.parseColor("#1A241A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(2))
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
