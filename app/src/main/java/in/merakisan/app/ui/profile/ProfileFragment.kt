package in.merakisan.app.ui.profile

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
import in.merakisan.app.R
import in.merakisan.app.core.security.GoogleAuthManager
import in.merakisan.app.core.security.SessionManager

class ProfileFragment : Fragment() {

    private lateinit var tvUserName: TextView
    private lateinit var tvUserContact: TextView
    private lateinit var tvUserLocation: TextView
    private lateinit var rgRoleMode: RadioGroup
    private lateinit var rbFarmer: RadioButton
    private lateinit var rbBuyer: RadioButton
    private lateinit var rbBoth: RadioButton
    private lateinit var btnOrderHistory: View
    private lateinit var btnManageCrops: View
    private lateinit var btnBuyerRequests: View
    private lateinit var btnLogout: View

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
        tvUserName = v.findViewById(R.id.tv_profile_name)
        tvUserContact = v.findViewById(R.id.tv_profile_contact)
        tvUserLocation = v.findViewById(R.id.tv_profile_location)
        rgRoleMode = v.findViewById(R.id.rg_role_mode)
        rbFarmer = v.findViewById(R.id.rb_role_farmer)
        rbBuyer = v.findViewById(R.id.rb_role_buyer)
        rbBoth = v.findViewById(R.id.rb_role_both)
        btnOrderHistory = v.findViewById(R.id.btn_order_history)
        btnManageCrops = v.findViewById(R.id.btn_manage_crops)
        btnBuyerRequests = v.findViewById(R.id.btn_buyer_requests)
        btnLogout = v.findViewById(R.id.btn_logout)
    }

    private fun loadUserData() {
        val name = try {
            SessionManager.getUserName(requireContext())
        } catch (_: Throwable) {
            val prefs = requireContext().getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
            prefs.getString("user_name", "किसान साथी")
        } ?: "किसान साथी"

        val email = try {
            SessionManager.getUserEmail(requireContext())
        } catch (_: Throwable) {
            val prefs = requireContext().getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
            prefs.getString("user_email", "")
        } ?: ""

        val phone = try {
            SessionManager.getUserPhone(requireContext())
        } catch (_: Throwable) {
            val prefs = requireContext().getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
            prefs.getString("user_phone", "")
        } ?: ""

        val district = try {
            SessionManager.getUserDistrict(requireContext())
        } catch (_: Throwable) {
            val prefs = requireContext().getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
            prefs.getString("user_district", "मंदसौर (मध्य प्रदेश)")
        } ?: "मंदसौर (मध्य प्रदेश)"

        val role = try {
            SessionManager.getUserRole(requireContext())
        } catch (_: Throwable) {
            val prefs = requireContext().getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
            prefs.getString("user_role", "BOTH")
        } ?: "BOTH"

        tvUserName.text = name
        tvUserContact.text = if (phone.isNotEmpty()) phone else email
        tvUserLocation.text = district

        when (role.uppercase()) {
            "FARMER" -> rbFarmer.isChecked = true
            "BUYER" -> rbBuyer.isChecked = true
            else -> rbBoth.isChecked = true
        }
    }

    private fun setupListeners() {
        rgRoleMode.setOnCheckedChangeListener { _, checkedId ->
            val selectedRole = when (checkedId) {
                R.id.rb_role_farmer -> "FARMER"
                R.id.rb_role_buyer -> "BUYER"
                else -> "BOTH"
            }
            try {
                SessionManager.setUserRole(requireContext(), selectedRole)
            } catch (_: Throwable) {
                val prefs = requireContext().getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
                prefs.edit().putString("user_role", selectedRole).apply()
            }
            Toast.makeText(requireContext(), "सक्रिय भूमिका बदली गई", Toast.LENGTH_SHORT).show()
        }

        btnOrderHistory.setOnClickListener {
            try {
                findNavController().navigate(R.id.ordersFragment)
            } catch (_: Throwable) {}
        }

        btnManageCrops.setOnClickListener {
            try {
                findNavController().navigate(R.id.marketplaceFragment)
            } catch (_: Throwable) {}
        }

        btnBuyerRequests.setOnClickListener {
            try {
                findNavController().navigate(R.id.buyerRequestsFragment)
            } catch (_: Throwable) {}
        }

        btnLogout.setOnClickListener {
            val authManager = GoogleAuthManager(requireContext())
            authManager.signOut {
                try {
                    SessionManager.clearSession(requireContext())
                } catch (_: Throwable) {
                    val prefs = requireContext().getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
                    prefs.edit().clear().apply()
                }
                Toast.makeText(requireContext(), "लॉगआउट सफल", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.loginFragment)
            }
        }
    }
}
