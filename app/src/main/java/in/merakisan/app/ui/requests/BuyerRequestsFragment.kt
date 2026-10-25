// app/src/main/java/in/merakisan/app/ui/requests/BuyerRequestsFragment.kt
package `in`.merakisan.app.ui.requests

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
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
import org.json.JSONArray
import org.json.JSONObject

/**
 * MERA KISAN — BUYER DEMAND BOARD ("मुझे यह चाहिए" / Reverse Marketplace)
 * Exact Repository Path: app/src/main/java/in/merakisan/app/ui/requests/BuyerRequestsFragment.kt
 *
 * Implements (Master Spec Sec 7 & merged.txt Sec 14, 15, 38, 61, 77):
 * - Buyers post what they need (Product, Variety, Quantity, Unit, Target Budget, District, Radius, Notes).
 * - Farmers browse active buyer requests and submit offers ("मेरे पास उपलब्ध है — भाव भेजें").
 * - Buyers view all farmer responses (`GET /requests/{id}/matches`) and contact the farmer via WhatsApp/Call.
 */
class BuyerRequestsFragment : Fragment() {

    private lateinit var tvSubtitle: TextView
    private lateinit var btnCreateNewRequest: Button
    private lateinit var btnFilterAllDistricts: Button
    private lateinit var btnFilterMyDistrict: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var requestsListContainer: LinearLayout

    private var filterByMyDistrictOnly: Boolean = false
    private val unitList = listOf("kg", "quintal", "litre", "packet", "piece")

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

        headerCard.addView(
            TextView(ctx).apply {
                text = if (isFarmer) {
                    "📢 ग्राहकों की मांग बोर्ड (“मुझे यह चाहिए”)"
                } else {
                    "📢 मुझे यह चाहिए — अपनी मांग पोस्ट करें"
                }
                setTextColor(Color.parseColor("#124116"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )

        tvSubtitle = TextView(ctx).apply {
            text = if (isFarmer) {
                "ग्राहकों ने सीधे नीचे दिए गए उत्पाद माँगे हैं। यदि आपके पास उपलब्ध है, तो अपना भाव प्रस्ताव भेजें:"
            } else {
                "यदि आपका मनचाहा देशी उत्पाद सूची में नहीं दिख रहा है, तो यहाँ अपनी मांग लिखें — किसान स्वयं आपसे संपर्क करेंगे:"
            }
            setTextColor(Color.parseColor("#4A5A4A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(4), 0, dp(10))
        }
        headerCard.addView(tvSubtitle)

        btnCreateNewRequest = Button(ctx).apply {
            text = "➕ नई मांग ('मुझे यह चाहिए') पोस्ट करें"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            background = roundedBg("#BF360C", "#BF360C", dp(10))
            minHeight = dp(46)
            setOnClickListener { showCreateBuyerRequestDialog() }
        }
        headerCard.addView(btnCreateNewRequest)

        val filterRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }

        btnFilterAllDistricts = Button(ctx).apply {
            text = "🌍 सभी ज़िलों की मांगें"
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
            minHeight = dp(38)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(6)
            }
            setOnClickListener {
                filterByMyDistrictOnly = false
                updateFilterButtonStyles()
                loadBuyerRequests()
            }
        }

        val myDist = SessionManager.getUserDistrict(ctx).ifBlank { "मेरा ज़िला" }
        btnFilterMyDistrict = Button(ctx).apply {
            text = "📍 केवल $myDist"
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
            minHeight = dp(38)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(6)
            }
            setOnClickListener {
                filterByMyDistrictOnly = true
                updateFilterButtonStyles()
                loadBuyerRequests()
            }
        }

        filterRow.addView(btnFilterAllDistricts)
        filterRow.addView(btnFilterMyDistrict)
        headerCard.addView(filterRow)

        root.addView(headerCard)

        progressBar = ProgressBar(ctx).apply {
            visibility = View.GONE
            setPadding(0, dp(12), 0, dp(12))
        }
        root.addView(progressBar)

        requestsListContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        root.addView(requestsListContainer)

        scroll.addView(root)
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateFilterButtonStyles()
        loadBuyerRequests()
    }

    private fun updateFilterButtonStyles() {
        if (!filterByMyDistrictOnly) {
            btnFilterAllDistricts.background = roundedBg("#1B5E20", "#1B5E20", dp(8))
            btnFilterAllDistricts.setTextColor(Color.WHITE)
            btnFilterMyDistrict.background = roundedBg("#E8F5E9", "#81C784", dp(8))
            btnFilterMyDistrict.setTextColor(Color.parseColor("#1B5E20"))
        } else {
            btnFilterMyDistrict.background = roundedBg("#1B5E20", "#1B5E20", dp(8))
            btnFilterMyDistrict.setTextColor(Color.WHITE)
            btnFilterAllDistricts.background = roundedBg("#E8F5E9", "#81C784", dp(8))
            btnFilterAllDistricts.setTextColor(Color.parseColor("#1B5E20"))
        }
    }

    private fun loadBuyerRequests() {
        val ctx = context ?: return
        progressBar.visibility = View.VISIBLE
        requestsListContainer.removeAllViews()

        val distParam = if (filterByMyDistrictOnly) {
            SessionManager.getUserDistrict(ctx).takeIf { it.isNotBlank() }
        } else {
            null
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.getBuyerRequests(ctx, district = distParam)
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val arr = res.dataArray ?: JSONArray()
            if (arr.length() == 0) {
                requestsListContainer.addView(
                    TextView(ctx).apply {
                        text = "अभी इस श्रेणी में कोई खुली ग्राहक मांग नहीं है। ऊपर बटन दबाकर पहली मांग पोस्ट करें!"
                        setTextColor(Color.parseColor("#555555"))
                        setPadding(dp(8), dp(18), dp(8), dp(18))
                    }
                )
                return@launch
            }

            val myUid = SessionManager.getUserUid(ctx) ?: ""
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                requestsListContainer.addView(buildRequestCard(item, myUid))
            }
        }
    }

