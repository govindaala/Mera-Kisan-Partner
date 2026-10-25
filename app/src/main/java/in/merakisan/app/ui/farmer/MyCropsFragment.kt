// app/src/main/java/in/merakisan/app/ui/farmer/MyCropsFragment.kt
package `in`.merakisan.app.ui.farmer

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import `in`.merakisan.app.core.config.FeatureManager
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.security.SessionManager
import `in`.merakisan.app.ui.main.MainActivity
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * MERA KISAN — FARMER PRODUCT MANAGEMENT ("मेरी फसलें")
 * Exact Repository Path: app/src/main/java/in/merakisan/app/ui/farmer/MyCropsFragment.kt
 *
 * Implements (merged.txt Sec 36 & 51):
 * - Filter by ALL / ACTIVE / SOLD
 * - ➕ नई फसल डालें (Add New Product)
 * - Edit Price & Stock, Mark Sold / Re-activate, Delete Listing
 * - Request Crop Boost (Admin configurable price & duration via FeatureManager)
 * - Open AI .JPG Poster Studio for WhatsApp/Social marketing
 */
class MyCropsFragment : Fragment() {

    private var statusFilter: String = "ALL" // "ALL", "ACTIVE", "SOLD"
    private val allMyCrops = mutableListOf<JSONObject>()

