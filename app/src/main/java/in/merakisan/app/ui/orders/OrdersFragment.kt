// app/src/main/java/in/merakisan/app/ui/orders/OrdersFragment.kt
package `in`.merakisan.app.ui.orders

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
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
import `in`.merakisan.app.ui.main.MainActivity
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * MERA KISAN — UNIFIED ORDERS, STATE MACHINE, DISPUTE & COMPLETED-ORDER RATING SCREEN
 * Exact Repository Path: app/src/main/java/in/merakisan/app/ui/orders/OrdersFragment.kt
 *
 * Implements (Master Spec Sec 9, 11 & merged.txt Sec 15-20, 41, 50):
 * - Role-enforced state transitions (PLACED -> ACCEPTED -> READY -> SHIPPED -> DELIVERED -> COMPLETED).
 * - Multi-parameter Rating & Review ONLY after order reaches COMPLETED state.
 * - Atomic Dispute filing (`POST /disputes`) with category & evidence notes.
 */
class OrdersFragment : Fragment() {

    private lateinit var tvHeaderTitle: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var ordersListContainer: LinearLayout

    private val disputeCategories = listOf(
        "PRODUCT_QUALITY" to "उत्पाद की गुणवत्ता में अंतर (Product Quality)",
        "QUANTITY_MISMATCH" to "मात्रा कम या गलत होना (Quantity Mismatch)",
        "DELIVERY_DELAY" to "डिलीवरी / पिकअप में देरी (Delivery Delay)",
        "WRONG_PRODUCT" to "गलत उत्पाद प्राप्त होना (Wrong Product)",
        "PAYMENT_ISSUE" to "भुगतान संबंधित समस्या (Payment Issue)",
        "OTHER" to "अन्य समस्या (Other)"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val isFarmer = SessionManager.isFarmerModeActive(ctx)

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

        tvHeaderTitle = TextView(ctx).apply {
            text = if (isFarmer) {
                "📦 किसानों को प्राप्त ग्राहक ऑर्डर्स"
            } else {
                "🛒 मेरे द्वारा किए गए ऑर्डर्स"
            }
            setTextColor(Color.parseColor("#124116"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
            setTypeface(typeface, Typeface.BOLD)
        }
        headerCard.addView(tvHeaderTitle)

        val subRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }

        val btnRefresh = createButton("🔄 ऑर्डर्स रिफ्रेश करें", "#1B5E20") {
            loadOrdersFromServer()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(6)
            }
        }

        val btnOffers = createButton("🤝 मोलभाव (Offers) देखें", "#0D47A1") {
            (activity as? MainActivity)?.openNegotiationOffersScreen()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(6)
            }
        }

        subRow.addView(btnRefresh)
        subRow.addView(btnOffers)
        headerCard.addView(subRow)
        root.addView(headerCard)

        progressBar = ProgressBar(ctx).apply {
            visibility = View.GONE
            setPadding(0, dp(12), 0, dp(12))
        }
        root.addView(progressBar)

        ordersListContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        root.addView(ordersListContainer)

