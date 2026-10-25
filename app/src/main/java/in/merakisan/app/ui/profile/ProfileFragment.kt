// app/src/main/java/in/merakisan/app/ui/profile/ProfileFragment.kt
package `in`.merakisan.app.ui.profile

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
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
import com.google.firebase.auth.FirebaseAuth
import `in`.merakisan.app.core.config.FeatureManager
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.security.SessionManager
import `in`.merakisan.app.ui.main.MainActivity
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * MERA KISAN — UNIFIED PROFILE, 6-LEVEL VERIFICATION, DEALER PORTAL & SETTINGS HUB
 * Exact Repository Path: app/src/main/java/in/merakisan/app/ui/profile/ProfileFragment.kt
 *
 * Implements (Master Spec Sec 5, 10, 20 & merged.txt Sec 7, 35, 77):
 * - Farmer & Buyer profile view/edit synced with `users`, `farmers`, and `buyers` tables.
 * - 1-Tap Role Switcher (Farmer Mode <-> Buyer Mode).
 * - 6-Level Farmer Verification application (`farmer_declared`, `document_verified`, `organic_certified`, `lab_tested`).
 * - Authorized Agri-Input Dealer Management (add Fungicide/Pesticide/Herbicide/Micronutrient/Fertilizer/Seeds by Company).
 * - Feature-flag controlled links to Wallet, Subscription, Oil Mill, Farm Box, Batch QR, and Fair Price Calculator.
 */
class ProfileFragment : Fragment() {