    private fun buildRequestCard(reqObj: JSONObject, myUid: String): View {
        val ctx = requireContext()
        val reqId = reqObj.optString("id", "")
        val buyerUid = reqObj.optString("buyer_uid", "")
        val buyerName = reqObj.optString("buyer_name", "ग्राहक")
        val prodName = reqObj.optString("product_name", "कृषि उत्पाद")
        val variety = reqObj.optString("variety", "")
        val qty = reqObj.optDouble("quantity", 1.0)
        val unit = reqObj.optString("unit", "kg")
        val targetRupees = reqObj.optDouble("target_price_rupees", 0.0)
        val district = reqObj.optString("district", "")
        val village = reqObj.optString("village", "")
        val radiusKm = reqObj.optInt("radius_km", FeatureManager.getDefaultSearchRadiusKm())
        val notes = reqObj.optString("notes", "")
        val responsesCount = reqObj.optInt("responses_count", 0)
        val createdAt = reqObj.optString("created_at", "").take(10)

        val isMyOwnRequest = myUid.isNotBlank() && myUid == buyerUid

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
                text = "📌 मांग: $prodName ${if (variety.isNotBlank() && variety != "null") "($variety)" else ""}"
                setTextColor(Color.parseColor("#124116"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )

        card.addView(
            TextView(ctx).apply {
                text = "📦 आवश्यक मात्रा: $qty $unit  |  💰 अपेक्षित बजट: ₹$targetRupees / $unit"
                setTextColor(Color.parseColor("#BF360C"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(4), 0, dp(4))
            }
        )

        card.addView(
            TextView(ctx).apply {
                text = "👤 ग्राहक: $buyerName • 📍 ${if (village.isNotBlank() && village != "null") "$village, " else ""}$district (${radiusKm} km दायरा) • 📅 $createdAt"
                setTextColor(Color.parseColor("#4A5A4A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            }
        )

        if (notes.isNotBlank() && notes != "null") {
            card.addView(
                TextView(ctx).apply {
                    text = "📝 विवरण: $notes"
                    setTextColor(Color.parseColor("#333333"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setPadding(0, dp(4), 0, dp(4))
                }
            )
        }

        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }

        if (!isMyOwnRequest) {
            val btnOffer = Button(ctx).apply {
                text = "✋ मेरे पास उपलब्ध है — भाव भेजें"
                isAllCaps = false
                setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                background = roundedBg("#1B5E20", "#1B5E20", dp(8))
                minHeight = dp(42)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = dp(6)
                }
                setOnClickListener {
                    showFarmerRespondDialog(reqId, prodName, qty, unit)
                }
            }
            btnRow.addView(btnOffer)
        }

        val btnViewMatches = Button(ctx).apply {
            text = "📬 किसानों के प्रस्ताव देखें ($responsesCount)"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            background = roundedBg("#0D47A1", "#0D47A1", dp(8))
            minHeight = dp(42)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                loadAndShowFarmerMatchesDialog(reqId, prodName, unit)
            }
        }
        btnRow.addView(btnViewMatches)

        card.addView(btnRow)
        return card
    }

    private fun showCreateBuyerRequestDialog() {
        val ctx = context ?: return
        val scroll = ScrollView(ctx)
        val form = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(10))
        }

        val prodInput = EditText(ctx).apply {
            hint = "उत्पाद का नाम (उदा. देशी गेहूं, मूंगफली तेल) *"
            filters = arrayOf(InputFilter.LengthFilter(100))
        }
        val varietyInput = EditText(ctx).apply {
            hint = "किस्म (उदा. शरबती, बिना पॉलिश दाल - वैकल्पिक)"
            filters = arrayOf(InputFilter.LengthFilter(60))
        }
        val qtyInput = EditText(ctx).apply {
            hint = "आवश्यक मात्रा (उदा. 20) *"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val unitSpinner = Spinner(ctx).apply {
            adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, unitList)
        }
        val priceInput = EditText(ctx).apply {
            hint = "आपका अधिकतम बजट भाव (₹ प्रति इकाई) *"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val distInput = EditText(ctx).apply {
            hint = "आपका ज़िला / शहर *"
            setText(SessionManager.getUserDistrict(ctx))
            filters = arrayOf(InputFilter.LengthFilter(80))
        }
        val notesInput = EditText(ctx).apply {
            hint = "अतिरिक्त जानकारी (कब तक चाहिए, गुणवत्ता आदि)"
            filters = arrayOf(InputFilter.LengthFilter(250))
        }

        form.addView(prodInput)
        form.addView(varietyInput)
        form.addView(qtyInput)
        form.addView(unitSpinner)
        form.addView(priceInput)
        form.addView(distInput)
        form.addView(notesInput)
        scroll.addView(form)

        AlertDialog.Builder(ctx)
            .setTitle("📢 मुझे यह चाहिए — नई मांग पोस्ट करें")
            .setView(scroll)
            .setPositiveButton("मांग प्रकाशित करें") { _, _ ->
                val pName = prodInput.text?.toString()?.trim() ?: ""
                val qtyVal = qtyInput.text?.toString()?.toDoubleOrNull() ?: 0.0
                val budgetVal = priceInput.text?.toString()?.toDoubleOrNull() ?: 0.0
                val distVal = distInput.text?.toString()?.trim() ?: ""
                val unitVal = unitList.getOrNull(unitSpinner.selectedItemPosition) ?: "kg"

                if (pName.length < 2 || qtyVal <= 0.0 || budgetVal <= 0.0 || distVal.length < 2) {
                    Toast.makeText(ctx, "कृपया उत्पाद नाम, मात्रा, बजट और ज़िला सही भरें।", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    val payload = JSONObject().apply {
                        put("product_name", pName)
                        put("variety", varietyInput.text?.toString()?.trim() ?: "")
                        put("quantity", qtyVal)
                        put("unit", unitVal)
                        put("target_price_paise", Math.round(budgetVal * 100))
                        put("district", distVal)
                        put("village", SessionManager.getUserVillage(ctx))
                        put("radius_km", FeatureManager.getDefaultSearchRadiusKm())
                        put("notes", notesInput.text?.toString()?.trim() ?: "")
                    }
                    val res = ApiClient.createBuyerRequest(ctx, payload)
                    if (res.isSuccess) {
                        Toast.makeText(ctx, "✅ आपकी मांग डिमांड बोर्ड पर प्रकाशित हो गई!", Toast.LENGTH_LONG).show()
                        loadBuyerRequests()
                    } else {
                        Toast.makeText(ctx, res.errorMessage ?: "मांग पोस्ट करने में त्रुटि।", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun showFarmerRespondDialog(
        requestId: String,
        productName: String,
        requestedQty: Double,
        unit: String
    ) {
        val ctx = context ?: return
        val form = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }

        val priceInput = EditText(ctx).apply {
            hint = "आपका प्रस्तावित भाव (₹ प्रति $unit) *"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val availQtyInput = EditText(ctx).apply {
            hint = "आपके पास उपलब्ध मात्रा ($unit में) *"
            setText(requestedQty.toString())
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val msgInput = EditText(ctx).apply {
            hint = "ग्राहक के लिए संदेश"
            setText("मेरे पास ताज़ा '$productName' उपलब्ध है।")
        }

        form.addView(priceInput)
        form.addView(availQtyInput)
        form.addView(msgInput)

        AlertDialog.Builder(ctx)
            .setTitle("✋ '$productName' के लिए अपना भाव दें")
            .setView(form)
            .setPositiveButton("भाव प्रस्ताव भेजें") { _, _ ->
                val priceVal = priceInput.text?.toString()?.toDoubleOrNull() ?: 0.0
                val availVal = availQtyInput.text?.toString()?.toDoubleOrNull() ?: requestedQty
                val msg = msgInput.text?.toString()?.trim() ?: ""

                if (priceVal <= 0.0) {
                    Toast.makeText(ctx, "कृपया मान्य भाव (₹) दर्ज करें।", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    val res = ApiClient.respondToBuyerRequest(
                        context = ctx,
                        requestId = requestId,
                        offeredPriceRupees = priceVal,
                        availableQty = availVal,
                        message = msg
                    )
                    if (res.isSuccess) {
                        Toast.makeText(ctx, "✅ आपका प्रस्ताव ग्राहक को भेज दिया गया!", Toast.LENGTH_LONG).show()
                        loadBuyerRequests()
                    } else {
                        Toast.makeText(ctx, res.errorMessage ?: "प्रस्ताव भेजने में समस्या आई।", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun loadAndShowFarmerMatchesDialog(requestId: String, productName: String, unit: String) {
        val ctx = context ?: return
        progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.request(ctx, "/requests/$requestId/matches", method = "GET")
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            val arr = res.dataArray ?: JSONArray()
            if (arr.length() == 0) {
                AlertDialog.Builder(ctx)
                    .setTitle("📬 '$productName' पर किसानों के प्रस्ताव")
                    .setMessage("अभी तक किसी किसान ने इस मांग पर प्रस्ताव नहीं भेजा है।")
                    .setPositiveButton("ठीक है", null)
                    .show()
                return@launch
            }

            val scroll = ScrollView(ctx)
            val listLayout = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(12), dp(18), dp(12))
            }

            for (i in 0 until arr.length()) {
                val m = arr.optJSONObject(i) ?: continue
                val fName = m.optString("farmer_name", "किसान")
                val fPhone = m.optString("farmer_phone", "")
                val fVillage = m.optString("farmer_village", "")
                val fDist = m.optString("farmer_district", "")
                val offRupees = m.optDouble("offered_price_rupees", 0.0)
                val availQty = m.optDouble("available_quantity", 1.0)
                val msg = m.optString("message", "")

                val box = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    background = roundedBg("#F1F8E9", "#A5D6A7", dp(10))
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    val lp = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    lp.bottomMargin = dp(8)
                    layoutParams = lp
                }

                box.addView(
                    TextView(ctx).apply {
                        text = "👨‍🌾 $fName (${if (fVillage.isNotBlank() && fVillage != "null") "$fVillage, " else ""}$fDist)"
                        setTextColor(Color.parseColor("#1B5E20"))
                        setTypeface(typeface, Typeface.BOLD)
                    }
                )
                box.addView(
                    TextView(ctx).apply {
                        text = "💰 किसान का भाव: ₹$offRupees/$unit • उपलब्ध: $availQty $unit\n💬 \"$msg\""
                        setTextColor(Color.parseColor("#333333"))
                        setPadding(0, dp(4), 0, dp(6))
                    }
                )

                if (fPhone.isNotBlank() && fPhone != "null") {
                    val contactBtn = Button(ctx).apply {
                        text = "📞 किसान से बात करें / WhatsApp ($fPhone)"
                        isAllCaps = false
                        setTextColor(Color.WHITE)
                        background = roundedBg("#2E7D32", "#2E7D32", dp(8))
                        setOnClickListener {
                            val clean = fPhone.replace("\\D".toRegex(), "")
                            val intl = if (clean.length == 10) "91$clean" else clean
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://wa.me/$intl?text=${Uri.encode("नमस्ते $fName जी, मैंने 'मेरा किसान' डिमांड बोर्ड पर '$productName' के लिए आपका प्रस्ताव (₹$offRupees/$unit) देखा।")}")
                                )
                            )
                        }
                    }
                    box.addView(contactBtn)
                }

                listLayout.addView(box)
            }

            scroll.addView(listLayout)
            AlertDialog.Builder(ctx)
                .setTitle("📬 किसानों के प्राप्त प्रस्ताव (${arr.length()})")
                .setView(scroll)
                .setPositiveButton("बंद करें", null)
                .show()
        }
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