        scroll.addView(root)
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadOrdersFromServer()
    }

    private fun loadOrdersFromServer() {
        val ctx = context ?: return
        val roleView = if (SessionManager.isFarmerModeActive(ctx)) "farmer" else "buyer"
        progressBar.visibility = View.VISIBLE
        ordersListContainer.removeAllViews()

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.getOrders(ctx, viewRole = roleView)
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val arr = res.dataArray ?: JSONArray()
            if (arr.length() == 0) {
                ordersListContainer.addView(
                    TextView(ctx).apply {
                        text = "अभी कोई ऑर्डर उपलब्ध नहीं है।"
                        setTextColor(Color.parseColor("#555555"))
                        setPadding(dp(8), dp(18), dp(8), dp(18))
                    }
                )
                return@launch
            }

            val myUid = SessionManager.getUserUid(ctx) ?: ""
            for (i in 0 until arr.length()) {
                val ord = arr.optJSONObject(i) ?: continue
                ordersListContainer.addView(buildOrderCard(ord, myUid))
            }
        }
    }

    private fun buildOrderCard(order: JSONObject, myUid: String): View {
        val ctx = requireContext()
        val orderId = order.optString("id", "")
        val prodName = order.optString("product_name", "कृषि उत्पाद")
        val qty = order.optDouble("quantity", 1.0)
        val unit = order.optString("unit", "kg")
        val totalPaise = order.optLong("total_paise", 0L)
        val totalRupees = order.optDouble("total_rupees", totalPaise / 100.0)
        val farmerNetPaise = order.optLong("farmer_net_paise", totalPaise)
        val farmerNetRupees = farmerNetPaise / 100.0

        val buyerUid = order.optString("buyer_uid", "")
        val buyerName = order.optString("buyer_name", "खरीदार")
        val buyerPhone = order.optString("buyer_phone", "")
        val farmerUid = order.optString("farmer_uid", order.optString("seller_uid", ""))
        val farmerName = order.optString("farmer_name", "किसान")
        val farmerPhone = order.optString("farmer_phone", "")
        val shippingAddress = order.optString("shipping_address", "")
        val paymentMethod = order.optString("payment_method", "COD")
        val paymentStatus = order.optString("payment_status", "COD_PENDING")
        val status = order.optString("status", "PLACED").uppercase()
        val createdAt = order.optString("created_at", "").take(10)

        val amIFarmer = myUid == farmerUid
        val amIBuyer = myUid == buyerUid || !amIFarmer

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

        val statusBadgeHi = when (status) {
            "PLACED" -> "🆕 नया ऑर्डर (किसान की स्वीकृति बाकी)"
            "ACCEPTED" -> "✅ किसान द्वारा स्वीकृत"
            "PROCESSING" -> "⚙️ पैकिंग / तैयारी चल रही है"
            "READY" -> "📦 पिकअप / डिस्पैच के लिए तैयार"
            "SHIPPED" -> "🚚 रवाना किया जा चुका है (In Transit)"
            "DELIVERED" -> "📍 पहुँच चुका है (खरीदार पुष्टि बाकी)"
            "COMPLETED" -> "🎉 सफलतापूर्वक पूर्ण (COMPLETED)"
            "DISPUTED" -> "⚖️ विवादित (Dispute Under Review)"
            "CANCELLED", "REJECTED" -> "❌ रद्द / अस्वीकृत ($status)"
            else -> status
        }

        card.addView(
            TextView(ctx).apply {
                text = "$statusBadgeHi • #$orderId"
                setTextColor(
                    when (status) {
                        "COMPLETED" -> Color.parseColor("#1B5E20")
                        "DISPUTED", "CANCELLED", "REJECTED" -> Color.parseColor("#B71C1C")
                        else -> Color.parseColor("#0D47A1")
                    }
                )
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )

        card.addView(
            TextView(ctx).apply {
                text = "$prodName — $qty $unit"
                setTextColor(Color.parseColor("#1A241A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(4), 0, dp(2))
            }
        )

        val amountLine = if (amIFarmer) {
            "💰 कुल ऑर्डर: ₹$totalRupees (किसान शुद्ध प्राप्ति: ₹$farmerNetRupees) • $paymentMethod ($paymentStatus)"
        } else {
            "💰 कुल देय राशि: ₹$totalRupees • भुगतान माध्यम: $paymentMethod ($paymentStatus)"
        }

        card.addView(
            TextView(ctx).apply {
                text = amountLine
                setTextColor(Color.parseColor("#BF360C"))
                setTypeface(typeface, Typeface.BOLD)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            }
        )

        card.addView(
            TextView(ctx).apply {
                text = "👨‍🌾 किसान: $farmerName  |  🛒 खरीदार: $buyerName • 📅 $createdAt\n📍 पता/पिकअप: $shippingAddress"
                setTextColor(Color.parseColor("#4A5A4A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, dp(4), 0, dp(8))
            }
        )

        // Role-Enforced State Machine Action Buttons
        val stateBtnRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }

        if (amIFarmer) {
            when (status) {
                "PLACED" -> {
                    stateBtnRow.addView(
                        createSmallBtn("✅ स्वीकार करें", "#1B5E20") {
                            transitionOrderStatus(orderId, "ACCEPTED")
                        }
                    )
                    stateBtnRow.addView(
                        createSmallBtn("❌ अस्वीकार करें", "#B71C1C") {
                            transitionOrderStatus(orderId, "REJECTED")
                        }
                    )
                }
                "ACCEPTED", "PROCESSING" -> {
                    stateBtnRow.addView(
                        createSmallBtn("📦 तैयार है (READY)", "#0D47A1") {
                            transitionOrderStatus(orderId, "READY")
                        }
                    )
                    stateBtnRow.addView(
                        createSmallBtn("🚚 रवाना (SHIPPED)", "#1B5E20") {
                            transitionOrderStatus(orderId, "SHIPPED")
                        }
                    )
                }
                "READY", "SHIPPED" -> {
                    stateBtnRow.addView(
                        createSmallBtn("📍 डिलीवर हो गया (DELIVERED)", "#1B5E20") {
                            transitionOrderStatus(orderId, "DELIVERED")
                        }
                    )
                }
            }
        }

        if (amIBuyer) {
            when (status) {
                "PLACED" -> {
                    stateBtnRow.addView(
                        createSmallBtn("❌ ऑर्डर रद्द करें", "#B71C1C") {
                            transitionOrderStatus(orderId, "CANCELLED")
                        }
                    )
                }
                "READY", "SHIPPED", "DELIVERED" -> {
                    stateBtnRow.addView(
                        createSmallBtn("🎉 माल मिल गया — डिलीवरी कन्फर्म करें", "#1B5E20") {
                            transitionOrderStatus(orderId, "COMPLETED")
                        }
                    )
                }
            }
        }

        if (stateBtnRow.childCount > 0) {
            card.addView(stateBtnRow)
        }

        // Secondary Actions Row: [ ⭐ Rate Order (if COMPLETED) ] [ ⚖️ Raise Dispute ] [ 📞 Call Counterparty ]
        val extraRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }

        if (status == "COMPLETED" && amIBuyer && FeatureManager.isEnabled("ratings", ctx)) {
            extraRow.addView(
                createSmallBtn("⭐ किसान को रेटिंग दें", "#F57F17") {
                    showRatingDialog(orderId, prodName, farmerName)
                }
            )
        }

        if (status !in listOf("CANCELLED", "REJECTED", "DISPUTED", "REFUNDED")) {
            extraRow.addView(
                createSmallBtn("⚖️ समस्या / विवाद दर्ज करें", "#4E342E") {
                    showRaiseDisputeDialog(orderId, prodName)
                }
            )
        }

        val counterpartyPhone = if (amIFarmer) buyerPhone else farmerPhone
        if (counterpartyPhone.isNotBlank() && counterpartyPhone != "null") {
            extraRow.addView(
                createSmallBtn("📞 कॉल", "#0D47A1") {
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$counterpartyPhone")))
                }
            )
        }

        if (extraRow.childCount > 0) {
            card.addView(extraRow)
        }

        return card
    }

    private fun transitionOrderStatus(orderId: String, nextStatus: String) {
        val ctx = context ?: return
        progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.updateOrderStatus(ctx, orderId, nextStatus)
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            if (res.isSuccess) {
                Toast.makeText(ctx, "✅ ऑर्डर स्थिति '$nextStatus' पर अपडेट हो गई!", Toast.LENGTH_SHORT).show()
                loadOrdersFromServer()
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

        val overallSpinner = Spinner(ctx).apply {
            adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, starOptions)
        }
        val commentInput = EditText(ctx).apply {
            hint = "उत्पाद की गुणवत्ता और किसान के व्यवहार पर अपनी राय लिखें..."
        }

        layout.addView(overallSpinner)
        layout.addView(commentInput)

        AlertDialog.Builder(ctx)
            .setTitle("⭐ रेटिंग दें: $prodName ($farmerName)")
            .setView(layout)
            .setPositiveButton("रेटिंग सेव करें") { _, _ ->
                val ratingNum = 5 - overallSpinner.selectedItemPosition
                val comment = commentInput.text?.toString()?.trim() ?: ""

                viewLifecycleOwner.lifecycleScope.launch {
                    val payload = JSONObject().apply {
                        put("order_id", orderId)
                        put("rating", ratingNum)
                        put("quality_rating", ratingNum)
                        put("communication_rating", ratingNum)
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
            adapter = ArrayAdapter(
                ctx,
                android.R.layout.simple_spinner_dropdown_item,
                disputeCategories.map { it.second }
            )
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
                    Toast.makeText(ctx, "कृपया समस्या का स्पष्ट विवरण (कम से कम 10 अक्षर) लिखें।", Toast.LENGTH_LONG).show()
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
                        loadOrdersFromServer()
                    } else {
                        Toast.makeText(ctx, res.errorMessage ?: "विवाद दर्ज करने में त्रुटि।", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("रद्द करें", null)
            .show()
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

    private fun createSmallBtn(label: String, bgHex: String, onClick: () -> Unit): Button =
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
