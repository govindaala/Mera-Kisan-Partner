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
import `in`.merakisan.app.R
import `in`.merakisan.app.core.security.GoogleAuthManager

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
        tvUserName = findViewSafe(v, "tv_profile_name", "tvProfileName", "tvName")
        tvUserContact = findViewSafe(v, "tv_profile_contact", "tvProfileContact", "tvContact", "tvEmail")
        tvUserLocation = findViewSafe(v, "tv_profile_location", "tvProfileLocation", "tvLocation")
        rgRoleMode = findViewSafe(v, "rg_role_mode", "rgRoleMode", "radioGroupRole")
        rbFarmer = findViewSafe(v, "rb_role_farmer", "rbRoleFarmer", "rbFarmer")
        rbBuyer = findViewSafe(v, "rb_role_buyer", "rbRoleBuyer", "rbBuyer")
        rbBoth = findViewSafe(v, "rb_role_both", "rbRoleBoth", "rbBoth")
        btnOrderHistory = findViewSafe(v, "btn_order_history", "btnOrderHistory")
        btnManageCrops = findViewSafe(v, "btn_manage_crops", "btnManageCrops")
        btnBuyerRequests = findViewSafe(v, "btn_buyer_requests", "btnBuyerRequests")
        btnLogout = findViewSafe(v, "btn_logout", "btnLogout")
    }

    private fun loadUserData() {
        val prefs = requireContext().getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
        val name = prefs.getString("user_name", "किसान साथी") ?: "किसान साथी"
        val email = prefs.getString("user_email", "") ?: ""
        val phone = prefs.getString("user_phone", "") ?: ""
        val district = prefs.getString("user_district", "मंदसौर (मध्य प्रदेश)") ?: "मंदसौर (मध्य प्रदेश)"
        val role = prefs.getString("user_role", "BOTH") ?: "BOTH"

        tvUserName?.text = name
        tvUserContact?.text = if (phone.isNotEmpty()) phone else email
        tvUserLocation?.text = district

        when (role.uppercase()) {
            "FARMER" -> rbFarmer?.isChecked = true
            "BUYER" -> rbBuyer?.isChecked = true
            else -> rbBoth?.isChecked = true
        }
    }

    private fun setupListeners() {
        val prefs = requireContext().getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)

        rgRoleMode?.setOnCheckedChangeListener { _, checkedId ->
            val selectedRole = when (checkedId) {
                rbFarmer?.id -> "FARMER"
                rbBuyer?.id -> "BUYER"
                else -> "BOTH"
            }
            prefs.edit().putString("user_role", selectedRole).apply()
            Toast.makeText(requireContext(), "सक्रिय भूमिका बदली गई", Toast.LENGTH_SHORT).show()
        }

        btnOrderHistory?.setOnClickListener {
            navigateSafe("ordersFragment")
        }

        btnManageCrops?.setOnClickListener {
            navigateSafe("marketplaceFragment", "myCropsFragment")
        }

        btnBuyerRequests?.setOnClickListener {
            navigateSafe("buyerRequestsFragment")
        }

        btnLogout?.setOnClickListener {
            val authManager = GoogleAuthManager(requireContext())
            authManager.signOut {
                prefs.edit().clear().apply()
                Toast.makeText(requireContext(), "लॉगआउट सफल", Toast.LENGTH_SHORT).show()
                navigateSafe("loginFragment")
            }
        }
    }

    private fun navigateSafe(vararg destinationNames: String) {
        val context = requireContext()
        val packageName = context.packageName
        for (name in destinationNames) {
            val destId = context.resources.getIdentifier(name, "id", packageName)
            if (destId != 0) {
                try {
                    findNavController().navigate(destId)
                    return
                } catch (_: Throwable) {}
            }
        }
    }

    private fun <T : View> findViewSafe(root: View, vararg idNames: String): T? {
        val context = root.context
        val packageName = context.packageName
        for (name in idNames) {
            val id = context.resources.getIdentifier(name, "id", packageName)
            if (id != 0) {
                val found = root.findViewById<T>(id)
                if (found != null) return found
            }
        }
        return null
    }
}
