// app/src/main/java/in/merakisan/app/ui/orders/OrderDetailFragment.kt
package `in`.merakisan.app.ui.orders

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
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
import kotlinx.coroutines.launch
import org.json.JSONObject

class OrderDetailFragment : Fragment() {

    private var targetOrderId: String = ""
    private var currentOrderData: JSONObject? = null

    private lateinit var progressBar: ProgressBar
    private lateinit var contentContainer: LinearLayout

    private val disputeCategories = listOf(
        "PRODUCT_QUALITY" to "उत्पाद की गुणवत्ता में अंतर (Product Quality)",
        "QUANTITY_MISMATCH" to "मात्रा कम होना (Quantity Mismatch)",
        "WRONG_PRODUCT" to "गलत उत्पाद प्राप्त होना (Wrong Product)",
        "DELIVERY_DELAY" to "डिलीवरी में अत्यधिक देरी (Delivery Delay)",
        "PAYMENT_ISSUE" to "भुगतान संबंधित समस्या (Payment Issue)",
        "OTHER" to "अन्य समस्या (Other)"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetOrderId = arguments?.getString("order_id") ?: ""
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

        progressBar = ProgressBar(ctx).apply {
            visibility = View.VISIBLE
            setPadding(0, dp(16), 0, dp(16))
        }
        root.addView(progressBar)

        contentContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(contentContainer)

        scroll.addView(root)
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadOrderDetailFromServer()
    }

