// app/src/main/java/in/merakisan/app/ui/modules/FarmBoxGroupBuyFragment.kt
package `in`.merakisan.app.ui.modules

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
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
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * MERA KISAN — VILLAGE FRESH FARM BOX, GROUP BUYING & HARVEST PRE-ORDER HUB
 * Exact Repository Path: app/src/main/java/in/merakisan/app/ui/modules/FarmBoxGroupBuyFragment.kt
 *
 * Implements (Master Spec Sec 5, 16, 20 & merged.txt Sec 6-8, 38-40, 45-47):
 * - Village Fresh Farm Box: Combines small quantities from multiple local farmers into a family box.
 * - Group Buying: Urban families pool demand (e.g., 50-100 kg target) for direct farm pickup/delivery.
 * - Harvest Pre-Booking: Farmers list upcoming harvest 15-30 days in advance; buyers reserve shares.
 */
class FarmBoxGroupBuyFragment : Fragment() {

    private var activeTab: String = "FARM_BOX" // "FARM_BOX", "GROUP_BUY", "PRE_ORDER"

    private lateinit var btnTabFarmBox: Button
    private lateinit var btnTabGroupBuy: Button
    private lateinit var btnTabPreOrder: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var listContainer: LinearLayout

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

        val headerCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#FFFFFF", "#D4E2D4", dp(14))
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        headerCard.addView(
            TextView(ctx).apply {
                text = "🧺 ग्राम ताज़ा फार्म बॉक्स, सामूहिक खरीदारी व प्री-ऑर्डर"
                setTextColor(Color.parseColor("#124116"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )

        headerCard.addView(
            TextView(ctx).apply {
                text = "छोटे किसानों की उपज का संयुक्त फैमिली बॉक्स और शहरी परिवारों की सामूहिक बचत खरीदारी:"
                setTextColor(Color.parseColor("#4A5A4A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(4), 0, dp(10))
            }
        )

        val tabRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        btnTabFarmBox = createTabButton("🧺 फार्म बॉक्स") {
            activeTab = "FARM_BOX"
            updateTabStyles()
            loadActiveSection()
        }
        btnTabGroupBuy = createTabButton("👨‍👩‍👧‍👦 ग्रुप बाइंग") {
            activeTab = "GROUP_BUY"
            updateTabStyles()
            loadActiveSection()
        }
        btnTabPreOrder = createTabButton("🌱 प्री-ऑर्डर") {
            activeTab = "PRE_ORDER"
            updateTabStyles()
            loadActiveSection()
        }

        tabRow.addView(btnTabFarmBox)
        tabRow.addView(btnTabGroupBuy)
        tabRow.addView(btnTabPreOrder)
        headerCard.addView(tabRow)
        root.addView(headerCard)

        progressBar = ProgressBar(ctx).apply {
            visibility = View.GONE
            setPadding(0, dp(12), 0, dp(12))
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
        updateTabStyles()
        loadActiveSection()
    }

    private fun updateTabStyles() {
        val pairs = listOf(
            btnTabFarmBox to "FARM_BOX",
            btnTabGroupBuy to "GROUP_BUY",
            btnTabPreOrder to "PRE_ORDER"
        )
        for ((btn, key) in pairs) {
            if (key == activeTab) {
                btn.background = roundedBg("#1B5E20", "#1B5E20", dp(8))
                btn.setTextColor(Color.WHITE)
            } else {
                btn.background = roundedBg("#E8F5E9", "#81C784", dp(8))
                btn.setTextColor(Color.parseColor("#1B5E20"))
            }
        }
    }

    private fun loadActiveSection() {
        when (activeTab) {
            "FARM_BOX" -> loadFarmBoxes()
            "GROUP_BUY" -> loadGroupBuyPools()
            "PRE_ORDER" -> loadPreOrders()
        }
    }

    private fun loadFarmBoxes() {
        val ctx = context ?: return
        listContainer.removeAllViews()
        progressBar.visibility = View.VISIBLE

        if (SessionManager.isFarmerModeActive(ctx)) {
            listContainer.addView(
                createActionButton("➕ नया ग्राम ताज़ा फार्म बॉक्स (Farm Box) बनाएँ", "#1B5E20") {
                    showCreateFarmBoxDialog()
                }
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.request(ctx, "/modules/farm-boxes", method = "GET")
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val arr = res.dataArray ?: JSONArray()
            if (arr.length() == 0) {
                listContainer.addView(
                    TextView(ctx).apply {
                        text = "अभी कोई सक्रिय फार्म बॉक्स उपलब्ध नहीं है।"
                        setTextColor(Color.parseColor("#555555"))
                        setPadding(dp(8), dp(16), dp(8), dp(16))
                    }
                )
                return@launch
            }

            for (i in 0 until arr.length()) {
                val box = arr.optJSONObject(i) ?: continue
                val title = box.optString("title", "ग्राम ताज़ा फार्म बॉक्स")
                val itemsSummary = box.optString("items_summary", "ताज़ा सब्जियां व अनाज")
                val priceRupees = box.optDouble("price_rupees", 399.0)
                val district = box.optString("district", "")
                val availableBoxes = box.optInt("available_boxes", 10)

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

                card.addView(
                    TextView(ctx).apply {
                        text = "🧺 $title — ₹$priceRupees / बॉक्स"
                        setTextColor(Color.parseColor("#1B5E20"))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                        setTypeface(typeface, Typeface.BOLD)
                    }
                )
                card.addView(
                    TextView(ctx).apply {
                        text = "📦 शामिल उत्पाद: $itemsSummary\n📍 क्षेत्र: $district • उपलब्ध बॉक्स: $availableBoxes"
                        setTextColor(Color.parseColor("#333333"))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        setPadding(0, dp(4), 0, dp(8))
                    }
                )
                listContainer.addView(card)
            }
        }
    }

    private fun loadGroupBuyPools() {
        val ctx = context ?: return
        listContainer.removeAllViews()
        progressBar.visibility = View.VISIBLE

        listContainer.addView(
            createActionButton("➕ नया सामूहिक खरीदारी पूल (Group Buying Pool) शुरू करें", "#0D47A1") {
                showCreateGroupBuyDialog()
            }
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.request(ctx, "/modules/group-buying", method = "GET")
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val arr = res.dataArray ?: JSONArray()
            if (arr.length() == 0) {
                listContainer.addView(
                    TextView(ctx).apply {
                        text = "अभी कोई सामूहिक खरीदारी पूल सक्रिय नहीं है।"
                        setTextColor(Color.parseColor("#555555"))
                        setPadding(dp(8), dp(16), dp(8), dp(16))
                    }
                )
                return@launch
            }

            for (i in 0 until arr.length()) {
                val pool = arr.optJSONObject(i) ?: continue
                val poolId = pool.optString("id", "")
                val prodName = pool.optString("product_name", "देशी फसल")
                val pricePerKg = pool.optDouble("price_per_kg_rupees", 40.0)
                val targetKg = pool.optDouble("target_quantity_kg", 100.0)
                val bookedKg = pool.optDouble("booked_quantity_kg", 0.0)
                val remainingKg = (targetKg - bookedKg).coerceAtLeast(0.0)
                val district = pool.optString("district", "")
                val status = pool.optString("status", "OPEN")

                val card = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    background = roundedBg("#FFFFFF", "#90CAF9", dp(12))
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    val lp = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    lp.topMargin = dp(10)
                    layoutParams = lp
                }

                card.addView(
                    TextView(ctx).apply {
                        text = "👨‍👩‍👧‍👦 $prodName — सामूहिक भाव: ₹$pricePerKg/kg ($status)"
                        setTextColor(Color.parseColor("#0D47A1"))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                        setTypeface(typeface, Typeface.BOLD)
                    }
                )
                card.addView(
                    TextView(ctx).apply {
                        text = "🎯 लक्ष्य: $targetKg kg  |  ✅ बुक हो चुका: $bookedKg kg  |  ⏳ शेष: $remainingKg kg\n📍 क्षेत्र: $district"
                        setTextColor(Color.parseColor("#333333"))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        setPadding(0, dp(4), 0, dp(8))
                    }
                )

                if (status == "OPEN" && remainingKg > 0) {
                    card.addView(
                        createActionButton("🤝 इस ग्रुप ऑर्डर में अपना हिस्सा बुक करें", "#1B5E20") {
                            showJoinGroupBuyDialog(poolId, prodName, remainingKg)
                        }
                    )
                }

                listContainer.addView(card)
            }
        }
    }

    private fun loadPreOrders() {
        val ctx = requireContext()
        listContainer.removeAllViews()

        val settings = FeatureManager.getFarmBoxAndGroupBuySettings(ctx)
        val infoCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#FFFFFF", "#C8E6C9", dp(12))
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        infoCard.addView(
            TextView(ctx).apply {
                text = "🌱 हार्वेस्ट प्री-ऑर्डर (कटाई से ${settings.maxPreOrderDays} दिन पूर्व बुकिंग)"
                setTextColor(Color.parseColor("#1B5E20"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )
        infoCard.addView(
            TextView(ctx).apply {
                text = "किसान अपनी आगामी फसल की संभावित कटाई तिथि के साथ लिस्टिंग कर सकते हैं और शहरी ग्राहक पहले से अपनी मात्रा आरक्षित कर सकते हैं।"
                setTextColor(Color.parseColor("#4A5A4A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(4), 0, dp(8))
            }
        )
        listContainer.addView(infoCard)
    }

    private fun showCreateFarmBoxDialog() {
        val ctx = context ?: return
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }
        val titleInp = EditText(ctx).apply {
            hint = "बॉक्स का नाम *"
            setText("ग्राम ताज़ा फैमिली बॉक्स (Village Fresh Box)")
        }
        val itemsInp = EditText(ctx).apply {
            hint = "शामिल उत्पाद व मात्रा (उदा. टमाटर 2kg, बैंगन 1kg, दाल 1kg, गुड़ 500g) *"
        }
        val priceInp = EditText(ctx).apply {
            hint = "बॉक्स की कीमत (₹) *"
            setText(FeatureManager.getFarmBoxAndGroupBuySettings(ctx).defaultBoxPriceRupees.toString())
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        layout.addView(titleInp)
        layout.addView(itemsInp)
        layout.addView(priceInp)

        AlertDialog.Builder(ctx)
            .setTitle("🧺 नया ग्राम ताज़ा फार्म बॉक्स बनाएँ")
            .setView(layout)
            .setPositiveButton("प्रकाशित करें") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val body = JSONObject().apply {
                        put("title", titleInp.text?.toString()?.trim() ?: "Village Fresh Box")
                        put("items_summary", itemsInp.text?.toString()?.trim() ?: "")
                        put("price_rupees", priceInp.text?.toString()?.toDoubleOrNull() ?: 399.0)
                        put("district", SessionManager.getUserDistrict(ctx))
                    }
                    val res = ApiClient.request(ctx, "/modules/farm-boxes", method = "POST", bodyJson = body)
                    if (res.isSuccess) {
                        Toast.makeText(ctx, "✅ फार्म बॉक्स प्रकाशित हो गया!", Toast.LENGTH_LONG).show()
                        loadFarmBoxes()
                    }
                }
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun showCreateGroupBuyDialog() {
        val ctx = context ?: return
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }
        val prodInp = EditText(ctx).apply { hint = "फसल का नाम (उदा. देशी गेहूं / मूंगफली) *" }
        val targetInp = EditText(ctx).apply {
            hint = "सामूहिक लक्ष्य मात्रा (kg में - उदा. 100) *"
            setText("100")
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val priceInp = EditText(ctx).apply {
            hint = "सामूहिक थोक भाव (₹ प्रति kg) *"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        layout.addView(prodInp)
        layout.addView(targetInp)
        layout.addView(priceInp)

        AlertDialog.Builder(ctx)
            .setTitle("👨‍👩‍👧‍👦 नया सामूहिक खरीदारी पूल शुरू करें")
            .setView(layout)
            .setPositiveButton("शुरू करें") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val body = JSONObject().apply {
                        put("product_name", prodInp.text?.toString()?.trim() ?: "")
                        put("target_quantity_kg", targetInp.text?.toString()?.toDoubleOrNull() ?: 100.0)
                        put("price_per_kg_rupees", priceInp.text?.toString()?.toDoubleOrNull() ?: 40.0)
                        put("district", SessionManager.getUserDistrict(ctx))
                    }
                    val res = ApiClient.request(ctx, "/modules/group-buying", method = "POST", bodyJson = body)
                    if (res.isSuccess) {
                        Toast.makeText(ctx, "✅ ग्रुप बाइंग पूल शुरू हो गया!", Toast.LENGTH_LONG).show()
                        loadGroupBuyPools()
                    }
                }
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun showJoinGroupBuyDialog(poolId: String, prodName: String, maxRemainingKg: Double) {
        val ctx = context ?: return
        val qtyInp = EditText(ctx).apply {
            hint = "आपकी मात्रा (kg में — अधिकतम $maxRemainingKg kg)"
            setText("10")
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(dp(18), dp(12), dp(18), dp(12))
        }

        AlertDialog.Builder(ctx)
            .setTitle("🤝 '$prodName' ग्रुप ऑर्डर में जुड़ें")
            .setView(qtyInp)
            .setPositiveButton("हिस्सा बुक करें") { _, _ ->
                val qty = qtyInp.text?.toString()?.toDoubleOrNull() ?: 0.0
                if (qty <= 0.0 || qty > maxRemainingKg) {
                    Toast.makeText(ctx, "कृपया 1 से $maxRemainingKg kg के बीच मात्रा डालें।", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val body = JSONObject().apply { put("quantity_kg", qty) }
                    val res = ApiClient.request(ctx, "/modules/group-buying/$poolId/join", method = "POST", bodyJson = body)
                    if (res.isSuccess) {
                        Toast.makeText(ctx, "🎉 आपका $qty kg हिस्सा ग्रुप पूल में जुड़ गया!", Toast.LENGTH_LONG).show()
                        loadGroupBuyPools()
                    } else {
                        Toast.makeText(ctx, res.errorMessage ?: "जुड़ने में समस्या आई।", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun createTabButton(label: String, onClick: () -> Unit): Button =
        Button(requireContext()).apply {
            text = label
            isAllCaps = false
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            minHeight = dp(40)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(4)
            }
            setOnClickListener { onClick() }
        }

    private fun createActionButton(label: String, bgHex: String, onClick: () -> Unit): Button =
        Button(requireContext()).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
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