    private lateinit var tvSummaryCount: TextView
    private lateinit var btnFilterAll: Button
    private lateinit var btnFilterActive: Button
    private lateinit var btnFilterSold: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var cropsListContainer: LinearLayout

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
            setPadding(dp(14), dp(14), dp(14), dp(28))
        }

        // Top Header Card
        val headerCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#FFFFFF", "#D4E2D4", dp(14))
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        headerCard.addView(
            TextView(ctx).apply {
                text = "🌾 मेरी फसलें व उत्पाद प्रबंधन"
                setTextColor(Color.parseColor("#124116"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )

        tvSummaryCount = TextView(ctx).apply {
            text = "आपकी सूचीबद्ध फसलें लोड हो रही हैं..."
            setTextColor(Color.parseColor("#4A5A4A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(4), 0, dp(10))
        }
        headerCard.addView(tvSummaryCount)

        headerCard.addView(
            createButton("➕ नई फसल / बीज / उत्पाद डालें", "#1B5E20") {
                (activity as? MainActivity)?.openAddProductScreen(null)
            }
        )

        // Filter Tabs Row: [ सभी ] [ 🟢 चालू (Active) ] [ 🔴 बिक चुकी (Sold) ]
        val filterRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }

        btnFilterAll = createFilterTabButton("सभी") { applyStatusFilter("ALL") }
        btnFilterActive = createFilterTabButton("🟢 चालू (Active)") { applyStatusFilter("ACTIVE") }
        btnFilterSold = createFilterTabButton("🔴 बिक चुकी (Sold)") { applyStatusFilter("SOLD") }

        filterRow.addView(btnFilterAll)
        filterRow.addView(btnFilterActive)
        filterRow.addView(btnFilterSold)
        headerCard.addView(filterRow)

        root.addView(headerCard)

        progressBar = ProgressBar(ctx).apply {
            visibility = View.GONE
            setPadding(0, dp(12), 0, dp(12))
        }
        root.addView(progressBar)

        cropsListContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        root.addView(cropsListContainer)

        scroll.addView(root)
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateFilterTabStyles()
        fetchMyCropsFromServer()
    }

    private fun fetchMyCropsFromServer() {
        val ctx = context ?: return
        val myUid = SessionManager.getUserUid(ctx) ?: ""
        if (myUid.isBlank()) {
            tvSummaryCount.text = "कृपया अपनी फसलें देखने के लिए लॉगिन करें।"
            return
        }

        progressBar.visibility = View.VISIBLE
        cropsListContainer.removeAllViews()

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.request(
                context = ctx,
                endpointPath = "/products",
                method = "GET",
                queryParams = mapOf(
                    "farmer_uid" to myUid,
                    "include_sold" to "true"
                )
            )
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            allMyCrops.clear()
            val arr = res.dataArray ?: JSONArray()
            var activeCount = 0
            var soldCount = 0

            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                allMyCrops.add(obj)
                val st = obj.optString("status", "ACTIVE").uppercase()
                val stock = obj.optDouble("stock_quantity", 0.0)
                if (st == "ACTIVE" && stock > 0) activeCount += 1 else soldCount += 1
            }

            tvSummaryCount.text = "कुल उत्पाद: ${allMyCrops.size}  |  🟢 चालू: $activeCount  |  🔴 बिक चुके: $soldCount"
            renderFilteredCrops()
        }
    }

    private fun applyStatusFilter(newFilter: String) {
        statusFilter = newFilter
        updateFilterTabStyles()
        renderFilteredCrops()
    }

    private fun updateFilterTabStyles() {
        val pairs = listOf(
            btnFilterAll to "ALL",
            btnFilterActive to "ACTIVE",
            btnFilterSold to "SOLD"
        )
        for ((btn, key) in pairs) {
            if (key == statusFilter) {
                btn.background = roundedBg("#1B5E20", "#1B5E20", dp(8))
                btn.setTextColor(Color.WHITE)
            } else {
                btn.background = roundedBg("#F1F8E9", "#A5D6A7", dp(8))
                btn.setTextColor(Color.parseColor("#1B5E20"))
            }
        }
    }

    private fun renderFilteredCrops() {
        val ctx = context ?: return
        cropsListContainer.removeAllViews()

        val filtered = allMyCrops.filter { obj ->
            val st = obj.optString("status", "ACTIVE").uppercase()
            val stock = obj.optDouble("stock_quantity", 0.0)
            val isEffectiveActive = st == "ACTIVE" && stock > 0
            when (statusFilter) {
                "ACTIVE" -> isEffectiveActive
                "SOLD" -> !isEffectiveActive
                else -> true
            }
        }

        if (filtered.isEmpty()) {
            cropsListContainer.addView(
                TextView(ctx).apply {
                    text = "इस श्रेणी में अभी आपकी कोई फसल नहीं है।"
                    setTextColor(Color.parseColor("#555555"))
                    setPadding(dp(8), dp(18), dp(8), dp(18))
                }
            )
            return
        }

        for (crop in filtered) {
            cropsListContainer.addView(buildCropManagementCard(crop))
        }
    }

    private fun buildCropManagementCard(prod: JSONObject): View {
        val ctx = requireContext()
        val prodId = prod.optString("id", "")
        val name = prod.optString("name", "फसल")
        val variety = prod.optString("variety", "")
        val category = prod.optString("category", "grains")
        val priceRupees = prod.optDouble("price_rupees", prod.optDouble("price", 0.0))
        val unit = prod.optString("unit", "kg")
        val stock = prod.optDouble("stock_quantity", 0.0)
        val minOrder = prod.optDouble("min_order_quantity", 1.0)
        val status = prod.optString("status", "ACTIVE").uppercase()
        val isEffectiveActive = status == "ACTIVE" && stock > 0
        val boosted = prod.optBoolean("boosted", false)

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#FFFFFF", "#C8E6C9", dp(12))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(10)
            layoutParams = lp
        }

        val statusBadgeText = buildString {
            append(if (isEffectiveActive) "🟢 चालू (ACTIVE)" else "🔴 बिक चुकी (SOLD)")
            if (boosted) append(" • 🚀 बूस्टेड (Top Listing)")
            if (minOrder <= 25.0) append(" • ⚖️ छोटी मात्रा उपलब्ध")
        }

        card.addView(
            TextView(ctx).apply {
                text = statusBadgeText
                setTextColor(if (isEffectiveActive) Color.parseColor("#1B5E20") else Color.parseColor("#B71C1C"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )

        card.addView(
            TextView(ctx).apply {
                text = "$name ${if (variety.isNotBlank() && variety != "null") "($variety)" else ""} [$category]"
                setTextColor(Color.parseColor("#1A241A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(4), 0, dp(2))
            }
        )

        card.addView(
            TextView(ctx).apply {
                text = "💰 भाव: ₹$priceRupees/$unit  |  📦 उपलब्ध स्टॉक: $stock $unit (न्यूनतम: $minOrder $unit)"
                setTextColor(Color.parseColor("#BF360C"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, 0, 0, dp(10))
            }
        )

        // Row 1: Edit | Quick Stock/Sold Toggle | Delete
        val row1 = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }

        val btnEdit = createSmallButton("✏️ एडिट करें", "#0D47A1") {
            (activity as? MainActivity)?.openAddProductScreen(prodId)
        }

        val btnToggleSold = if (isEffectiveActive) {
            createSmallButton("✅ बिक गई मार्क करें", "#4E342E") {
                markCropAsSold(prodId, name)
            }
        } else {
            createSmallButton("🔄 पुनः स्टॉक डालें", "#2E7D32") {
                showRestockDialog(prodId, name, priceRupees, unit)
            }
        }

        val btnDelete = createSmallButton("🗑️ हटाएँ", "#B71C1C") {
            confirmDeleteCrop(prodId, name)
        }

        row1.addView(btnEdit)
        row1.addView(btnToggleSold)
        row1.addView(btnDelete)
        card.addView(row1)

        // Row 2: AI .JPG Poster Studio | Crop Boost
        val row2 = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }

        val btnPoster = createSmallButton("🎨 AI .JPG पोस्टर व शेयर", "#1B5E20") {
            (activity as? MainActivity)?.openAiPosterStudioScreen(prodId)
        }
        row2.addView(btnPoster)

        if (FeatureManager.isEnabled("crop_boost", ctx) && isEffectiveActive) {
            val boostFee = FeatureManager.getBoostPriceRupees()
            val boostDays = FeatureManager.getBoostDurationDays()
            val btnBoost = createSmallButton("🚀 बूस्ट (₹$boostFee/${boostDays}d)", "#BF360C") {
                requestCropBoost(prodId, name)
            }
            row2.addView(btnBoost)
        }

        card.addView(row2)
        return card
    }

    private fun markCropAsSold(productId: String, name: String) {
        val ctx = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val updates = JSONObject().apply {
                put("status", "SOLD")
                put("stock_quantity", 0)
            }
            val res = ApiClient.updateProduct(ctx, productId, updates)
            if (res.isSuccess) {
                Toast.makeText(ctx, "'$name' को बिक गई (SOLD) मार्क कर दिया गया।", Toast.LENGTH_SHORT).show()
                fetchMyCropsFromServer()
            } else {
                Toast.makeText(ctx, res.errorMessage ?: "अपडेट विफल रहा।", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showRestockDialog(productId: String, name: String, currentPrice: Double, unit: String) {
        val ctx = context ?: return
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }
        val stockInput = EditText(ctx).apply {
            hint = "नई उपलब्ध मात्रा ($unit में)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val priceInput = EditText(ctx).apply {
            hint = "नया भाव (₹ प्रति $unit)"
            setText(currentPrice.toString())
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        layout.addView(stockInput)
        layout.addView(priceInput)

        AlertDialog.Builder(ctx)
            .setTitle("🔄 '$name' को पुनः चालू करें")
            .setView(layout)
            .setPositiveButton("चालू करें") { _, _ ->
                val newStock = stockInput.text?.toString()?.toDoubleOrNull() ?: 0.0
                val newPrice = priceInput.text?.toString()?.toDoubleOrNull() ?: currentPrice
                if (newStock <= 0.0 || newPrice <= 0.0) {
                    Toast.makeText(ctx, "कृपया मान्य मात्रा और भाव दर्ज करें।", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val updates = JSONObject().apply {
                        put("status", "ACTIVE")
                        put("stock_quantity", newStock)
                        put("price_paise", Math.round(newPrice * 100))
                        put("price", newPrice)
                    }
                    val res = ApiClient.updateProduct(ctx, productId, updates)
                    if (res.isSuccess) {
                        Toast.makeText(ctx, "'$name' पुनः बिक्री के लिए चालू हो गई!", Toast.LENGTH_SHORT).show()
                        fetchMyCropsFromServer()
                    }
                }
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun confirmDeleteCrop(productId: String, name: String) {
        val ctx = context ?: return
        AlertDialog.Builder(ctx)
            .setTitle("🗑️ फसल हटाएँ")
            .setMessage("क्या आप वाकई '$name' को बाज़ार से हटाना चाहते हैं?")
            .setPositiveButton("हाँ, हटाएँ") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val res = ApiClient.deleteProduct(ctx, productId)
                    if (res.isSuccess) {
                        Toast.makeText(ctx, "'$name' को हटा दिया गया।", Toast.LENGTH_SHORT).show()
                        fetchMyCropsFromServer()
                    } else {
                        Toast.makeText(ctx, res.errorMessage ?: "हटाने में समस्या आई।", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("नहीं", null)
            .show()
    }

    private fun requestCropBoost(productId: String, name: String) {
        val ctx = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val body = JSONObject().apply { put("product_id", productId) }
            val res = ApiClient.request(ctx, "/crop-boost", method = "POST", bodyJson = body)
            if (res.isSuccess) {
                Toast.makeText(
                    ctx,
                    "🚀 '$name' के लिए बूस्ट अनुरोध दर्ज हो गया!",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(ctx, res.errorMessage ?: "बूस्ट अनुरोध विफल रहा।", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createFilterTabButton(label: String, onClick: () -> Unit): Button =
        Button(requireContext()).apply {
            text = label
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
            minHeight = dp(38)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(6)
            }
            setOnClickListener { onClick() }
        }

    private fun createButton(label: String, bgHex: String, onClick: () -> Unit): Button =
        Button(requireContext()).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            background = roundedBg(bgHex, bgHex, dp(10))
            minHeight = dp(46)
            setOnClickListener { onClick() }
        }

    private fun createSmallButton(label: String, bgHex: String, onClick: () -> Unit): Button =
        Button(requireContext()).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            background = roundedBg(bgHex, bgHex, dp(8))
            minHeight = dp(40)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(4)
            }
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
