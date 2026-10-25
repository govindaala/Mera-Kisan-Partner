// app/src/main/java/in/merakisan/app/ui/marketplace/MakeOfferDialogFragment.kt
package `in`.merakisan.app.ui.marketplace

import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.ui.main.MainActivity
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * MERA KISAN — DEDICATED MAKE OFFER / NEGOTIATION DIALOG
 * Exact Repository Path: app/src/main/java/in/merakisan/app/ui/marketplace/MakeOfferDialogFragment.kt
 *
 * Implements (Master Spec Sec 8 & merged.txt Sec 14, 18, 39, 76):
 * - Displays current listed price and calculates total savings in real-time.
 * - Validates offered price and bounded quantity.
 * - Posts offer via ApiClient.createOffer (`POST /offers`).
 * - Navigates directly to NegotiationHistoryFragment on success.
 */
class MakeOfferDialogFragment : DialogFragment() {

    private var productId: String = ""
    private var productName: String = ""
    private var listedPriceRupees: Double = 0.0
    private var unit: String = "kg"
    private var minOrder: Double = 1.0
    private var availableStock: Double = 100.0

    private lateinit var inputOfferedPrice: EditText
    private lateinit var inputQuantity: EditText
    private lateinit var inputMessage: EditText
    private lateinit var tvTotalSummary: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSubmitOffer: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            productId = it.getString("product_id", "")
            productName = it.getString("product_name", "कृषि उत्पाद")
            listedPriceRupees = it.getDouble("listed_price", 0.0)
            unit = it.getString("unit", "kg")
            minOrder = it.getDouble("min_order", 1.0)
            availableStock = it.getDouble("available_stock", 100.0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#FFFFFF", "#C8E6C9", dp(16))
            setPadding(dp(22), dp(20), dp(22), dp(24))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        root.addView(
            TextView(ctx).apply {
                text = "🤝 भाव प्रस्ताव दें (Make an Offer)"
                setTextColor(Color.parseColor("#124116"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )

        root.addView(
            TextView(ctx).apply {
                text = "$productName — वर्तमान भाव: ₹$listedPriceRupees / $unit (उपलब्ध: $availableStock $unit)"
                setTextColor(Color.parseColor("#4A5A4A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(4), 0, dp(12))
            }
        )

        root.addView(createLabel("1. आपका प्रस्तावित भाव (₹ प्रति $unit) *: "))
        inputOfferedPrice = EditText(ctx).apply {
            hint = "उदा. ${if (listedPriceRupees > 5) (listedPriceRupees - 3).toInt() else listedPriceRupees.toInt()}"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            background = roundedBg("#FAFAFA", "#B8CBB8", dp(8))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            minHeight = dp(44)
        }
        root.addView(inputOfferedPrice)

        root.addView(createLabel("2. आवश्यक मात्रा ($unit में — न्यूनतम $minOrder) *: "))
        inputQuantity = EditText(ctx).apply {
            setText(minOrder.toString())
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            background = roundedBg("#FAFAFA", "#B8CBB8", dp(8))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            minHeight = dp(44)
        }
        root.addView(inputQuantity)

        tvTotalSummary = TextView(ctx).apply {
            text = "प्रस्तावित कुल मूल्य: ₹0"
            setTextColor(Color.parseColor("#BF360C"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(6), 0, dp(8))
        }
        root.addView(tvTotalSummary)

        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updateSummary() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        }
        inputOfferedPrice.addTextChangedListener(watcher)
        inputQuantity.addTextChangedListener(watcher)

        root.addView(createLabel("3. किसान के लिए संदेश (वैकल्पिक):"))
        inputMessage = EditText(ctx).apply {
            hint = "उदा. यदि माल अच्छा है तो हम नियमित रूप से लेंगे..."
            background = roundedBg("#FAFAFA", "#B8CBB8", dp(8))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            minHeight = dp(44)
        }
        root.addView(inputMessage)

        progressBar = ProgressBar(ctx).apply {
            visibility = View.GONE
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(progressBar)

        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(14), 0, 0)
        }

        val btnCancel = Button(ctx).apply {
            text = "रद्द करें"
            isAllCaps = false
            setTextColor(Color.parseColor("#555555"))
            background = roundedBg("#EEEEEE", "#CCCCCC", dp(8))
            minHeight = dp(46)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(6)
            }
            setOnClickListener { dismissAllowingStateLoss() }
        }

        btnSubmitOffer = Button(ctx).apply {
            text = "🤝 प्रस्ताव भेजें"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            background = roundedBg("#1B5E20", "#1B5E20", dp(8))
            minHeight = dp(46)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(6)
            }
            setOnClickListener { submitOffer() }
        }

        btnRow.addView(btnCancel)
        btnRow.addView(btnSubmitOffer)
        root.addView(btnRow)

        return root
    }

    private fun updateSummary() {
        val price = inputOfferedPrice.text?.toString()?.toDoubleOrNull() ?: 0.0
        val qty = inputQuantity.text?.toString()?.toDoubleOrNull() ?: 0.0
        val total = price * qty
        val originalTotal = listedPriceRupees * qty
        val diff = originalTotal - total

        tvTotalSummary.text = if (total > 0) {
            "💰 प्रस्तावित कुल: ₹${String.format("%.2f", total)} (${if (diff > 0) "बचत: ₹${String.format("%.2f", diff)}" else "समान भाव"})"
        } else {
            "प्रस्तावित कुल मूल्य: ₹0"
        }
    }

    private fun submitOffer() {
        val ctx = context ?: return
        val offPrice = inputOfferedPrice.text?.toString()?.toDoubleOrNull() ?: 0.0
        val qty = inputQuantity.text?.toString()?.toDoubleOrNull() ?: 0.0
        val msg = inputMessage.text?.toString()?.trim() ?: ""

        if (offPrice <= 0.0) {
            Toast.makeText(ctx, "कृपया अपना प्रस्तावित भाव दर्ज करें।", Toast.LENGTH_SHORT).show()
            return
        }
        if (qty < minOrder || qty > availableStock) {
            Toast.makeText(ctx, "मात्रा $minOrder और $availableStock $unit के बीच होनी चाहिए।", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnSubmitOffer.isEnabled = false

        lifecycleScope.launch {
            val payload = JSONObject().apply {
                put("product_id", productId)
                put("offered_price_paise", Math.round(offPrice * 100))
                put("quantity", qty)
                put("message", msg)
            }

            val res = ApiClient.createOffer(ctx, payload)
            progressBar.visibility = View.GONE
            btnSubmitOffer.isEnabled = true

            if (res.isSuccess) {
                Toast.makeText(ctx, "✅ आपका भाव प्रस्ताव किसान को भेज दिया गया!", Toast.LENGTH_LONG).show()
                dismissAllowingStateLoss()
                (activity as? MainActivity)?.openNegotiationOffersScreen()
            } else {
                Toast.makeText(ctx, res.errorMessage ?: "प्रस्ताव भेजने में समस्या आई।", Toast.LENGTH_LONG).show()
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
