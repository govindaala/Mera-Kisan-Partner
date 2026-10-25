// app/src/main/java/in/merakisan/app/ui/marketplace/CheckoutBottomSheetFragment.kt
package `in`.merakisan.app.ui.marketplace

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
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.security.SessionManager
import `in`.merakisan.app.ui.main.MainActivity
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

/**
 * MERA KISAN — QUICK CHECKOUT BOTTOM SHEET MODAL
 * Exact Repository Path: app/src/main/java/in/merakisan/app/ui/marketplace/CheckoutBottomSheetFragment.kt
 *
 * Implements (Master Spec Sec 11 & merged.txt Sec 15-20):
 * - Quantity validation bounded between min_order_quantity and stock_quantity.
 * - Delivery mode choice: Farmer Delivery vs Self Pickup.
 * - Real-time Total Order Cost calculation.
 * - Idempotent order placement via ApiClient.createOrder (protects against concurrent duplicate orders).
 */
class CheckoutBottomSheetFragment : BottomSheetDialogFragment() {

    private var productId: String = ""
    private var productName: String = ""
    private var priceRupees: Double = 0.0
    private var unit: String = "kg"
    private var minOrder: Double = 1.0
    private var availableStock: Double = 100.0
    private var farmerName: String = ""

    private lateinit var inputQuantity: EditText
    private lateinit var tvTotalAmount: TextView
    private lateinit var radioGroupDelivery: RadioGroup
    private lateinit var rbDelivery: RadioButton
    private lateinit var rbPickup: RadioButton
    private lateinit var inputShippingAddress: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var btnConfirmOrder: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            productId = it.getString("product_id", "")
            productName = it.getString("product_name", "कृषि उत्पाद")
            priceRupees = it.getDouble("price_rupees", 0.0)
            unit = it.getString("unit", "kg")
            minOrder = it.getDouble("min_order_quantity", 1.0)
            availableStock = it.getDouble("available_stock", 100.0)
            farmerName = it.getString("farmer_name", "किसान")
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
            setBackgroundColor(Color.WHITE)
            setPadding(dp(20), dp(18), dp(20), dp(28))
        }

        root.addView(
            TextView(ctx).apply {
                text = "🛒 ऑर्डर बुक करें: $productName"
                setTextColor(Color.parseColor("#124116"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )

        root.addView(
            TextView(ctx).apply {
                text = "भाव: ₹$priceRupees / $unit  |  👨‍🌾 किसान: $farmerName  |  उपलब्ध: $availableStock $unit"
                setTextColor(Color.parseColor("#4A5A4A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(4), 0, dp(12))
            }
        )

        root.addView(createLabel("1. आवश्यक मात्रा ($unit में — न्यूनतम $minOrder $unit):"))
        inputQuantity = EditText(ctx).apply {
            setText(minOrder.toString())
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            background = roundedBg("#FAFAFA", "#B8CBB8", dp(8))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            minHeight = dp(44)
        }
        root.addView(inputQuantity)

        tvTotalAmount = TextView(ctx).apply {
            text = "💰 कुल अनुमानित राशि: ₹${minOrder * priceRupees}"
            setTextColor(Color.parseColor("#BF360C"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(12))
        }
        root.addView(tvTotalAmount)

        inputQuantity.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.toDoubleOrNull() ?: 0.0
                tvTotalAmount.text = "💰 कुल अनुमानित राशि: ₹${String.format("%.2f", q * priceRupees)}"
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        })

        root.addView(createLabel("2. डिलीवरी या पिकअप माध्यम चुनें:"))
        radioGroupDelivery = RadioGroup(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        rbDelivery = RadioButton(ctx).apply {
            text = "🚚 किसान / स्थानीय डिलीवरी (घर पर डिलीवरी)"
            isChecked = true
        }
        rbPickup = RadioButton(ctx).apply {
            text = "🚜 किसान के खेत / पिकअप पॉइंट से स्वयं प्राप्त करें (Self Pickup)"
        }
        radioGroupDelivery.addView(rbDelivery)
        radioGroupDelivery.addView(rbPickup)
        root.addView(radioGroupDelivery)

        root.addView(createLabel("3. डिलीवरी पता / पिकअप निर्देश *:"))
        inputShippingAddress = EditText(ctx).apply {
            hint = "अपना पूरा पता, गाँव/वार्ड और लैंडमार्क लिखें..."
            setText(
                buildString {
                    val v = SessionManager.getUserVillage(ctx)
                    val d = SessionManager.getUserDistrict(ctx)
                    if (v.isNotBlank()) append("$v, ")
                    if (d.isNotBlank()) append(d)
                }
            )
            background = roundedBg("#FAFAFA", "#B8CBB8", dp(8))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            minHeight = dp(52)
        }
        root.addView(inputShippingAddress)

        progressBar = ProgressBar(ctx).apply {
            visibility = View.GONE
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(progressBar)

        btnConfirmOrder = Button(ctx).apply {
            text = "✅ ऑर्डर पक्का करें (Cash on Delivery / Direct)"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            background = roundedBg("#1B5E20", "#1B5E20", dp(10))
            minHeight = dp(50)
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(14)
            layoutParams = lp
            setOnClickListener { submitDirectOrder() }
        }
        root.addView(btnConfirmOrder)

        return root
    }

    private fun submitDirectOrder() {
        val ctx = context ?: return
        val qty = inputQuantity.text?.toString()?.toDoubleOrNull() ?: 0.0
        val address = inputShippingAddress.text?.toString()?.trim() ?: ""
        val isSelfPickup = rbPickup.isChecked

        if (qty < minOrder || qty > availableStock) {
            Toast.makeText(ctx, "कृपया $minOrder और $availableStock $unit के बीच मात्रा भरें।", Toast.LENGTH_LONG).show()
            return
        }
        if (address.length < 3) {
            Toast.makeText(ctx, "कृपया अपना डिलीवरी पता या पिकअप निर्देश लिखें।", Toast.LENGTH_SHORT).show()
            return
        }

        val idempotencyKey = UUID.randomUUID().toString()
        progressBar.visibility = View.VISIBLE
        btnConfirmOrder.isEnabled = false

        lifecycleScope.launch {
            val payload = JSONObject().apply {
                put("product_id", productId)
                put("quantity", qty)
                put("shipping_address", address)
                put("delivery_option", if (isSelfPickup) "BUYER_PICKUP" else "FARMER_DELIVERY")
                put("payment_method", "COD")
            }

            val res = ApiClient.createOrder(ctx, payload, idempotencyKey)
            progressBar.visibility = View.GONE
            btnConfirmOrder.isEnabled = true

            if (res.isSuccess) {
                Toast.makeText(ctx, "🎉 आपका ऑर्डर सफलतापूर्वक दर्ज हो गया!", Toast.LENGTH_LONG).show()
                dismissAllowingStateLoss()
                (activity as? MainActivity)?.openOrdersScreen()
            } else {
                Toast.makeText(ctx, res.errorMessage ?: "ऑर्डर दर्ज करने में समस्या आई।", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createLabel(textStr: String): TextView =
        TextView(requireContext()).apply {
            text = textStr
            setTextColor(Color.parseColor("#1A241A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(3))
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
