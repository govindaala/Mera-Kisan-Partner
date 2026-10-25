// app/src/main/java/in/merakisan/app/ui/crops/MarketplaceFragment.kt
package `in`.merakisan.app.ui.crops

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
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
import org.json.JSONArray
import org.json.JSONObject

/**
 * MERA KISAN — UNIFIED MARKETPLACE & FARMER "MY CROPS" MANAGER
 *
 * Modes:
 * - FARMER_MY_CROPS: Farmer manages own listings (Active / Sold, Edit, Mark Sold, Delete, Crop Boost, Poster Share).
 * - BUYER_DISCOVERY: Buyer searches by natural Hindi query, category (including separate Seeds & Cold Pressed Oil),
 *   Small Quantity (1-25 kg), Fresh Today, District, and can save search alerts.
 */
class MarketplaceFragment : Fragment() {

    private var screenMode: String = "BUYER_DISCOVERY"

    private lateinit var headerTitle: TextView
    private lateinit var searchInput: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var checkSmallQty: CheckBox
    private lateinit var checkFreshToday: CheckBox
    private lateinit var progressBar: ProgressBar
    private lateinit var listContainer: LinearLayout

    private val categoriesList = listOf(
        "all" to "सभी श्रेणियाँ (All Categories)",
        "grains" to "🌾 देशी अनाज (Grains & Millets)",
        "pulses_oilseeds" to "🥜 दाल व तिलहन (Pulses & Oilseeds)",
        "vegetables" to "🥬 ताज़ा सब्जियां (Fresh Vegetables)",
        "fruits" to "🍊 देशी फल (Fruits)",
        "cold_pressed_oil" to "🫙 कच्ची घानी तेल (Cold Pressed Oil)",
        "spices_jaggery" to "🍯 गुड़ व मसाले (Jaggery & Spices)",
        "seeds" to "🌱 देशी व प्रमाणित बीज (Seeds)",
        "traditional" to "🏺 दुर्लभ / पारंपरिक किस्में (Rare/Desi)"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val argMode = arguments?.getString("mode")
        screenMode = if (!argMode.isNullOrBlank()) {
            argMode
        } else if (SessionManager.isFarmerModeActive(requireContext())) {
            "FARMER_MY_CROPS"
        } else {
            "BUYER_DISCOVERY"
        }
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
            setPadding(dp(14), dp(14), dp(14), dp(28))
        }

        val isFarmerMyCrops = screenMode == "FARMER_MY_CROPS"

        // 1. Header & Filter Card
        val topCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#FFFFFF", "#D4E2D4", dp(14))
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        headerTitle = TextView(ctx).apply {
            text = if (isFarmerMyCrops) "🌾 मेरी फसलें व उत्पाद प्रबंधन" else "🔍 किसान बाज़ार — खोजें व फ़िल्टर करें"
            setTextColor(Color.parseColor("#124116"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        }
        topCard.addView(headerTitle)

        if (isFarmerMyCrops) {
            topCard.addView(
                createButton("➕ नई फसल / बीज / उत्पाद जोड़ें", "#1B5E20") {
                    (activity as? MainActivity)?.openAddProductScreen(null)
                }
            )
        } else {
            searchInput = EditText(ctx).apply {
                hint = "खोजें: उदा. देशी गेहूं 10 किलो मंदसौर..."
                background = roundedBg("#FAFAFA", "#B8CBB8", dp(8))
                setPadding(dp(12), dp(10), dp(12), dp(10))
                minHeight = dp(44)
            }
            topCard.addView(searchInput)

            spinnerCategory = Spinner(ctx).apply {
                minimumHeight = dp(46)
                adapter = ArrayAdapter(
                    ctx,
                    android.R.layout.simple_spinner_dropdown_item,
                    categoriesList.map { it.second }
                )
            }
            topCard.addView(spinnerCategory)

            val checksRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(4), 0, dp(6))
            }
            checkSmallQty = CheckBox(ctx).apply {
                text = "⚖️ छोटी मात्रा (1–25 kg)"
                setOnCheckedChangeListener { _, _ -> loadProductsList() }
            }
            checkFreshToday = CheckBox(ctx).apply {
                text = "🥬 आज ताज़ा"
                setOnCheckedChangeListener { _, _ -> loadProductsList() }
            }
            checksRow.addView(checkSmallQty)
            checksRow.addView(checkFreshToday)
            topCard.addView(checksRow)

            val btnRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            val btnSearch = createButton("🔍 खोजें", "#1B5E20") {
                loadProductsList()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = dp(6)
                }
            }
            btnRow.addView(btnSearch)