    private lateinit var tvNameAndRole: TextView
    private lateinit var tvVerificationBadge: TextView
    private lateinit var inputName: EditText
    private lateinit var inputPhone: EditText
    private lateinit var inputVillage: EditText
    private lateinit var inputDistrict: EditText
    private lateinit var inputState: EditText
    private lateinit var inputFarmStory: EditText
    private lateinit var progressBar: ProgressBar

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
            setPadding(dp(14), dp(14), dp(14), dp(32))
        }

        // 1. Identity & Active Mode Card
        val idCard = createCard()
        tvNameAndRole = TextView(ctx).apply {
            text = "${SessionManager.getUserName(ctx).ifBlank { "मेरा किसान उपयोगकर्ता" }} (${if (isFarmer) "👨‍🌾 किसान मोड" else "🛒 खरीदार मोड"})"
            setTextColor(Color.parseColor("#124116"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
            setTypeface(typeface, Typeface.BOLD)
        }
        idCard.addView(tvNameAndRole)

        tvVerificationBadge = TextView(ctx).apply {
            text = "सत्यापन स्तर: ${SessionManager.getVerificationStatus(ctx)}"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(10))
        }
        idCard.addView(tvVerificationBadge)

        idCard.addView(
            createButton(
                if (isFarmer) "🔄 खरीदार मोड (Buyer Mode) में स्विच करें" else "🔄 किसान विक्रेता मोड (Farmer Mode) में स्विच करें",
                "#BF360C"
            ) {
                (activity as? MainActivity)?.toggleFarmerBuyerMode()
            }
        )
        root.addView(idCard)

        // 2. Quick Hub Navigation (merged.txt Sec 77)
        val navCard = createCard()
        navCard.addView(createSectionHeading("📌 त्वरित नेविगेशन और सुविधाएँ"))

        val row1 = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        row1.addView(
            createHalfButton("📦 मेरे ऑर्डर्स", "#1B5E20") {
                (activity as? MainActivity)?.openOrdersScreen()
            }
        )
        row1.addView(
            createHalfButton("🤝 मेरे भाव प्रस्ताव (Offers)", "#0D47A1") {
                (activity as? MainActivity)?.openNegotiationOffersScreen()
            }
        )
        navCard.addView(row1)

        val row2 = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, 0)
        }
        row2.addView(
            createHalfButton("🌾 मेरी फसलें", "#2E7D32") {
                (activity as? MainActivity)?.openMyCropsScreen()
            }
        )
        row2.addView(
            createHalfButton("📢 मुझे यह चाहिए बोर्ड", "#4E342E") {
                (activity as? MainActivity)?.openBuyerRequestsScreen()
            }
        )
        navCard.addView(row2)

        val row3 = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, 0)
        }
        row3.addView(
            createHalfButton("🎨 AI .JPG पोस्टर स्टूडियो", "#1B5E20") {
                (activity as? MainActivity)?.openAiPosterStudioScreen(null)
            }
        )
        row3.addView(
            createHalfButton("🧮 उचित दाम कैलकुलेटर", "#0D47A1") {
                (activity as? MainActivity)?.openFairPriceCalculatorScreen()
            }
        )
        navCard.addView(row3)

        val row4 = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, 0)
        }
        row4.addView(
            createHalfButton("🔍 बैच QR ट्रेसबिलिटी", "#37474F") {
                (activity as? MainActivity)?.openBatchQrTraceabilityScreen(null)
            }
        )
        row4.addView(
            createHalfButton("🫙 कच्ची घानी तेल मिल", "#5D4037") {
                (activity as? MainActivity)?.openOilMillMarketplaceScreen()
            }
        )
        navCard.addView(row4)

        if (FeatureManager.isEnabled("farm_box", ctx) || FeatureManager.isEnabled("group_buying", ctx)) {
            navCard.addView(
                createButton("🧺 ग्राम ताज़ा फार्म बॉक्स व सामूहिक खरीदारी", "#2E7D32") {
                    (activity as? MainActivity)?.openFarmBoxGroupBuyScreen()
                }
            )
        }

        // Future Blank Settings Modules (Visible automatically when Admin sets ON)
        val walletSettings = FeatureManager.getWalletSettings(ctx)
        if (walletSettings.enabled) {
            navCard.addView(
                createButton("💳 मेरा किसान वॉलेट (दैनिक सीमा: ₹${walletSettings.dailyLimitRupees})", "#00695C") {
                    Toast.makeText(ctx, "मेरा किसान वॉलेट सक्रिय है।", Toast.LENGTH_SHORT).show()
                }
            )
        }

        val subSettings = FeatureManager.getSubscriptionSettings(ctx)
        if (subSettings.enabled) {
            navCard.addView(
                createButton("⭐ ${subSettings.planName} (₹${subSettings.proPriceRupees}/माह)", "#6A1B9A") {
                    Toast.makeText(ctx, "${subSettings.planName} सदस्यता विवरण।", Toast.LENGTH_SHORT).show()
                }
            )
        }

        root.addView(navCard)

        // 3. Edit Profile & Farm Story Card
        val editCard = createCard()
        editCard.addView(createSectionHeading("✏️ मेरी प्रोफ़ाइल व खेती की जानकारी"))

        inputName = addInput(editCard, "पूरा नाम", SessionManager.getUserName(ctx))
        inputPhone = addInput(editCard, "सेल्फ-डिक्लेयर्ड 10-अंकों का मोबाइल नंबर", SessionManager.getSelfDeclaredPhone(ctx), isPhone = true)
        inputVillage = addInput(editCard, "गाँव / मोहल्ला", SessionManager.getUserVillage(ctx))
        inputDistrict = addInput(editCard, "ज़िला (District)", SessionManager.getUserDistrict(ctx))
        inputState = addInput(editCard, "राज्य (State)", SessionManager.getUserState(ctx))
        inputFarmStory = addInput(editCard, "मेरी खेती की कहानी (Farm Story)", "")

        progressBar = ProgressBar(ctx).apply {
            visibility = View.GONE
            setPadding(0, dp(8), 0, dp(8))
        }
        editCard.addView(progressBar)

        editCard.addView(
            createButton("💾 प्रोफ़ाइल बदलाव सर्वर पर सेव करें", "#1B5E20") {
                saveProfileChanges()
            }
        )
        root.addView(editCard)

        // 4. 6-Level Farmer Verification Request Card (Master Spec Sec 10)
        if (FeatureManager.isEnabled("farmer_verification", ctx)) {
            val verifCard = createCard()
            verifCard.addView(createSectionHeading("✔ 6-स्तरीय किसान सत्यापन बैज आवेदन"))
            verifCard.addView(
                TextView(ctx).apply {
                    text = "प्रमाणित बैज (Document Verified / Organic Certified / Lab Tested) के लिए अपना प्रमाण-पत्र लिंक भेजें:"
                    setTextColor(Color.parseColor("#4A5A4A"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setPadding(0, 0, 0, dp(8))
                }
            )
            verifCard.addView(
                createButton("📜 सत्यापन बैज के लिए आवेदन करें", "#0D47A1") {
                    showVerificationRequestDialog()
                }
            )
            root.addView(verifCard)
        }

        // 5. Authorized Agri-Input Dealer Management Card
        if (FeatureManager.isEnabled("agri_dealer_portal", ctx)) {
            val dealerCard = createCard()
            dealerCard.addView(createSectionHeading("🧪 अधिकृत कृषि दवाई व बीज विक्रेता पैनल"))
            val dealerToken = SessionManager.getDealerToken(ctx)
            val shopName = SessionManager.getDealerShopName(ctx)

            if (!dealerToken.isNullOrBlank()) {
                dealerCard.addView(
                    TextView(ctx).apply {
                        text = "🟢 लॉगिन दुकान: $shopName"
                        setTextColor(Color.parseColor("#1B5E20"))
                        setTypeface(typeface, Typeface.BOLD)
                        setPadding(0, 0, 0, dp(8))
                    }
                )
                dealerCard.addView(
                    createButton("➕ नई कृषि दवाई / खाद / बीज (कंपनी अनुसार) जोड़ें", "#1B5E20") {
                        showDealerAddAgriInputDialog()
                    }
                )
            } else {
                dealerCard.addView(
                    createButton("🔐 एडमिन द्वारा प्रदत्त डीलर ID/Password से लॉगिन करें", "#3E2723") {
                        showDealerLoginDialog()
                    }
                )
            }
            root.addView(dealerCard)
        }

        // 6. Support & Logout Card
        val footerCard = createCard()
        val supportWa = FeatureManager.getSupportWhatsApp()
        if (supportWa.isNotBlank()) {
            footerCard.addView(
                createButton("🟢 मेरा किसान सहायता WhatsApp ($supportWa)", "#2E7D32") {
                    val clean = supportWa.replace("\\D".toRegex(), "")
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$clean")))
                }
            )
        }

        footerCard.addView(
            createButton("🚪 सुरक्षित लॉगआउट करें (Logout)", "#B71C1C") {
                performSecureLogout()
            }
        )
        root.addView(footerCard)

        scroll.addView(root)
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refreshServerProfile()
    }

    private fun refreshServerProfile() {
        val ctx = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.getMyProfile(ctx)
            if (!isAdded || !res.isSuccess || res.dataObject == null) return@launch
            val d = res.dataObject
            val verif = d.optString("verification_status", SessionManager.getVerificationStatus(ctx))
            tvVerificationBadge.text = "सत्यापन स्तर: $verif"
            val farmerProf = d.optJSONObject("farmer_profile")
            val story = farmerProf?.optString("farm_story", "")?.takeIf { it != "null" } ?: ""
            if (story.isNotBlank() && inputFarmStory.text.isNullOrBlank()) {
                inputFarmStory.setText(story)
            }
        }
    }

    private fun saveProfileChanges() {
        val ctx = context ?: return
        val name = inputName.text?.toString()?.trim() ?: ""
        val phone = inputPhone.text?.toString()?.replace("\\D".toRegex(), "") ?: ""
        val village = inputVillage.text?.toString()?.trim() ?: ""
        val district = inputDistrict.text?.toString()?.trim() ?: ""
        val state = inputState.text?.toString()?.trim() ?: ""
        val story = inputFarmStory.text?.toString()?.trim() ?: ""

        if (name.length < 2 || !phone.matches(Regex("^[6-9]\\d{9}$"))) {
            Toast.makeText(ctx, "कृपया मान्य नाम और 10-अंकों का मोबाइल नंबर दर्ज करें।", Toast.LENGTH_LONG).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val body = JSONObject().apply {
                put("name", name)
                put("phone", phone)
                put("village", village)
                put("district", district)
                put("state", state)
                put("farm_story", story)
            }
            val res = ApiClient.updateProfile(ctx, body)
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            if (res.isSuccess) {
                SessionManager.saveOnboardedUserSession(
                    context = ctx,
                    uid = SessionManager.getUserUid(ctx) ?: "",
                    name = name,
                    phone = phone,
                    roles = SessionManager.getAllowedRoles(ctx).ifEmpty { listOf("farmer", "buyer") },
                    activeMode = SessionManager.getActiveMode(ctx),
                    village = village,
                    district = district,
                    state = state,
                    verificationStatus = SessionManager.getVerificationStatus(ctx)
                )
                Toast.makeText(ctx, "✅ प्रोफ़ाइल सफलतापूर्वक अपडेट हो गई!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(ctx, res.errorMessage ?: "प्रोफ़ाइल अपडेट विफल रहा।", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showVerificationRequestDialog() {
        val ctx = context ?: return
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }

        val types = listOf(
            "farmer_declared" to "👨‍🌾 किसान स्व-घोषित (Farmer Declared)",
            "document_verified" to "📄 दस्तावेज़ सत्यापित (Document Verified)",
            "organic_certified" to "🌿 प्रमाणित जैविक (Organic Certified)",
            "lab_tested" to "🧪 लैब परीक्षित (Lab Tested)"
        )

        val typeSpinner = Spinner(ctx).apply {
            adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, types.map { it.second })
        }
        val docUrlInput = EditText(ctx).apply {
            hint = "प्रमाण-पत्र / रिपोर्ट का Google Drive HTTPS लिंक"
        }
        val notesInput = EditText(ctx).apply {
            hint = "खेती विधि या प्रमाण का विवरण"
        }

        layout.addView(typeSpinner)
        layout.addView(docUrlInput)
        layout.addView(notesInput)

        AlertDialog.Builder(ctx)
            .setTitle("✔ किसान सत्यापन बैज आवेदन")
            .setView(layout)
            .setPositiveButton("आवेदन भेजें") { _, _ ->
                val chosenCode = types.getOrNull(typeSpinner.selectedItemPosition)?.first ?: "farmer_declared"
                val docUrl = docUrlInput.text?.toString()?.trim() ?: ""
                val notes = notesInput.text?.toString()?.trim() ?: ""

                viewLifecycleOwner.lifecycleScope.launch {
                    val body = JSONObject().apply {
                        put("verification_type", chosenCode)
                        if (docUrl.isNotBlank()) put("document_url", docUrl)
                        put("notes", notes)
                    }
                    val res = ApiClient.request(ctx, "/trust/verification/request", method = "POST", bodyJson = body)
                    if (res.isSuccess) {
                        Toast.makeText(ctx, "✅ आपका सत्यापन आवेदन दर्ज हो गया!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(ctx, res.errorMessage ?: "आवेदन भेजने में त्रुटि।", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun showDealerLoginDialog() {
        val ctx = context ?: return
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }
        val idInput = EditText(ctx).apply { hint = "डीलर लॉगिन आईडी" }
        val pwdInput = EditText(ctx).apply {
            hint = "डीलर पासवर्ड"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        layout.addView(idInput)
        layout.addView(pwdInput)

        AlertDialog.Builder(ctx)
            .setTitle("🔐 अधिकृत कृषि दवाई विक्रेता लॉगिन")
            .setView(layout)
            .setPositiveButton("लॉगिन करें") { _, _ ->
                val lid = idInput.text?.toString()?.trim() ?: ""
                val pwd = pwdInput.text?.toString() ?: ""
                viewLifecycleOwner.lifecycleScope.launch {
                    val res = ApiClient.dealerLogin(ctx, lid, pwd)
                    if (res.isSuccess && res.dataObject != null) {
                        val token = res.dataObject.optString("dealer_token", "")
                        val shop = res.dataObject.optJSONObject("dealer")?.optString("shop_name", "कृषि केंद्र") ?: "कृषि केंद्र"
                        SessionManager.saveDealerSession(ctx, token, lid, shop)
                        Toast.makeText(ctx, "स्वागत है, $shop!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(ctx, res.errorMessage ?: "डीलर लॉगिन विफल रहा।", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun showDealerAddAgriInputDialog() {
        val ctx = context ?: return
        val scroll = ScrollView(ctx)
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }

        val cats = listOf("FUNGICIDE", "PESTICIDE", "HERBICIDE", "MICRONUTRIENT", "FERTILIZER", "BIO_INPUT", "SEEDS")
        val catSpinner = Spinner(ctx).apply {
            adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, cats)
        }
        val nameInput = EditText(ctx).apply { hint = "दवाई / बीज का नाम *" }
        val companyInput = EditText(ctx).apply { hint = "निर्माता कंपनी का नाम (Company Name) *" }
        val packInput = EditText(ctx).apply { hint = "पैक साइज़ (उदा. 1 Litre / 500g)" }
        val priceInput = EditText(ctx).apply {
            hint = "विक्रय मूल्य (₹) *"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val stockInput = EditText(ctx).apply {
            hint = "उपलब्ध पैकेट संख्या *"
            setText("20")
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        layout.addView(catSpinner)
        layout.addView(nameInput)
        layout.addView(companyInput)
        layout.addView(packInput)
        layout.addView(priceInput)
        layout.addView(stockInput)
        scroll.addView(layout)

        AlertDialog.Builder(ctx)
            .setTitle("🧪 नई कृषि दवाई / बीज जोड़ें")
            .setView(scroll)
            .setPositiveButton("जोड़ें") { _, _ ->
                val priceVal = priceInput.text?.toString()?.toDoubleOrNull() ?: 0.0
                val payload = JSONObject().apply {
                    put("category", cats[catSpinner.selectedItemPosition])
                    put("name", nameInput.text?.toString()?.trim() ?: "")
                    put("company_name", companyInput.text?.toString()?.trim() ?: "")
                    put("pack_size", packInput.text?.toString()?.trim()?.ifBlank { "1 Pack" } ?: "1 Pack")
                    put("price_paise", Math.round(priceVal * 100))
                    put("stock_quantity", stockInput.text?.toString()?.toIntOrNull() ?: 10)
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val res = ApiClient.dealerAddProduct(ctx, payload)
                    if (res.isSuccess) {
                        Toast.makeText(ctx, "✅ कृषि उत्पाद सफलतापूर्वक जुड़ गया!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(ctx, res.errorMessage ?: "जोड़ने में त्रुटि।", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun performSecureLogout() {
        val ctx = context ?: return
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {}
        SessionManager.clearAllSession(ctx)
        Toast.makeText(ctx, "आप सुरक्षित रूप से लॉगआउट हो गए हैं।", Toast.LENGTH_SHORT).show()
        (activity as? MainActivity)?.openLoginAndOnboardingScreen()
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
            lp.topMargin = dp(12)
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

    private fun addInput(parent: LinearLayout, label: String, initialValue: String, isPhone: Boolean = false): EditText {
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
            setText(initialValue)
            inputType = if (isPhone) InputType.TYPE_CLASS_PHONE else InputType.TYPE_CLASS_TEXT
            background = roundedBg("#FAFAFA", "#B8CBB8", dp(8))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            minHeight = dp(44)
        }
        parent.addView(edit)
        return edit
    }

    private fun createButton(label: String, bgHex: String, onClick: () -> Unit): Button =
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

    private fun createHalfButton(label: String, bgHex: String, onClick: () -> Unit): Button =
        Button(requireContext()).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            background = roundedBg(bgHex, bgHex, dp(8))
            minHeight = dp(42)
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
