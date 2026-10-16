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
import `in`.merakisan.app.core.state.RoleStateManager

class ProfileFragment : Fragment() {

    private var tvUserName: TextView? = null
    private var tvUserContact: TextView? = null
    private var tvUserLocation: TextView? = null
    private var tvAvatarInitial: TextView? = null
    private var rgRoleMode: RadioGroup? = null
    private var rbFarmer: RadioButton? = null
    private var rbBuyer: RadioButton? = null
    private var rbBoth: RadioButton? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_profile, container, false)

        initViews(root)
        renderUserData(root)
        setupRoleSwitching()
        setupQuickLinks(root)
        setupLogout(root)

        return root
    }

    override fun onResume() {
        super.onResume()
        view?.let { renderUserData(it) }
    }

    private fun initViews(v: View) {
        // Direct View IDs & Safe fallback
        tvUserName = findViewDirectOrScan(v, "tv_profile_name", "tvProfileName", "userNameTextView", "tvName")
        tvUserContact = findViewDirectOrScan(v, "tv_profile_contact", "tvProfileContact", "userContactTextView", "tvContact")
        tvUserLocation = findViewDirectOrScan(v, "tv_profile_location", "tvProfileLocation", "locationTextView", "tvLocation")
        tvAvatarInitial = findViewDirectOrScan(v, "tv_avatar_initial", "avatarInitial", "tvInitial", "tv_user_initial")

        rgRoleMode = findViewDirectOrScan(v, "rg_role_mode", "rgRoleMode", "radioGroupRole")
        rbFarmer = findViewDirectOrScan(v, "rb_role_farmer", "rbRoleFarmer", "rbFarmer")
        rbBuyer = findViewDirectOrScan(v, "rb_role_buyer", "rbRoleBuyer", "rbBuyer")
        rbBoth = findViewDirectOrScan(v, "rb_role_both", "rbRoleBoth", "rbBoth")
    }

    private fun renderUserData(root: View) {
        val context = context ?: return
        val prefs = context.getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)

        // 1. Google Display Name extraction
        var resolvedName: String? = null
        try {
            val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
            if (googleAccount != null && !googleAccount.displayName.isNullOrBlank()) {
                resolvedName = googleAccount.displayName?.trim()
            }
        } catch (_: Throwable) {}

        if (resolvedName.isNullOrBlank()) {
            val saved = prefs.getString("user_name", "")?.trim()
                ?: prefs.getString("displayName", "")?.trim()
            if (!saved.isNullOrBlank() && !saved.matches(Regex("^[0-9_a-fA-F]{12,}$"))) {
                resolvedName = saved
            }
        }

        if (resolvedName.isNullOrBlank()) {
            resolvedName = "किसान साथी"
        }

        // 2. Contact (Phone ya Email)
        var contact = prefs.getString("user_phone", "")?.trim() ?: ""
        if (contact.isBlank()) {
            contact = prefs.getString("user_email", "")?.trim() ?: ""
        }
        if (contact.isBlank()) {
            try {
                contact = GoogleSignIn.getLastSignedInAccount(context)?.email?.trim() ?: ""
            } catch (_: Throwable) {}
        }
        if (contact.isBlank()) {
            contact = "Google खाता सत्यापित"
        }

        // 3. Location (Clean format, no dangling commas)
        val village = prefs.getString("user_village", "")?.trim() ?: ""
        val district = prefs.getString("user_district", "")?.trim() ?: ""
        val location = when {
            village.isNotBlank() && district.isNotBlank() -> "$village,$district"
            district.isNotBlank() -> district
            village.isNotBlank() -> village
            else -> "मध्य प्रदेश"
        }

        // View Binding
        tvUserName?.text = resolvedName
        tvUserContact?.text = contact
        tvUserLocation?.text = location

        // Avatar Letter
        val initialLetter = resolvedName.firstOrNull()?.toString()?.uppercase() ?: "K"
        tvAvatarInitial?.text = initialLetter

        // Sync RadioButton with RoleStateManager
        val currentRole = RoleStateManager.getCurrentRole()
        when (currentRole) {
            RoleStateManager.ROLE_FARMER -> rbFarmer?.isChecked = true
            RoleStateManager.ROLE_BUYER -> rbBuyer?.isChecked = true
            else -> rbBoth?.isChecked = true
        }
    }

    private fun setupRoleSwitching() {
        val context = context ?: return

        rgRoleMode?.setOnCheckedChangeListener { _, checkedId ->
            val selectedRole = when (checkedId) {
                rbFarmer?.id -> RoleStateManager.ROLE_FARMER
                rbBuyer?.id -> RoleStateManager.ROLE_BUYER
                else -> RoleStateManager.ROLE_BOTH
            }

            // RoleStateManager update karte hi poore app me event broadcast hota hai
            RoleStateManager.setRole(context, selectedRole)
            Toast.makeText(context, "सक्रिय भूमिका बदली गई: $selectedRole", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupQuickLinks(root: View) {
        val btnOrderHistory = findViewDirectOrScan<View>(root, "btn_order_history", "btnOrderHistory", "orderHistoryCard", "card_order_history")
        val btnManageCrops = findViewDirectOrScan<View>(root, "btn_manage_crops", "btnManageCrops", "manageCropsCard", "card_manage_crops")
        val btnBuyerRequests = findViewDirectOrScan<View>(root, "btn_buyer_requests", "btnBuyerRequests", "buyerRequestsCard", "card_buyer_requests")

        btnOrderHistory?.setOnClickListener {
            navigateToDestination("ordersFragment")
        }

        btnManageCrops?.setOnClickListener {
            navigateToDestination("marketplaceFragment", "myCropsFragment")
        }

        btnBuyerRequests?.setOnClickListener {
            navigateToDestination("buyerRequestsFragment")
        }
    }

    private fun setupLogout(root: View) {
        val btnLogout = findViewDirectOrScan<View>(root, "btn_logout", "btnLogout", "logoutButton", "btn_sign_out")
        btnLogout?.setOnClickListener {
            val context = context ?: return@setOnClickListener
            val authManager = GoogleAuthManager(context)
            authManager.signOut {
                val userPrefs = context.getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
                userPrefs.edit().clear().apply()

                val sessionPrefs = context.getSharedPreferences("mera_kisan_session", Context.MODE_PRIVATE)
                sessionPrefs.edit().clear().apply()

                RoleStateManager.setRole(context, RoleStateManager.ROLE_BOTH)
                Toast.makeText(context, "लॉगआउट सफल", Toast.LENGTH_SHORT).show()
                navigateToDestination("loginFragment")
            }
        }
    }

    private fun navigateToDestination(vararg destinationNames: String) {
        val ctx = context ?: return
        val pkg = ctx.packageName
        for (name in destinationNames) {
            val destId = ctx.resources.getIdentifier(name, "id", pkg)
            if (destId != 0) {
                try {
                    findNavController().navigate(destId)
                    return
                } catch (_: Throwable) {}
            }
        }
    }

    private fun <T : View> findViewDirectOrScan(root: View, vararg idNames: String): T? {
        val ctx = root.context
        val pkg = ctx.packageName
        for (name in idNames) {
            val id = ctx.resources.getIdentifier(name, "id", pkg)
            if (id != 0) {
                val found = root.findViewById<T>(id)
                if (found != null) return found
            }
        }
        return null
    }
}