            if (FeatureManager.isEnabled("saved_search", ctx)) {
                val btnSaveSearch = createButton("🔔 सर्च अलर्ट सेव करें", "#0D47A1") {
                    saveCurrentSearchAlert()
                }.apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        leftMargin = dp(6)
                    }
                }
                btnRow.addView(btnSaveSearch)
            }
            topCard.addView(btnRow)

            spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    loadProductsList()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        root.addView(topCard)

        progressBar = ProgressBar(ctx).apply {
            visibility = View.GONE
            setPadding(0, dp(10), 0, dp(10))
        }
        root.addView(progressBar)

        listContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        root.addView(listContainer)

        scroll.addView(root)
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadProductsList()
    }

    private fun loadProductsList() {
        val ctx = context ?: return
        val isFarmerMyCrops = screenMode == "FARMER_MY_CROPS"
        progressBar.visibility = View.VISIBLE
        listContainer.removeAllViews()

        viewLifecycleOwner.lifecycleScope.launch {
            val res = if (isFarmerMyCrops) {
                val myUid = SessionManager.getUserUid(ctx) ?: ""
                ApiClient.request(
                    context = ctx,
                    endpointPath = "/products",
                    method = "GET",
                    queryParams = mapOf(
                        "farmer_uid" to myUid,
                        "include_sold" to "true"
                    )
                )
            } else {
                val q = searchInput.text?.toString()?.trim()
                val catCode = categoriesList.getOrNull(spinnerCategory.selectedItemPosition)?.first ?: "all"
                ApiClient.getProducts(
                    context = ctx,
                    category = if (catCode == "all") null else catCode,
                    smallQuantityOnly = checkSmallQty.isChecked,
                    freshTodayOnly = checkFreshToday.isChecked,
                    searchQuery = q?.ifBlank { null }
                )
            }

            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val arr = res.dataArray ?: JSONArray()
            if (arr.length() == 0) {
                listContainer.addView(
                    TextView(ctx).apply {
                        text = if (isFarmerMyCrops) {
                            "आपने अभी तक कोई फसल या उत्पाद नहीं जोड़ा है।"
                        } else {
                            "आपकी खोज के अनुसार अभी कोई उत्पाद नहीं मिला।"
                        }
                        setTextColor(Color.parseColor("#555555"))
                        setPadding(dp(8), dp(16), dp(8), dp(16))
                    }
                )
                return@launch
            }

            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                listContainer.addView(
                    if (isFarmerMyCrops) buildFarmerManagedCropCard(item)
                    else buildBuyerDiscoveryCard(item)
                )
            }
        }
    }

    private fun buildFarmerManagedCropCard(prod: JSONObject): View {
        val ctx = requireContext()
        val prodId = prod.optString("id", "")
        val name = prod.optString("name", "फसल")
        val variety = prod.optString("variety", "")
        val priceRupees = prod.optDouble("price_rupees", prod.optDouble("price", 0.0))
        val unit = prod.optString("unit", "kg")
        val stock = prod.optDouble("stock_quantity", 0.0)
        val status = prod.optString("status", "ACTIVE").uppercase()
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

        val statusLabel = when (status) {
            "SOLD" -> "🔴 बिक गई (SOLD)"
            "ACTIVE" -> "🟢 बिक्री के लिए चालू (ACTIVE)"
            else -> status
        }

        card.addView(
            TextView(ctx).apply {
                text = "$statusLabel ${if (boosted) "• 🚀 बूस्टेड" else ""}"
                setTextColor(if (status == "ACTIVE") Color.parseColor("#1B5E20") else Color.parseColor("#B71C1C"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )

        card.addView(
            TextView(ctx).apply {
                text = "$name ${if (variety.isNotBlank() && variety != "null") "($variety)" else ""}"
                setTextColor(Color.parseColor("#1A241A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(4), 0, dp(2))
            }
        )

        card.addView(
            TextView(ctx).apply {
                text = "💰 भाव: ₹$priceRupees/$unit • 📦 स्टॉक: $stock $unit"
                setTextColor(Color.parseColor("#BF360C"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, 0, 0, dp(10))
            }
        )

        // Row 1: Edit | Mark Sold | Delete
        val row1 = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }

        val btnEdit = createButton("✏️ एडिट", "#0D47A1") {
            (activity as? MainActivity)?.openAddProductScreen(prodId)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(4)
            }
        }

        val btnMarkSold = createButton("✅ बिक गई", "#4E342E") {
            markCropAsSold(prodId, name)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(4)
                rightMargin = dp(4)
            }
        }

        val btnDelete = createButton("🗑️ हटाएँ", "#B71C1C") {
            confirmAndDeleteCrop(prodId, name)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(4)
            }
        }

        row1.addView(btnEdit)
        row1.addView(btnMarkSold)
        row1.addView(btnDelete)
        card.addView(row1)

        // Row 2: Poster & Share | Crop Boost (if enabled)
        val row2 = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }

        val btnPosterShare = createButton("🎨 .JPG पोस्टर व शेयर", "#1B5E20") {
            (activity as? MainActivity)?.openProductDetailScreen(prodId)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(4)
            }
        }
        row2.addView(btnPosterShare)

        if (FeatureManager.isEnabled("crop_boost", ctx)) {
            val boostFee = FeatureManager.getBoostPriceRupees()
            val boostDays = FeatureManager.getBoostDurationDays()
            val btnBoost = createButton("🚀 बूस्ट (₹$boostFee/${boostDays}d)", "#BF360C") {
                requestCropBoost(prodId, name)
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = dp(4)
                }
            }
            row2.addView(btnBoost)
        }

        card.addView(row2)
        return card
    }

    private fun buildBuyerDiscoveryCard(prod: JSONObject): View {
        val ctx = requireContext()
        val prodId = prod.optString("id", "")
        val name = prod.optString("name", "कृषि उत्पाद")
        val variety = prod.optString("variety", "")
        val priceRupees = prod.optDouble("price_rupees", prod.optDouble("price", 0.0))
        val unit = prod.optString("unit", "kg")
        val stock = prod.optDouble("stock_quantity", 0.0)
        val minOrder = prod.optDouble("min_order_quantity", 1.0)
        val farmerName = prod.optString("farmer_name", "किसान")
        val district = prod.optString("district", "")
        val verif = prod.optString("verification_status", "farmer_declared")

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#FFFFFF", "#D4E2D4", dp(12))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(10)
            layoutParams = lp
            setOnClickListener {
                (activity as? MainActivity)?.openProductDetailScreen(prodId)
            }
        }

        card.addView(
            TextView(ctx).apply {
                text = "$name ${if (variety.isNotBlank() && variety != "null") "($variety)" else ""}"
                setTextColor(Color.parseColor("#124116"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )
        card.addView(
            TextView(ctx).apply {
                text = "₹$priceRupees / $unit • उपलब्ध: $stock $unit (न्यूनतम ऑर्डर: $minOrder $unit)"
                setTextColor(Color.parseColor("#BF360C"))
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(3), 0, dp(3))
            }
        )
        card.addView(
            TextView(ctx).apply {
                text = "👨‍🌾 $farmerName ($district) • स्थिति: $verif"
                setTextColor(Color.parseColor("#4A5A4A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, 0, 0, dp(8))
            }
        )
        card.addView(
            createButton("🛒 विवरण देखें / खरीदें / भाव प्रस्ताव दें", "#1B5E20") {
                (activity as? MainActivity)?.openProductDetailScreen(prodId)
            }
        )
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
                loadProductsList()
            } else {
                Toast.makeText(ctx, res.errorMessage ?: "अपडेट विफल रहा।", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmAndDeleteCrop(productId: String, name: String) {
        val ctx = context ?: return
        AlertDialog.Builder(ctx)
            .setTitle("🗑️ फसल हटाएँ")
            .setMessage("क्या आप वाकई '$name' को बाज़ार से हटाना चाहते हैं?")
            .setPositiveButton("हाँ, हटाएँ") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val res = ApiClient.deleteProduct(ctx, productId)
                    if (res.isSuccess) {
                        Toast.makeText(ctx, "'$name' को हटा दिया गया।", Toast.LENGTH_SHORT).show()
                        loadProductsList()
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
                    "'$name' के लिए बूस्ट अनुरोध दर्ज हो गया! सत्यापन के बाद यह शीर्ष पर दिखेगी।",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(ctx, res.errorMessage ?: "बूस्ट अनुरोध विफल रहा।", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveCurrentSearchAlert() {
        val ctx = context ?: return
        val q = searchInput.text?.toString()?.trim() ?: ""
        if (q.length < 2) {
            Toast.makeText(ctx, "कृपया पहले खोज बॉक्स में फसल का नाम लिखें।", Toast.LENGTH_SHORT).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val body = JSONObject().apply {
                put("query", q)
                put("district", SessionManager.getUserDistrict(ctx))
            }
            val res = ApiClient.request(ctx, "/modules/saved-searches", method = "POST", bodyJson = body)
            if (res.isSuccess) {
                Toast.makeText(ctx, "🔔 आपकी खोज '$q' सेव हो गई! नया उत्पाद आने पर सूचना मिलेगी।", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(ctx, res.errorMessage ?: "खोज सहेजने में त्रुटि।", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createButton(label: String, bgHex: String, onClick: () -> Unit): Button =
        Button(requireContext()).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            background = roundedBg(bgHex, bgHex, dp(10))
            minHeight = dp(42)
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
