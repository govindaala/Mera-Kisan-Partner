package `in`.merakisan.app.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import `in`.merakisan.app.R
import `in`.merakisan.app.core.security.GoogleAuthManager
import `in`.merakisan.app.core.security.SessionManager

class ProfileFragment : Fragment() {

    private var tvUserName: TextView? = null
    private var tvUserContact: TextView? = null
    private var tvUserLocation: TextView? = null
    private var rgRoleMode: RadioGroup? = null
    private var rbFarmer: RadioButton? = null
    private var rbBuyer: RadioButton? = null
    private var rbBoth: RadioButton? = null
    private var btnOrderHistory: View? = null
    private var btnManageCrops: View? = null
    private var btnBuyerRequests: View? = null
    private var btnLogout: View? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        initViews(view)
        loadUserData()
        setupListeners()

        return view
    }

    private fun initViews(v: View) {
        tvUserName = findViewSafe(v, "tv_profile_name", "tvProfileName", "tvName", "userNameTextView")
        tvUserContact = findViewSafe(v, "tv_profile_contact", "tvProfileContact", "tvContact", "tvEmail", "userContactTextView")
        tvUserLocation = findViewSafe(v, "tv_profile_location", "tvProfileLocation", "tvLocation", "locationTextView")
        rgRoleMode = findViewSafe(v, "rg_role_mode", "rgRoleMode", "radioGroupRole")
        rbFarmer = findViewSafe(v, "rb_role_farmer", "rbRoleFarmer", "rbFarmer")
        rbBuyer = findViewSafe(v, "rb_role_buyer", "rbRoleBuyer", "rbBuyer")
        rbBoth = findViewSafe(v, "rb_role_both", "rbRoleBoth", "rbBoth")
        btnOrderHistory = findViewSafe(v, "btn_order_history", "btnOrderHistory", "orderHistoryCard")
        btnManageCrops = findViewSafe(v, "btn_manage_crops", "btnManageCrops", "manageCropsCard")
        btnBuyerRequests = findViewSafe(v, "btn_buyer_requests", "btnBuyerRequests", "buyerRequestsCard")
        btnLogout = findViewSafe(v, "btn_logout", "btnLogout", "logoutButton")
    }

    private fun loadUserData() {
        val context = context ?: return
        val prefs = context.getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)

        // 1. वास्तविक Google नाम प्राप्त करने का व्यवस्थित क्रम
        var resolvedName: String? = null

        try {
            val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
            if (googleAccount != null && !googleAccount.displayName.isNullOrBlank()) {
                resolvedName = googleAccount.displayName?.trim()
            }
        } catch (_: Throwable) {}

        if (resolvedName.isNullOrBlank()) {
            val savedName = prefs.getString("user_name", "")?.trim()
                ?: prefs.getString("displayName", "")?.trim()
                ?: prefs.getString("name", "")?.trim()

            // यदि नाम केवल अंकों वाली आईडी या टोकन हो तो उसे किसान साथी पर रीसेट करना
            if (!savedName.isNullOrBlank() && !savedName.matches(Regex("^[0-9_a-fA-F]{12,}$"))) {
                resolvedName = savedName
            }
        }

        if (resolvedName.isNullOrBlank()) {
            resolvedName = "किसान साथी"
        }

        // 2. संपर्क विवरण (ईमेल / फोन)
        var contact = prefs.getString("user_phone", "")?.trim() ?: ""
        if (contact.isBlank()) {
            contact = prefs.getString("user_email", "")?.trim() ?: ""
        }
        if (contact.isBlank()) {
            try {
                val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
                contact = googleAccount?.email?.trim() ?: ""
            } catch (_: Throwable) {}
        }
        if (contact.isBlank()) {
            contact = "Google खाता सत्यापित"
        }

        // 3. स्थान विवरण (बिना किसी फालतू कॉमा के स्वच्छ रेंडरिंग)
        val village = prefs.getString("user_village", "")?.trim() ?: ""
        val district = prefs.getString("user_district", "")?.trim() ?: ""

        val locationText = when {
            village.isNotBlank() && district.isNotBlank() -> "$village,$district"
            district.isNotBlank() -> district
            village.isNotBlank() -> village
            else -> "मध्य प्रदेश"
        }

        // 4. सक्रिय भूमिका (SessionManager.getUserRole सुरक्षित रूप से उपयोग किया गया)
        val role = try {
            SessionManager.getUserRole(context)
        } catch (_: Throwable) {
            prefs.getString("user_role", "BOTH") ?: "BOTH"
        }

        tvUserName?.text = resolvedName
        tvUserContact?.text = contact
        tvUserLocation?.text = locationText

        when (role.uppercase()) {
            "FARMER" -> rbFarmer?.isChecked = true
            "BUYER" -> rbBuyer?.isChecked = true
            else -> rbBoth?.isChecked = true
        }
    }

    private fun setupListeners() {
        val context = context ?: return

        // सक्रिय भूमिका बदलने पर सभी आवश्यक SharedPreferences में सुरक्षित रूप से लिखना
        rgRoleMode?.setOnCheckedChangeListener { _, checkedId ->
            val selectedRole = when (checkedId) {
                rbFarmer?.id -> "FARMER"
                rbBuyer?.id -> "BUYER"
                else -> "BOTH"
            }

            // SessionManager को बिना छुए सभी स्थानीय कॉन्फ़िगरेशन में रोल अपडेट करना
            val userPrefs = context.getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
            userPrefs.edit().putString("user_role", selectedRole).putString("role", selectedRole).apply()

            val sessionPrefs = context.getSharedPreferences("mera_kisan_session", Context.MODE_PRIVATE)
            sessionPrefs.edit().putString("user_role", selectedRole).apply()

            val defaultPrefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
            defaultPrefs.edit().putString("user_role", selectedRole).apply()

            Toast.makeText(context, "सक्रिय भूमिका बदली गई", Toast.LENGTH_SHORT).show()
        }

        // त्वरित नेविगेशन (Quick Links)
        btnOrderHistory?.setOnClickListener {
            navigateSafe("ordersFragment")
        }

        btnManageCrops?.setOnClickListener {
            navigateSafe("marketplaceFragment", "myCropsFragment")
        }

        btnBuyerRequests?.setOnClickListener {
            navigateSafe("buyerRequestsFragment")
        }

        // पूर्ण लॉगआउट
        btnLogout?.setOnClickListener {
            val authManager = GoogleAuthManager(context)
            authManager.signOut {
                // सभी स्थानीय सत्र साफ़ करना
                val userPrefs = context.getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
                userPrefs.edit().clear().apply()

                val sessionPrefs = context.getSharedPreferences("mera_kisan_session", Context.MODE_PRIVATE)
                sessionPrefs.edit().clear().apply()

                val defaultPrefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
                defaultPrefs.edit().clear().apply()

                Toast.makeText(context, "लॉगआउट सफल", Toast.LENGTH_SHORT).show()
                navigateSafe("loginFragment")
            }
        }
    }

    private fun navigateSafe(vararg destinationNames: String) {
        val ctx = context ?: return
        val packageName = ctx.packageName
        for (name in destinationNames) {
            val destId = ctx.resources.getIdentifier(name, "id", packageName)
            if (destId != 0) {
                try {
                    findNavController().navigate(destId)
                    return
                } catch (_: Throwable) {}
            }
        }
    }

    private fun <T : View> findViewSafe(root: View, vararg idNames: String): T? {
        val ctx = root.context
        val packageName = ctx.packageName
        for (name in idNames) {
            val id = ctx.resources.getIdentifier(name, "id", packageName)
            if (id != 0) {
                val found = root.findViewById<T>(id)
                if (found != null) return found
            }
        }
        return null
    }
}