    private fun loadOrderDetailFromServer() {
        val ctx = context ?: return
        if (targetOrderId.isBlank()) {
            progressBar.visibility = View.GONE
            Toast.makeText(ctx, "ऑर्डर ID उपलब्ध नहीं है।", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        contentContainer.removeAllViews()

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.request(ctx, "/orders/$targetOrderId", method = "GET")
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val ord = res.dataObject
            if (!res.isSuccess || ord == null) {
                contentContainer.addView(
                    TextView(ctx).apply {
                        text = res.errorMessage ?: "ऑर्डर का विवरण लोड करने में समस्या आई।"
                        setTextColor(Color.parseColor("#B71C1C"))
                        setPadding(dp(12), dp(20), dp(12), dp(20))
                    }
                )
                return@launch
            }

            currentOrderData = ord
            renderOrderDetailUi(ord)
        }
    }

    private fun renderOrderDetailUi(ord: JSONObject) {
        val ctx = requireContext()
        val orderId = ord.optString("id", targetOrderId)
        val prodName = ord.optString("product_name", "कृषि उत्पाद")
        val qty = ord.optDouble("quantity", 1.0)
        val unit = ord.optString("unit", "kg")
        val totalPaise = ord.optLong("total_paise", 0L)
        val totalRupees = ord.optDouble("total_rupees", totalPaise / 100.0)
        val farmerNetPaise = ord.optLong("farmer_net_paise", totalPaise)
        val farmerNetRupees = farmerNetPaise / 100.0
        val deliveryFee = ord.optDouble("delivery_fee_rupees", 0.0)

        val status = ord.optString("status", "PLACED").uppercase()
        val paymentMethod = ord.optString("payment_method", "COD")
        val paymentStatus = ord.optString("payment_status", "COD_PENDING")
        val createdAt = ord.optString("created_at", "").take(19).replace("T", " ")
        val shippingAddress = ord.optString("shipping_address", "")
        val buyerName = ord.optString("buyer_name", "खरीदार")
        val buyerPhone = ord.optString("buyer_phone", "")
        val farmerName = ord.optString("farmer_name", "किसान")
        val farmerPhone = ord.optString("farmer_phone", "")
        val farmerUid = ord.optString("farmer_uid", ord.optString("seller_uid", ""))

        val myUid = SessionManager.getUserUid(ctx) ?: ""
        val amIFarmer = myUid == farmerUid

        val statusCard = createCard()
        val statusTitleHi = when (status) {
            "PLACED" -> "🆕 नया ऑर्डर (स्वीकृति बाकी)"
            "ACCEPTED" -> "✅ किसान द्वारा स्वीकृत"
            "PROCESSING" -> "⚙️ पैकिंग व तैयारी जारी"
            "READY" -> "📦 डिस्पैच / पिकअप के लिए तैयार"
            "SHIPPED" -> "🚚 रवाना हो चुका है (In Transit)"
            "DELIVERED" -> "📍 पहुँच चुका है (खरीदार पुष्टि बाकी)"
            "COMPLETED" -> "🎉 सफलतापूर्वक पूर्ण (COMPLETED)"
            "DISPUTED" -> "⚖️ विवादित (Dispute Under Review)"
            "CANCELLED" -> "❌ रद्द (Cancelled)"
            "REJECTED" -> "❌ अस्वीकृत (Rejected)"
            else -> status
        }

        statusCard.addView(
            TextView(ctx).apply {
                text = "ऑर्डर #$orderId"
                setTextColor(Color.parseColor("#4A5A4A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            }
        )
        statusCard.addView(
            TextView(ctx).apply {
                text = statusTitleHi
                setTextColor(
                    when (status) {
                        "COMPLETED" -> Color.parseColor("#1B5E20")
                        "DISPUTED", "CANCELLED", "REJECTED" -> Color.parseColor("#B71C1C")
                        else -> Color.parseColor("#0D47A1")
                    }
                )
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(4), 0, dp(6))
            }
        )

        statusCard.addView(renderVisualOrderStepper(status))
        contentContainer.addView(statusCard)

        val itemCard = createCard()
        itemCard.addView(createSectionHeading("🌾 उत्पाद व मूल्य विवरण"))
        itemCard.addView(
            TextView(ctx).apply {
                text = "$prodName — $qty $unit"
                setTextColor(Color.parseColor("#1A241A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(2), 0, dp(6))
            }
        )

        val priceBreakdown = buildString {
            append("• कुल ऑर्डर मूल्य: ₹$totalRupees\n")
            if (deliveryFee > 0) append("• डिलीवरी / भाड़ा शुल्क: ₹$deliveryFee\n")
            append("• भुगतान माध्यम: $paymentMethod ($paymentStatus)\n")
            if (amIFarmer) {
                append("• किसान को शुद्ध प्राप्ति: ₹$farmerNetRupees\n")
            }
            append("• ऑर्डर समय: $createdAt")
        }

        itemCard.addView(
            TextView(ctx).apply {
                text = priceBreakdown
                setTextColor(Color.parseColor("#BF360C"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )
        contentContainer.addView(itemCard)

        val contactCard = createCard()
        contactCard.addView(createSectionHeading("📍 संपर्क व डिलीवरी पता"))

        val detailsText = if (amIFarmer) {
            "🛒 खरीदार: $buyerName\n📍 डिलीवरी/पिकअप पता: $shippingAddress"
        } else {
            "👨‍🌾 किसान: $farmerName\n📍 आपका डिलीवरी पता: $shippingAddress"
        }

        contactCard.addView(
            TextView(ctx).apply {
                text = detailsText
                setTextColor(Color.parseColor("#333333"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(0, 0, 0, dp(10))
            }
        )

        val contactPhone = if (amIFarmer) buyerPhone else farmerPhone
        if (contactPhone.isNotBlank() && contactPhone != "null") {
            val contactRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }

            val btnCall = Button(ctx).apply {
                text = "📞 कॉल करें ($contactPhone)"
                isAllCaps = false
                setTextColor(Color.WHITE)
                background = roundedBg("#0D47A1", "#0D47A1", dp(8))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = dp(6)
                }
                setOnClickListener {
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contactPhone")))
                }
            }

            val btnWa = Button(ctx).apply {
                text = "🟢 WhatsApp"
                isAllCaps = false
                setTextColor(Color.WHITE)
                background = roundedBg("#2E7D32", "#2E7D32", dp(8))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    val clean = contactPhone.replace("\\D".toRegex(), "")
                    val intl = if (clean.length == 10) "91$clean" else clean
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://wa.me/$intl?text=${Uri.encode("नमस्ते, यह 'मेरा किसान' ऑर्डर #$orderId ($prodName) के संबंध में है।")}")
                        )
                    )
                }
            }

            contactRow.addView(btnCall)
            contactRow.addView(btnWa)
            contactCard.addView(contactRow)
        }
        contentContainer.addView(contactCard)

        val actionCard = createCard()
        actionCard.addView(createSectionHeading("⚙️ ऑर्डर कार्रवाई"))
        val actionRow = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        if (amIFarmer) {
            when (status) {
                "PLACED" -> {
                    actionRow.addView(
                        createActionButton("✅ ऑर्डर स्वीकार करें (Accept Order)", "#1B5E20") {
                            updateOrderStatus(orderId, "ACCEPTED")
                        }
                    )
                    actionRow.addView(
                        createActionButton("❌ ऑर्डर अस्वीकार करें (Reject)", "#B71C1C") {
                            updateOrderStatus(orderId, "REJECTED")
                        }
                    )
                }
                "ACCEPTED", "PROCESSING" -> {
                    actionRow.addView(
                        createActionButton("📦 माल पैक हो गया — तैयार है (READY)", "#0D47A1") {
                            updateOrderStatus(orderId, "READY")
                        }
                    )
                    actionRow.addView(
                        createActionButton("🚚 रवाना किया (SHIPPED)", "#1B5E20") {
                            updateOrderStatus(orderId, "SHIPPED")
                        }
                    )
                }
                "READY", "SHIPPED" -> {
                    actionRow.addView(
                        createActionButton("📍 डिलीवर हो गया मार्क करें (DELIVERED)", "#1B5E20") {
                            updateOrderStatus(orderId, "DELIVERED")
                        }
                    )
                }
            }
        } else {
            when (status) {
                "PLACED" -> {
                    actionRow.addView(
                        createActionButton("❌ ऑर्डर रद्द करें (Cancel Order)", "#B71C1C") {
                            updateOrderStatus(orderId, "CANCELLED")
                        }
                    )
                }
                "READY", "SHIPPED", "DELIVERED" -> {
                    actionRow.addView(
                        createActionButton("🎉 माल मिल गया — डिलीवरी की पुष्टि करें (Confirm)", "#1B5E20") {
                            updateOrderStatus(orderId, "COMPLETED")
                        }
                    )
                }
            }
        }

        if (status == "COMPLETED" && !amIFarmer && FeatureManager.isEnabled("ratings", ctx)) {
            actionRow.addView(
                createActionButton("⭐ किसान को सत्यापित रेटिंग व रिव्यू दें", "#F57F17") {
                    showRatingDialog(orderId, prodName, farmerName)
                }
            )
        }

        if (status !in listOf("CANCELLED", "REJECTED", "DISPUTED", "REFUNDED")) {
            actionRow.addView(
                createActionButton("⚖️ कोई समस्या है? विवाद दर्ज करें (Raise Dispute)", "#4E342E") {
                    showRaiseDisputeDialog(orderId, prodName)
                }
            )
        }

        actionCard.addView(actionRow)
        contentContainer.addView(actionCard)
    }

    private fun renderVisualOrderStepper(currentStatus: String): View {
        val ctx = requireContext()
        val steps = listOf("PLACED", "ACCEPTED", "SHIPPED", "DELIVERED", "COMPLETED")
        val currentIdx = steps.indexOf(currentStatus).let { if (it >= 0) it else 0 }

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(4))
        }

        for (i in steps.indices) {
            val isDone = i <= currentIdx
            val dot = View(ctx).apply {
                val size = dp(14)
                layoutParams = LinearLayout.LayoutParams(size, size)
                background = roundedBg(
                    if (isDone) "#1B5E20" else "#CCCCCC",
                    if (isDone) "#1B5E20" else "#AAAAAA",
                    size / 2
                )
            }
            row.addView(dot)

            if (i < steps.size - 1) {
                val line = View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(3), 1f)
                    setBackgroundColor(if (i < currentIdx) Color.parseColor("#1B5E20") else Color.parseColor("#DDDDDD"))
                }
                row.addView(line)
            }
        }
        return row
    }

    private fun updateOrderStatus(orderId: String, nextStatus: String) {
        val ctx = context ?: return
        progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.updateOrderStatus(ctx, orderId, nextStatus)
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            if (res.isSuccess) {
                Toast.makeText(ctx, "✅ ऑर्डर स्थिति '$nextStatus' पर अपडेट हो गई!", Toast.LENGTH_SHORT).show()
                loadOrderDetailFromServer()
            } else {
                Toast.makeText(ctx, res.errorMessage ?: "स्टेटस बदलने में त्रुटि।", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showRatingDialog(orderId: String, prodName: String, farmerName: String) {
        val ctx = context ?: return
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }

        val starOptions = listOf(
            "5 ⭐⭐⭐⭐⭐ (उत्कृष्ट)",
            "4 ⭐⭐⭐⭐ (बहुत अच्छा)",
            "3 ⭐⭐⭐ (सामान्य)",
            "2 ⭐⭐ (सुधार आवश्यक)",
            "1 ⭐ (असंतुष्ट)"
        )

        val spinner = Spinner(ctx).apply {
            adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, starOptions)
        }
        val commentInput = EditText(ctx).apply {
            hint = "उत्पाद की गुणवत्ता और किसान के व्यवहार पर अपनी राय लिखें..."
        }

        layout.addView(spinner)
        layout.addView(commentInput)

        AlertDialog.Builder(ctx)
            .setTitle("⭐ रेटिंग दें: $prodName ($farmerName)")
            .setView(layout)
            .setPositiveButton("रेटिंग सेव करें") { _, _ ->
                val ratingVal = 5 - spinner.selectedItemPosition
                val comment = commentInput.text?.toString()?.trim() ?: ""

                viewLifecycleOwner.lifecycleScope.launch {
                    val payload = JSONObject().apply {
                        put("order_id", orderId)
                        put("rating", ratingVal)
                        put("quality_rating", ratingVal)
                        put("communication_rating", ratingVal)
                        put("comment", comment)
                    }
                    val res = ApiClient.submitReview(ctx, payload)
                    if (res.isSuccess) {
                        Toast.makeText(ctx, "🙏 आपकी सत्यापित रेटिंग दर्ज हो गई!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(ctx, res.errorMessage ?: "रेटिंग देने में समस्या आई।", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("बंद करें", null)
            .show()
    }

    private fun showRaiseDisputeDialog(orderId: String, prodName: String) {
        val ctx = context ?: return
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }

        val catSpinner = Spinner(ctx).apply {
            adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, disputeCategories.map { it.second })
        }
        val descInput = EditText(ctx).apply {
            hint = "समस्या का पूरा विवरण लिखें (कम से कम 10 अक्षर) *"
        }

        layout.addView(catSpinner)
        layout.addView(descInput)

        AlertDialog.Builder(ctx)
            .setTitle("⚖️ विवाद दर्ज करें (#$orderId)")
            .setMessage("उत्पाद: $prodName")
            .setView(layout)
            .setPositiveButton("विवाद दर्ज करें") { _, _ ->
                val catCode = disputeCategories.getOrNull(catSpinner.selectedItemPosition)?.first ?: "OTHER"
                val desc = descInput.text?.toString()?.trim() ?: ""

                if (desc.length < 10) {
                    Toast.makeText(ctx, "कृपया समस्या का स्पष्ट विवरण लिखें।", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    val payload = JSONObject().apply {
                        put("order_id", orderId)
                        put("category", catCode)
                        put("description", desc)
                    }
                    val res = ApiClient.createDispute(ctx, payload)
                    if (res.isSuccess) {
                        Toast.makeText(ctx, "⚖️ आपका विवाद दर्ज हो गया है। एडमिन टीम इसकी जाँच करेगी।", Toast.LENGTH_LONG).show()
                        loadOrderDetailFromServer()
                    } else {
                        Toast.makeText(ctx, res.errorMessage ?: "विवाद दर्ज करने में त्रुटि।", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun createCard(): LinearLayout {
        val ctx = requireContext()
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#FFFFFF", "#D4E2D4", dp(14))
            setPadding(dp(16), dp(14), dp(16), dp(14))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(10)
            layoutParams = lp
        }
    }

    private fun createSectionHeading(title: String): TextView =
        TextView(requireContext()).apply {
            text = title
            setTextColor(Color.parseColor("#124116"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        }

    private fun createActionButton(label: String, bgHex: String, onClick: () -> Unit): Button =
        Button(requireContext()).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            background = roundedBg(bgHex, bgHex, dp(8))
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
