// app/src/main/java/in/merakisan/app/ui/auth/LoginFragment.kt
package `in`.merakisan.app.ui.auth

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import `in`.merakisan.app.core.config.FeatureManager
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.security.SessionManager
import `in`.merakisan.app.ui.main.MainActivity
import kotlinx.coroutines.launch

/**
 * MERA KISAN — ONE-TIME ONBOARDING & ROLE SELECTION SCREEN
 *
 * Rules Enforced:
 * 1. Single Server Connection: Uses only ApiClient.
 * 2. Zero Plaintext PIN: Uses Firebase Auth + Server Token verification (merged.txt Sec 4-5).
 * 3. Zero Dummy Defaults: No hardcoded 'Mandsaur'/'Garoth' defaults; states & districts load live
 *    from government source via ApiClient, with clean manual entry support.
 * 4. Separate Tables Sync: Saves to `users`, `farmers`, and `buyers` collections via `/auth/onboarding`.
 * 5. No False Success: Navigates to Home ONLY after verified HTTP 200/201 from backend (merged.txt Sec 37).
 */
class LoginFragment : Fragment() {

    private var selectedRoleOption: String = "" // "farmer", "buyer", or "both"

    private lateinit var authStatusBanner: TextView
    private lateinit var btnGoogleSignIn: Button
    private lateinit var roleFarmerBtn: Button
    private lateinit var roleBuyerBtn: Button
    private lateinit var roleBothBtn: Button

    private lateinit var inputFullName: EditText
    private lateinit var inputMobileNumber: EditText
    private lateinit var spinnerState: Spinner
    private lateinit var spinnerDistrict: Spinner
    private lateinit var inputManualState: EditText
    private lateinit var inputManualDistrict: EditText
    private lateinit var inputVillage: EditText
    private lateinit var inputTehsil: EditText
    private lateinit var inputPincode: EditText

    private lateinit var farmerExtraContainer: LinearLayout
    private lateinit var spinnerFarmingMethod: Spinner
    private lateinit var inputFarmStory: EditText

    private lateinit var progressBar: ProgressBar
    private lateinit var btnCompleteOnboarding: Button
    private lateinit var btnDealerPortalLogin: Button

    private val liveStatesList = mutableListOf<String>()
    private val liveDistrictsList = mutableListOf<String>()

