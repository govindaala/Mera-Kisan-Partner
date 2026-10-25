// app/src/main/java/in/merakisan/app/ui/modules/FairPriceCalculatorFragment.kt
package `in`.merakisan.app.ui.modules

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
import `in`.merakisan.app.ui.main.MainActivity
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * MERA KISAN — FAIR PRICE CALCULATOR ("उचित दाम कैलकुलेटर")
 * Exact Repository Path: app/src/main/java/in/merakisan/app/ui/modules/FairPriceCalculatorFragment.kt
 *
 * Implements (Master Spec Sec 5 & merged.txt Sec 16, 17, 46, 53):
 * - Farmers input total quantity (kg), production cost, transport cost, labour, packaging, and desired margin %.
 * - Calculates Minimum Sustainable Price (₹/kg & ₹/quintal) and Suggested Direct Customer Price
 *   via ApiClient (`/modules/fair-price`) so farmers never sell below cost.
 */
class FairPriceCalculatorFragment : Fragment() {

    private lateinit var inputCropName: EditText
    private lateinit var inputQuantityKg: EditText
    private lateinit var inputProductionCost: EditText
    private lateinit var inputLabourCost: EditText
    private lateinit var inputTransportCost: EditText
    private lateinit var inputPackagingCost: EditText
    private lateinit var inputMarginPercent: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var resultBox: TextView

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

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#FFFFFF", "#D4E2D4", dp(14))
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        card.addView(
            TextView(ctx).apply {
                text = "🧮 किसान उचित दाम कैलकुलेटर (Fair Price Calculator)"
                setTextColor(Color.parseColor("#124116"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )

        card.addView(
            TextView(ctx).apply {
                text = "अपनी खेती लागत, मजदूरी, भाड़ा और पैकेजिंग खर्च जोड़कर जानें कि सीधे ग्राहक को किस भाव पर फसल बेचना फायदेमंद रहेगा:"
                setTextColor(Color.parseColor("#4A5A4A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(4), 0, dp(10))
            }
        )

        inputCropName = addInput(card, "1. फसल का नाम", "उदा. देशी गेहूं, लहसुन, मूंगफली", isNumber = false)
        inputQuantityKg = addInput(card, "2. कुल बिक्री योग्य मात्रा (kg में) *", "उदा. 100", isNumber = true).apply {
            setText("100")
        }
        inputProductionCost = addInput(card, "3. बीज, खाद व सिंचाई लागत (₹) *", "उदा. 1800", isNumber = true)
        inputLabourCost = addInput(card, "4. कटाई व मजदूरी खर्च (₹)", "उदा. 500", isNumber = true).apply {
            setText("0")
        }
        inputTransportCost = addInput(card, "5. परिवहन / भाड़ा खर्च (₹)", "उदा. 250", isNumber = true).apply {
            setText("0")
        }
        inputPackagingCost = addInput(card, "6. बोरी / पैकेजिंग खर्च (₹)", "उदा. 150", isNumber = true).apply {
            setText("0")
        }
        inputMarginPercent = addInput(card, "7. अपेक्षित किसान मुनाफा मार्जिन (%) *", "उदा. 20", isNumber = true).apply {
            setText("20")
        }

        progressBar = ProgressBar(ctx).apply {
            visibility = View.GONE
            setPadding(0, dp(8), 0, dp(8))
        }
        card.addView(progressBar)

        card.addView(
            Button(ctx).apply {
                text = "🧮 उचित प्रत्यक्ष विक्रय भाव निकालें"
                isAllCaps = false
                setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                background = roundedBg("#1B5E20", "#1B5E20", dp(10))
                minHeight = dp(48)
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = dp(12)
                layoutParams = lp
                setOnClickListener { calculateFairDirectPrice() }
            }
        )

        resultBox = TextView(ctx).apply {
            visibility = View.GONE
            background = roundedBg("#E8F5E9", "#81C784", dp(12))
            setTextColor(Color.parseColor("#124116"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(12)
            layoutParams = lp
        }
        card.addView(resultBox)

        card.addView(
            Button(ctx).apply {
                text = "➕ इसी भाव पर अपनी फसल बेचने के लिए सूचीबद्ध करें"
                isAllCaps = false
                setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
                background = roundedBg("#BF360C", "#BF360C", dp(10))
                minHeight = dp(44)
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = dp(10)
                layoutParams = lp
                setOnClickListener {
                    (activity as? MainActivity)?.openAddProductScreen(null)
                }
            }
        )

        root.addView(card)
        scroll.addView(root)
        return scroll
    }

    private fun calculateFairDirectPrice() {
        val ctx = context ?: return
        val qtyKg = inputQuantityKg.text?.toString()?.toDoubleOrNull() ?: 0.0
        if (qtyKg <= 0.0) {
            Toast.makeText(ctx, "कृपया मान्य मात्रा (kg में) दर्ज करें।", Toast.LENGTH_SHORT).show()
            return
        }

        val prodCost = inputProductionCost.text?.toString()?.toDoubleOrNull() ?: 0.0
        val labourCost = inputLabourCost.text?.toString()?.toDoubleOrNull() ?: 0.0
        val transCost = inputTransportCost.text?.toString()?.toDoubleOrNull() ?: 0.0
        val packCost = inputPackagingCost.text?.toString()?.toDoubleOrNull() ?: 0.0
        val marginPct = inputMarginPercent.text?.toString()?.toDoubleOrNull() ?: 15.0

        progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val payload = JSONObject().apply {
                put("quantity_kg", qtyKg)
                put("production_cost_rupees", prodCost + labourCost)
                put("transport_cost_rupees", transCost)
                put("packaging_cost_rupees", packCost)
                put("desired_margin_percent", marginPct)
                put("platform_commission_percent", FeatureManager.getCommissionPercent())
            }

            val res = ApiClient.request(ctx, "/modules/fair-price", method = "POST", bodyJson = payload)
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val d = res.dataObject
            if (res.isSuccess && d != null) {
                val totalCost = d.optDouble("total_cost_rupees", prodCost + labourCost + transCost + packCost)
                val minSustKg = d.optDouble("minimum_sustainable_price_kg_rupees", totalCost / qtyKg)
                val suggestedKg = d.optDouble("suggested_direct_price_kg_rupees", minSustKg * (1 + marginPct / 100.0))
                val suggestedQuintal = String.format("%.0f", suggestedKg * 100)
                val netProfitTotal = String.format("%.0f", (suggestedKg * qtyKg) - totalCost)

                resultBox.visibility = View.VISIBLE
                resultBox.text = "📊 उचित दाम विश्लेषण परिणाम:\n\n" +
                    "• कुल लागत ($qtyKg kg पर): ₹$totalCost\n" +
                    "• न्यूनतम लागत मूल्य (Break-Even): ₹$minSustKg / kg\n" +
                    "• ✅ सुझाया गया प्रत्यक्ष ग्राहक भाव: ₹$suggestedKg / kg (₹$suggestedQuintal / क्विंटल)\n" +
                    "• 🌾 $qtyKg kg पर अनुमानित शुद्ध किसान लाभ: ₹$netProfitTotal"
            } else {
                Toast.makeText(ctx, res.errorMessage ?: "गणना करने में समस्या आई।", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addInput(parent: LinearLayout, label: String, hint: String, isNumber: Boolean): EditText {
        val ctx = requireContext()
        parent.addView(
            TextView(ctx).apply {
                text = label
                setTextColor(Color.parseColor("#1A241A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(8), 0, dp(2))
            }
        )
        val edit = EditText(ctx).apply {
            this.hint = hint
            inputType = if (isNumber) {
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            } else {
                InputType.TYPE_CLASS_TEXT
            }
            background = roundedBg("#FAFAFA", "#B8CBB8", dp(8))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            minHeight = dp(44)
        }
        parent.addView(edit)
        return edit
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
