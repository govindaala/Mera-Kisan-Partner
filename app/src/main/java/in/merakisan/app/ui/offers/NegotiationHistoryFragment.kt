// app/src/main/java/in/merakisan/app/ui/offers/NegotiationHistoryFragment.kt
package `in`.merakisan.app.ui.offers

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
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.security.SessionManager
import `in`.merakisan.app.ui.main.MainActivity
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * MERA KISAN — NEGOTIATION & COUNTER-OFFER CENTER
 * Exact Repository Path: app/src/main/java/in/merakisan/app/ui/offers/NegotiationHistoryFragment.kt
 *
 * Implements (Master Spec Sec 8 & merged.txt Sec 14, 18, 39, 76):
 * - Displays all active & historical negotiation offers for the logged-in Farmer/Buyer.
 * - Enforces counterparty turn rules: only the receiving party can ACCEPT an offer/counter-offer.
 * - Supports ACCEPT (atomically locks stock & creates an Order on backend), COUNTER, and REJECT.
 */
class NegotiationHistoryFragment : Fragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var offersContainer: LinearLayout

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
                text = "🤝 मोलभाव व भाव प्रस्ताव केंद्र (Offers & Counter-Offers)"
                setTextColor(Color.parseColor("#124116"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )
        headerCard.addView(
            TextView(ctx).apply {
                text = "प्रस्ताव स्वीकार करते ही सहमति वाले भाव पर स्टॉक रिज़र्व होकर पक्का ऑर्डर बन जाता है।"
                setTextColor(Color.parseColor("#4A5A4A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(4), 0, dp(8))
            }
        )

        headerCard.addView(
            Button(ctx).apply {
                text = "🔄 प्रस्ताव सूची रिफ्रेश करें"
                isAllCaps = false
                setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
                background = roundedBg("#1B5E20", "#1B5E20", dp(10))
                minHeight = dp(42)
                setOnClickListener { loadOffersFromServer() }
            }
        )
        root.addView(headerCard)

        progressBar = ProgressBar(ctx).apply {
            visibility = View.GONE
            setPadding(0, dp(12), 0, dp(12))
        }
        root.addView(progressBar)

        offersContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        root.addView(offersContainer)

        scroll.addView(root)
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadOffersFromServer()
    }

    private fun loadOffersFromServer() {
        val ctx = context ?: return
        progressBar.visibility = View.VISIBLE
        offersContainer.removeAllViews()

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.getOffers(ctx)
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val arr = res.dataArray ?: JSONArray()
            if (arr.length() == 0) {
                offersContainer.addView(
                    TextView(ctx).apply {
                        text = "अभी कोई मोलभाव प्रस्ताव नहीं है। किसी भी उत्पाद पेज पर '🤝 भाव प्रस्ताव दें' से शुरुआत करें।"
                        setTextColor(Color.parseColor("#555555"))
                        setPadding(dp(8), dp(18), dp(8), dp(18))
                    }
                )
                return@launch
            }

            val myUid = SessionManager.getUserUid(ctx) ?: ""
            for (i in 0 until arr.length()) {
                val offerObj = arr.optJSONObject(i) ?: continue
                offersContainer.addView(buildOfferCard(offerObj, myUid))
            }
        }
    }

    private fun buildOfferCard(offer: JSONObject, myUid: String): View {
        val ctx = requireContext()
        val offerId = offer.optString("id", "")
        val prodName = offer.optString("product_name", "कृषि उत्पाद")
        val unit = offer.optString("unit", "kg")
        val qty = offer.optDouble("quantity", 1.0)
        val origPaise = offer.optLong("original_price_paise", 0L)
        val activePaise = offer.optLong("active_price_paise", offer.optLong("offered_price_paise", 0L))
        val origRupees = origPaise / 100.0
        val activeRupees = activePaise / 100.0

        val buyerUid = offer.optString("buyer_uid", "")
        val buyerName = offer.optString("buyer_name", "खरीदार")
        val farmerUid = offer.optString("farmer_uid", "")
        val farmerName = offer.optString("farmer_name", "किसान")
        val status = offer.optString("status", "PENDING").uppercase()
        val lastActorRole = offer.optString("last_actor_role", "BUYER").uppercase()
        val lockedOrderId = offer.optString("locked_order_id", "")
        val message = offer.optString("message", "")

        val amIFarmer = myUid == farmerUid
        val myActorRole = if (amIFarmer) "FARMER" else "BUYER"
        val isOpenForAction = status in listOf("PENDING", "COUNTERED_BY_FARMER", "COUNTERED_BY_BUYER")
        val canIAccept = isOpenForAction && lastActorRole != myActorRole

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
            "PENDING" -> "⏳ नया भाव प्रस्ताव (प्रतीक्षारत)"
            "COUNTERED_BY_FARMER" -> "🔄 किसान ने काउंटर-ऑफर भेजा"
            "COUNTERED_BY_BUYER" -> "🔄 खरीदार ने काउंटर-ऑफर भेजा"
            "ACCEPTED" -> "✅ सौदा पक्का (ऑर्डर लॉक हो गया)"
            "REJECTED" -> "❌ अस्वीकार किया गया"
            "EXPIRED" -> "⌛ समय समाप्त (48 घंटे पूर्ण)"
            else -> status
        }

        card.addView(
            TextView(ctx).apply {
                text = statusBadgeHi
                setTextColor(
                    when (status) {
                        "ACCEPTED" -> Color.parseColor("#1B5E20")
                        "REJECTED", "EXPIRED" -> Color.parseColor("#B71C1C")
                        else -> Color.parseColor("#BF360C")
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

        card.addView(
            TextView(ctx).apply {
                text = "मूल भाव: ₹$origRupees/$unit  →  प्रस्तावित भाव: ₹$activeRupees/$unit\n" +
                    "👨‍🌾 किसान: $farmerName  |  🛒 खरीदार: $buyerName"
                setTextColor(Color.parseColor("#333333"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, 0, 0, dp(6))
            }
        )

        if (message.isNotBlank() && message != "null") {
            card.addView(
                TextView(ctx).apply {
                    text = "💬 संदेश: \"$message\""
                    setTextColor(Color.parseColor("#555555"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setPadding(0, 0, 0, dp(8))
                }
            )
        }

        if (status == "ACCEPTED" && lockedOrderId.isNotBlank() && lockedOrderId != "null") {
            card.addView(
                Button(ctx).apply {
                    text = "📦 लॉक किया गया ऑर्डर देखें (#$lockedOrderId)"
                    isAllCaps = false
                    setTextColor(Color.WHITE)
                    background = roundedBg("#1B5E20", "#1B5E20", dp(8))
                    setOnClickListener {
                        (activity as? MainActivity)?.openOrdersScreen()
                    }
                }
            )
        } else if (isOpenForAction) {
            val actionRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            if (canIAccept) {
                val btnAccept = createActionBtn("✅ स्वीकार करें", "#1B5E20") {
                    submitOfferResponse(offerId, "ACCEPT", null)
                }
                actionRow.addView(btnAccept)
            }

            val btnCounter = createActionBtn("🔄 नया काउंटर भाव दें", "#0D47A1") {
                showCounterOfferDialog(offerId, prodName, activeRupees, unit)
            }
            val btnReject = createActionBtn("❌ अस्वीकार", "#B71C1C") {
                submitOfferResponse(offerId, "REJECT", null)
            }

            actionRow.addView(btnCounter)
            actionRow.addView(btnReject)
            card.addView(actionRow)
        }

        return card
    }

    private fun showCounterOfferDialog(
        offerId: String,
        prodName: String,
        currentOfferRupees: Double,
        unit: String
    ) {
        val ctx = context ?: return
        val input = EditText(ctx).apply {
            hint = "आपका नया काउंटर भाव (₹ प्रति $unit)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
        }

        AlertDialog.Builder(ctx)
            .setTitle("🔄 काउंटर-ऑफर दें: $prodName")
            .setMessage("वर्तमान प्रस्ताव ₹$currentOfferRupees/$unit है। अपना नया भाव लिखें:")
            .setView(input)
            .setPositiveButton("काउंटर-ऑफर भेजें") { _, _ ->
                val counterVal = input.text?.toString()?.toDoubleOrNull() ?: 0.0
                if (counterVal <= 0.0) {
                    Toast.makeText(ctx, "कृपया मान्य काउंटर भाव दर्ज करें।", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                submitOfferResponse(offerId, "COUNTER", counterVal)
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun submitOfferResponse(offerId: String, action: String, counterPriceRupees: Double?) {
        val ctx = context ?: return
        progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.respondToOffer(ctx, offerId, action, counterPriceRupees)
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            if (res.isSuccess) {
                val msg = when (action) {
                    "ACCEPT" -> "🎉 प्रस्ताव स्वीकार हो गया! स्टॉक लॉक करके पक्का ऑर्डर बना दिया गया है।"
                    "COUNTER" -> "🔄 आपका काउंटर-ऑफर भेज दिया गया है।"
                    else -> "प्रस्ताव अस्वीकार कर दिया गया।"
                }
                Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                loadOffersFromServer()
            } else {
                Toast.makeText(ctx, res.errorMessage ?: "कार्रवाई पूरी करने में त्रुटि।", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createActionBtn(label: String, bgHex: String, onClick: () -> Unit): Button =
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