    private val farmingMethods = listOf(
        "पारंपरिक / देशी खेती (Traditional / Desi)",
        "प्राकृतिक खेती (Natural Farming)",
        "जैविक विधि (Organic Practice - Farmer Declared)",
        "सामान्य मानक खेती (Standard Farming)"
    )

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (!idToken.isNullOrBlank()) {
                    authenticateWithFirebaseGoogle(idToken)
                } else {
                    showMessage("Google आईडी टोकन प्राप्त नहीं हुआ। कृपया पुनः प्रयास करें।", true)
                }
            } catch (e: ApiException) {
                showMessage("Google साइन-इन रद्द या विफल रहा (Code: ${e.statusCode})।", true)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val scrollView = ScrollView(ctx).apply {
            isFillViewport = true
            setBackgroundColor(Color.parseColor("#F2F6F2"))
        }

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(32))
        }

        // 1. Header Banner
        val headerCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#1B5E20", dp(16))
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        headerCard.addView(
            TextView(ctx).apply {
                text = "🌾 मेरा किसान (MERA KISAN)"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )
        headerCard.addView(
            TextView(ctx).apply {
                text = "किसान से सीधे आपकी जरूरत तक • वन-टाइम खाता सेटअप"
                setTextColor(Color.parseColor("#E8F5E9"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(0, dp(4), 0, 0)
            }
        )
        root.addView(headerCard)

        // 2. Step 1: Google / Firebase Authentication Card
        val authCard = createSectionCard(
            title = "1. सुरक्षित खाता प्रमाणीकरण (Login)",
            subtitle = "कोई पिन या पासवर्ड याद रखने की आवश्यकता नहीं — अपने Google खाते से सुरक्षित जुड़ें:"
        )

        authStatusBanner = TextView(ctx).apply {
            text = "स्थिति: कृपया नीचे बटन दबाकर लॉगिन करें"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(10))
        }

        btnGoogleSignIn = Button(ctx).apply {
            text = "🔐 Google खाते से सुरक्षित लॉगिन करें"
            background = roundedBg("#0D47A1", dp(10))
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            minHeight = dp(48)
            setOnClickListener { startGoogleSignInFlow() }
        }

        authCard.addView(authStatusBanner)
        authCard.addView(btnGoogleSignIn)
        root.addView(authCard)

        // 3. Step 2: First-Run Role Selection ([ किसान-विक्रेता ] [ खरीदार ] [ दोनों ])
        val roleCard = createSectionCard(
            title = "2. अपनी भूमिका चुनें (First-Run Role Selection)",
            subtitle = "आप बाद में भी प्रोफ़ाइल या ऊपर के बार से किसान और खरीदार मोड बदल सकते हैं:"
        )

        roleFarmerBtn = createRoleOptionButton("👨‍🌾 किसान / विक्रेता (फसल व उत्पाद बेचें)") {
            selectRoleOption("farmer")
        }
        roleBuyerBtn = createRoleOptionButton("🛒 खरीदार (सीधे किसान से ताज़ा खरीदें)") {
            selectRoleOption("buyer")
        }
        roleBothBtn = createRoleOptionButton("🤝 दोनों (किसान विक्रेता + खरीदार दोनों)") {
            selectRoleOption("both")
        }

        roleCard.addView(roleFarmerBtn)
        roleCard.addView(spaceView(dp(8)))
        roleCard.addView(roleBuyerBtn)
        roleCard.addView(spaceView(dp(8)))
        roleCard.addView(roleBothBtn)
        root.addView(roleCard)

        // 4. Step 3: Self-Declared Mobile Number & Location Details (Zero Default Data)
        val detailsCard = createSectionCard(
            title = "3. आपका परिचय व सेल्फ-डिक्लेयर्ड मोबाइल नंबर",
            subtitle = "यह जानकारी केवल एक बार भरनी है और आपके चयन के अनुसार किसानों/खरीदारों की अलग टेबल में सुरक्षित रहेगी:"
        )

        inputFullName = createLabeledInput(
            parent = detailsCard,
            label = "पूरा नाम (Full Name) *",
            hint = "अपना पूरा नाम लिखें",
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS,
            maxLength = 80
        )

        inputMobileNumber = createLabeledInput(
            parent = detailsCard,
            label = "10-अंकों का मोबाइल नंबर (Self-Declared Mobile Number) *",
            hint = "उदा. 98XXXXXXXX (बिना +91 के 10 अंक)",
            inputType = InputType.TYPE_CLASS_PHONE,
            maxLength = 10
        )

        // Live State Dropdown from Government Server
        detailsCard.addView(createFieldLabel("राज्य चुनें (सरकारी सर्वर से लाइव) *"))
        spinnerState = Spinner(ctx).apply {
            minimumHeight = dp(46)
        }
        detailsCard.addView(spinnerState)

        inputManualState = createLabeledInput(
            parent = detailsCard,
            label = "या राज्य का नाम लिखें (यदि सूची लोड हो रही हो)",
            hint = "राज्य का नाम",
            inputType = InputType.TYPE_CLASS_TEXT,
            maxLength = 60
        )

        // Live District Dropdown from Government Server
        detailsCard.addView(createFieldLabel("ज़िला चुनें (राज्य के अनुसार लाइव) *"))
        spinnerDistrict = Spinner(ctx).apply {
            minimumHeight = dp(46)
            isEnabled = false
        }
        detailsCard.addView(spinnerDistrict)

        inputManualDistrict = createLabeledInput(
            parent = detailsCard,
            label = "या ज़िले का नाम लिखें *",
            hint = "अपने ज़िले का नाम लिखें",
            inputType = InputType.TYPE_CLASS_TEXT,
            maxLength = 60
        )

        inputVillage = createLabeledInput(
            parent = detailsCard,
            label = "गाँव / वार्ड / कॉलोनी का नाम",
            hint = "अपने गाँव या मोहल्ले का नाम लिखें",
            inputType = InputType.TYPE_CLASS_TEXT,
            maxLength = 80
        )

        inputTehsil = createLabeledInput(
            parent = detailsCard,
            label = "तहसील / ब्लॉक (वैकल्पिक)",
            hint = "तहसील का नाम लिखें",
            inputType = InputType.TYPE_CLASS_TEXT,
            maxLength = 80
        )

        inputPincode = createLabeledInput(
            parent = detailsCard,
            label = "पिनकोड (6 अंक - वैकल्पिक)",
            hint = "6 अंकों का पिनकोड",
            inputType = InputType.TYPE_CLASS_NUMBER,
            maxLength = 6
        )

        // Farmer-specific extra fields (Farming Method & Farm Story)
        farmerExtraContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, dp(8), 0, 0)
        }

        farmerExtraContainer.addView(createFieldLabel("खेती का तरीका (Farming Method)"))
        spinnerFarmingMethod = Spinner(ctx).apply {
            minimumHeight = dp(46)
            adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, farmingMethods)
        }
        farmerExtraContainer.addView(spinnerFarmingMethod)

        inputFarmStory = createLabeledInput(
            parent = farmerExtraContainer,
            label = "मेरी खेती की कहानी (Farm Story - वैकल्पिक)",
            hint = "अपनी खेती और खास फसलों के बारे में 1-2 वाक्य लिखें...",
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            maxLength = 400
        )

        detailsCard.addView(farmerExtraContainer)
        root.addView(detailsCard)

        // 5. Submit Button & Progress
        progressBar = ProgressBar(ctx).apply {
            visibility = View.GONE
        }
        root.addView(progressBar)

        btnCompleteOnboarding = Button(ctx).apply {
            text = "✅ खाता सुरक्षित करें और आगे बढ़ें"
            background = roundedBg("#1B5E20", dp(12))
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            minHeight = dp(52)
            setOnClickListener { submitOneTimeOnboarding() }
        }
        root.addView(btnCompleteOnboarding)

        // 6. Authorized Agri-Input Dealer Login Button (Feature Flag Controlled)
        btnDealerPortalLogin = Button(ctx).apply {
            text = "🧪 अधिकृत कृषि दवाई विक्रेता (Dealer) लॉगिन"
            background = roundedBg("#3E2723", dp(10))
            setTextColor(Color.WHITE)
            minHeight = dp(46)
            visibility = if (FeatureManager.isEnabled("agri_dealer_portal", ctx)) View.VISIBLE else View.GONE
            setOnClickListener { showDealerLoginDialog() }
        }
        root.addView(spaceView(dp(12)))
        root.addView(btnDealerPortalLogin)

        scrollView.addView(root)
        return scrollView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refreshFirebaseUserDisplay()
        loadLiveStatesFromGovServer()
    }

    private fun refreshFirebaseUserDisplay() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val emailOrUid = currentUser.email ?: currentUser.phoneNumber ?: currentUser.uid.take(8)
            authStatusBanner.text = "🟢 प्रमाणित खाता: $emailOrUid"
            btnGoogleSignIn.text = "🔄 दूसरा Google खाता चुनें"
            if (inputFullName.text.isNullOrBlank() && !currentUser.displayName.isNullOrBlank()) {
                inputFullName.setText(currentUser.displayName)
            }
        } else {
            authStatusBanner.text = "🔴 अभी लॉगिन नहीं है — कृपया पहले Google खाते से लॉगिन करें"
            btnGoogleSignIn.text = "🔐 Google खाते से सुरक्षित लॉगिन करें"
        }
    }

    private fun startGoogleSignInFlow() {
        val ctx = context ?: return
        try {
            val webClientIdRes = resources.getIdentifier("default_web_client_id", "string", ctx.packageName)
            val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()

            if (webClientIdRes != 0) {
                val webClientId = getString(webClientIdRes)
                if (webClientId.isNotBlank()) {
                    gsoBuilder.requestIdToken(webClientId)
                }
            }

            val googleSignInClient = GoogleSignIn.getClient(ctx, gsoBuilder.build())
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        } catch (e: Exception) {
            showMessage("Google साइन-इन शुरू करने में समस्या: ${e.message}", true)
        }
    }

    private fun authenticateWithFirebaseGoogle(idToken: String) {
        val ctx = context ?: return
        progressBar.visibility = View.VISIBLE
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (!isAdded) return@addOnCompleteListener
                progressBar.visibility = View.GONE

                if (task.isSuccessful) {
                    refreshFirebaseUserDisplay()
                    // जाँचें कि क्या यह यूज़र पहले से सर्वर पर ऑनबोर्डेड है
                    viewLifecycleOwner.lifecycleScope.launch {
                        val meRes = ApiClient.getMyProfile(ctx)
                        if (meRes.isSuccess && meRes.dataObject != null) {
                            val d = meRes.dataObject
                            if (d.optBoolean("onboarding_completed", false)) {
                                val rolesArr = d.optJSONArray("roles")
                                val roles = mutableListOf<String>()
                                if (rolesArr != null) {
                                    for (i in 0 until rolesArr.length()) {
                                        roles.add(rolesArr.optString(i))
                                    }
                                }
                                SessionManager.saveOnboardedUserSession(
                                    context = ctx,
                                    uid = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                                    name = d.optString("name", ""),
                                    phone = d.optString("phone", ""),
                                    roles = roles.ifEmpty { listOf("farmer") },
                                    activeMode = d.optString("active_role", "farmer"),
                                    village = d.optString("village", ""),
                                    district = d.optString("district", ""),
                                    state = d.optString("state", ""),
                                    verificationStatus = d.optString("verification_status", "farmer_declared")
                                )
                                showMessage("आपका खाता पहले से पंजीकृत है! स्वागत है।", false)
                                (activity as? MainActivity)?.onUserAuthenticatedAndOnboarded()
                            }
                        }
                    }
                } else {
                    showMessage("Firebase प्रमाणीकरण विफल: ${task.exception?.localizedMessage}", true)
                }
            }
    }

    private fun selectRoleOption(role: String) {
        selectedRoleOption = role
        val activeBg = "#1B5E20"
        val inactiveBg = "#FFFFFF"

        listOf(
            roleFarmerBtn to "farmer",
            roleBuyerBtn to "buyer",
            roleBothBtn to "both"
        ).forEach { (btn, key) ->
            if (key == role) {
                btn.background = roundedBg(activeBg, dp(10))
                btn.setTextColor(Color.WHITE)
            } else {
                btn.background = roundedBgWithBorder(inactiveBg, "#1B5E20", dp(10))
                btn.setTextColor(Color.parseColor("#1B5E20"))
            }
        }

        farmerExtraContainer.visibility = if (role == "farmer" || role == "both") {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    /**
     * सरकारी मंडी सोर्स से लाइव राज्यों की सूची मंगाता है (0% डिफ़ॉल्ट राज्य नाम)
     */
    private fun loadLiveStatesFromGovServer() {
        val ctx = context ?: return
        spinnerState.adapter = ArrayAdapter(
            ctx,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("⏳ सरकारी सर्वर से राज्य लोड हो रहे हैं...")
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.getMandiStates(ctx, forceRefresh = false)
            if (!isAdded) return@launch

            liveStatesList.clear()
            val statesArr = res.dataObject?.optJSONArray("states")
            if (res.isSuccess && statesArr != null && statesArr.length() > 0) {
                liveStatesList.add("-- अपना राज्य चुनें --")
                for (i in 0 until statesArr.length()) {
                    val stName = statesArr.optJSONObject(i)?.optString("state", "")?.trim() ?: ""
                    if (stName.isNotEmpty()) liveStatesList.add(stName)
                }
                spinnerState.adapter = ArrayAdapter(
                    ctx,
                    android.R.layout.simple_spinner_dropdown_item,
                    liveStatesList
                )

                spinnerState.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position > 0 && position < liveStatesList.size) {
                            val chosenState = liveStatesList[position]
                            inputManualState.setText(chosenState)
                            loadLiveDistrictsForState(chosenState)
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            } else {
                spinnerState.adapter = ArrayAdapter(
                    ctx,
                    android.R.layout.simple_spinner_dropdown_item,
                    listOf("नीचे बॉक्स में अपना राज्य लिखें")
                )
            }
        }
    }

    private fun loadLiveDistrictsForState(stateName: String) {
        val ctx = context ?: return
        spinnerDistrict.isEnabled = false
        spinnerDistrict.adapter = ArrayAdapter(
            ctx,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("⏳ $stateName के ज़िले लोड हो रहे हैं...")
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.getMandiDistricts(ctx, stateName, forceRefresh = false)
            if (!isAdded) return@launch

            liveDistrictsList.clear()
            val distArr = res.dataObject?.optJSONArray("districts")
            if (res.isSuccess && distArr != null && distArr.length() > 0) {
                liveDistrictsList.add("-- अपना ज़िला चुनें --")
                for (i in 0 until distArr.length()) {
                    val dName = distArr.optJSONObject(i)?.optString("district", "")?.trim() ?: ""
                    if (dName.isNotEmpty()) liveDistrictsList.add(dName)
                }
                spinnerDistrict.adapter = ArrayAdapter(
                    ctx,
                    android.R.layout.simple_spinner_dropdown_item,
                    liveDistrictsList
                )
                spinnerDistrict.isEnabled = true

                spinnerDistrict.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position > 0 && position < liveDistrictsList.size) {
                            inputManualDistrict.setText(liveDistrictsList[position])
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            } else {
                spinnerDistrict.adapter = ArrayAdapter(
                    ctx,
                    android.R.layout.simple_spinner_dropdown_item,
                    listOf("नीचे बॉक्स में अपना ज़िला लिखें")
                )
            }
        }
    }

    private fun submitOneTimeOnboarding() {
        val ctx = context ?: return
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            showMessage("कृपया पहले ऊपर 'Google खाते से सुरक्षित लॉगिन करें' बटन दबाएँ।", true)
            return
        }

        if (selectedRoleOption.isBlank()) {
            showMessage("कृपया अपनी भूमिका चुनें: किसान, खरीदार, या दोनों।", true)
            return
        }

        val fullName = inputFullName.text?.toString()?.trim() ?: ""
        if (fullName.length < 2) {
            inputFullName.error = "कृपया अपना पूरा नाम लिखें"
            showMessage("कृपया अपना पूरा नाम दर्ज करें।", true)
            return
        }

        val rawPhone = inputMobileNumber.text?.toString()?.replace("\\D".toRegex(), "") ?: ""
        if (!rawPhone.matches(Regex("^[6-9]\\d{9}$"))) {
            inputMobileNumber.error = "10 अंकों का सही मोबाइल नंबर लिखें"
            showMessage("कृपया 6, 7, 8 या 9 से शुरू होने वाला 10-अंकों का सही मोबाइल नंबर दर्ज करें।", true)
            return
        }

        val stateName = inputManualState.text?.toString()?.trim() ?: ""
        val districtName = inputManualDistrict.text?.toString()?.trim() ?: ""
        if (districtName.length < 2) {
            inputManualDistrict.error = "कृपया अपना ज़िला चुनें या लिखें"
            showMessage("कृपया अपना ज़िला चुनें या दर्ज करें।", true)
            return
        }

        val villageName = inputVillage.text?.toString()?.trim()
        val tehsilName = inputTehsil.text?.toString()?.trim()
        val pincodeVal = inputPincode.text?.toString()?.trim()
        val farmingMethod = if (selectedRoleOption in listOf("farmer", "both")) {
            spinnerFarmingMethod.selectedItem?.toString()
        } else {
            null
        }
        val farmStory = if (selectedRoleOption in listOf("farmer", "both")) {
            inputFarmStory.text?.toString()?.trim()
        } else {
            null
        }

        progressBar.visibility = View.VISIBLE
        btnCompleteOnboarding.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val result = ApiClient.completeOneTimeOnboarding(
                context = ctx,
                name = fullName,
                phone = rawPhone,
                roleSelection = selectedRoleOption,
                village = villageName,
                district = districtName,
                state = stateName.ifBlank { null },
                farmingMethod = farmingMethod
            )

            if (!isAdded) return@launch

            // यदि किसान की कहानी या अतिरिक्त तहसील/पिनकोड भी भरा गया है तो प्रोफाइल सिंक करें
            if (result.isSuccess && (!tehsilName.isNullOrBlank() || !pincodeVal.isNullOrBlank() || !farmStory.isNullOrBlank())) {
                val extraObj = org.json.JSONObject().apply {
                    if (!tehsilName.isNullOrBlank()) put("tehsil", tehsilName)
                    if (!pincodeVal.isNullOrBlank()) put("pincode", pincodeVal)
                    if (!farmStory.isNullOrBlank()) put("farm_story", farmStory)
                }
                ApiClient.updateProfile(ctx, extraObj)
            }

            progressBar.visibility = View.GONE
            btnCompleteOnboarding.isEnabled = true

            // नियम: केवल वास्तविक सर्वर पुष्टि (isSuccess == true) के बाद ही सफलता दिखाएँ (No False Success)
            if (result.isSuccess && result.dataObject != null) {
                val data = result.dataObject
                val rolesArr = data.optJSONArray("roles")
                val savedRoles = mutableListOf<String>()
                if (rolesArr != null) {
                    for (i in 0 until rolesArr.length()) {
                        savedRoles.add(rolesArr.optString(i))
                    }
                }
                if (savedRoles.isEmpty()) {
                    if (selectedRoleOption == "both") savedRoles.addAll(listOf("farmer", "buyer"))
                    else savedRoles.add(selectedRoleOption)
                }

                val activeRole = data.optString(
                    "active_role",
                    if (savedRoles.contains("farmer")) "farmer" else "buyer"
                )

                SessionManager.saveOnboardedUserSession(
                    context = ctx,
                    uid = firebaseUser.uid,
                    name = data.optString("name", fullName),
                    phone = data.optString("phone", rawPhone),
                    roles = savedRoles,
                    activeMode = activeRole,
                    village = villageName,
                    district = districtName,
                    state = stateName,
                    verificationStatus = data.optString("verification_status", "farmer_declared")
                )

                showMessage("आपका खाता सफलतापूर्वक सुरक्षित हो गया!", false)
                (activity as? MainActivity)?.onUserAuthenticatedAndOnboarded()
            } else {
                showMessage(
                    result.errorMessage ?: "सर्वर पर खाता सहेजने में समस्या आई। कृपया पुनः प्रयास करें।",
                    true
                )
            }
        }
    }

    private fun showDealerLoginDialog() {
        val ctx = context ?: return
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(8))
        }

        val loginIdInput = EditText(ctx).apply {
            hint = "डीलर लॉगिन आईडी (Admin द्वारा प्रदत्त)"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val passwordInput = EditText(ctx).apply {
            hint = "डीलर पासवर्ड"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(loginIdInput)
        layout.addView(spaceView(dp(8)))
        layout.addView(passwordInput)

        AlertDialog.Builder(ctx)
            .setTitle("🧪 अधिकृत कृषि दवाई विक्रेता लॉगिन")
            .setMessage("मेरा किसान एडमिन द्वारा जारी अपनी दुकान की लॉगिन आईडी और पासवर्ड दर्ज करें:")
            .setView(layout)
            .setPositiveButton("लॉगिन करें") { _, _ ->
                val lid = loginIdInput.text?.toString()?.trim() ?: ""
                val pwd = passwordInput.text?.toString() ?: ""
                if (lid.isBlank() || pwd.isBlank()) {
                    showMessage("कृपया डीलर आईडी और पासवर्ड दोनों दर्ज करें।", true)
                    return@setPositiveButton
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val res = ApiClient.dealerLogin(ctx, lid, pwd)
                    if (res.isSuccess && res.dataObject != null) {
                        val token = res.dataObject.optString("dealer_token", "")
                        val dealerObj = res.dataObject.optJSONObject("dealer")
                        val shopName = dealerObj?.optString("shop_name", "अधिकृत कृषि केंद्र") ?: "अधिकृत कृषि केंद्र"
                        SessionManager.saveDealerSession(ctx, token, lid, shopName)
                        showMessage("स्वागत है, $shopName! अब आप कृषि दवाइयां व बीज प्रबंधित कर सकते हैं।", false)
                    } else {
                        showMessage(res.errorMessage ?: "डीलर लॉगिन विफल रहा।", true)
                    }
                }
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun createSectionCard(title: String, subtitle: String): LinearLayout {
        val ctx = requireContext()
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#FFFFFF", dp(14))
            setPadding(dp(16), dp(16), dp(16), dp(16))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(14)
            layoutParams = lp

            addView(
                TextView(ctx).apply {
                    text = title
                    setTextColor(Color.parseColor("#124116"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                    setTypeface(typeface, Typeface.BOLD)
                }
            )
            addView(
                TextView(ctx).apply {
                    text = subtitle
                    setTextColor(Color.parseColor("#4A5A4A"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setPadding(0, dp(4), 0, dp(12))
                }
            )
        }
    }

    private fun createRoleOptionButton(label: String, onClick: () -> Unit): Button {
        val ctx = requireContext()
        return Button(ctx).apply {
            text = label
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            background = roundedBgWithBorder("#FFFFFF", "#1B5E20", dp(10))
            setTextColor(Color.parseColor("#1B5E20"))
            minHeight = dp(48)
            setOnClickListener { onClick() }
        }
    }

    private fun createFieldLabel(label: String): TextView {
        val ctx = requireContext()
        return TextView(ctx).apply {
            text = label
            setTextColor(Color.parseColor("#1A241A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(4))
        }
    }

    private fun createLabeledInput(
        parent: LinearLayout,
        label: String,
        hint: String,
        inputType: Int,
        maxLength: Int
    ): EditText {
        val ctx = requireContext()
        parent.addView(createFieldLabel(label))
        val edit = EditText(ctx).apply {
            this.hint = hint
            this.inputType = inputType
            filters = arrayOf(InputFilter.LengthFilter(maxLength))
            background = roundedBgWithBorder("#FAFAFA", "#B8CBB8", dp(8))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            minHeight = dp(46)
        }
        parent.addView(edit)
        return edit
    }

    private fun roundedBg(hexColor: String, radiusPx: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx.toFloat()
            setColor(Color.parseColor(hexColor))
        }

    private fun roundedBgWithBorder(fillHex: String, strokeHex: String, radiusPx: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx.toFloat()
            setColor(Color.parseColor(fillHex))
            setStroke(dp(2), Color.parseColor(strokeHex))
        }

    private fun spaceView(heightPx: Int): View =
        View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                heightPx
            )
        }

    private fun showMessage(msg: String, isError: Boolean) {
        val ctx = context ?: return
        Toast.makeText(ctx, msg, if (isError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
}
