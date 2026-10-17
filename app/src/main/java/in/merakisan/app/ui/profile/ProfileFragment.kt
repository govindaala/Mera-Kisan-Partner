// app/src/main/java/in/merakisan/app/ui/profile/ProfileFragment.kt
package `in`.merakisan.app.ui.profile

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import `in`.merakisan.app.R
import `in`.merakisan.app.core.security.GoogleAuthManager
import `in`.merakisan.app.core.state.RoleStateManager
import kotlinx.coroutines.launch

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
        val root = inflater.inflate(R.layout.fragment_profile, container, false)

        bindViewsAndFixHeader(root)
        populateIdentityData(root)
        setupRoleRadioButtons()
        setupWorkingQuickLinks()
        observeRoleForQuickLinks()

        return root
    }

    private fun bindViewsAndFixHeader(root: View) {
        tvUserName = findViewSafe(root, "tv_profile_name", "tvProfileName", "tvName", "userNameTextView")
        tvUserContact = findViewSafe(root, "tv_profile_contact", "tvProfileContact", "tvContact", "tvEmail")
        tvUserLocation = findViewSafe(root, "tv_profile_location", "tvProfileLocation", "tvLocation")

        rgRoleMode = findViewSafe(root, "rg_role_mode", "rgRoleMode", "radioGroupRole")
        rbFarmer = findViewSafe(root, "rb_role_farmer", "rbRoleFarmer", "rbFarmer")
        rbBuyer = findViewSafe(root, "rb_role_buyer", "rbRoleBuyer", "rbBuyer")
        rbBoth = findViewSafe(root, "rb_role_both", "rbRoleBoth", "rbBoth")

        btnOrderHistory = findViewSafe(root, "btn_order_history", "btnOrderHistory", "orderHistoryCard")
        btnManageCrops = findViewSafe(root, "btn_manage_crops", "btnManageCrops", "manageCropsCard")
        btnBuyerRequests = findViewSafe(root, "btn_buyer_requests", "btnBuyerRequests", "buyerRequestsCard")
        btnLogout = findViewSafe(root, "btn_logout", "btnLogout", "logoutButton")

        // यदि XML में रेडियो ग्रुप के अंदर रेडियो बटन्स अलग नाम से हों तो क्रम से प्राप्त करना
        if (rgRoleMode == null) {
            rgRoleMode = findFirstViewOfType(root, RadioGroup::class.java)
        }
        rgRoleMode?.let { rg ->
            if (rbFarmer == null && rg.childCount >= 1) rbFarmer = rg.getChildAt(0) as? RadioButton
            if (rbBuyer == null && rg.childCount >= 2) rbBuyer = rg.getChildAt(1) as? RadioButton
            if (rbBoth == null && rg.childCount >= 3) rbBoth = rg.getChildAt(2) as? RadioButton
        }
    }

    private fun populateIdentityData(root: View) {
        val ctx = context ?: return
        val prefs = ctx.getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)

        var resolvedName: String? = null
        var resolvedEmail: String? = null

        try {
            val acct = GoogleSignIn.getLastSignedInAccount(ctx)
            if (acct != null) {
                resolvedName = acct.displayName?.trim()
                resolvedEmail = acct.email?.trim()
            }
        } catch (_: Throwable) {}

        if (resolvedName.isNullOrBlank()) {
            resolvedName = prefs.getString("user_name", null)?.trim()
        }
        if (resolvedName.isNullOrBlank() || resolvedName.matches(Regex("^[0-9_a-fA-F]{12,}$"))) {
            resolvedName = "गोविंद राम पाटीदार (किसान साथी)"
        }

        if (resolvedEmail.isNullOrBlank()) {
            resolvedEmail = prefs.getString("user_email", null)?.trim()
                ?: prefs.getString("user_phone", null)?.trim()
                ?: "सत्यापित Google खाता"
        }

        val village = prefs.getString("user_village", "")?.trim() ?: ""
        val district = prefs.getString("user_district", "मंदसौर (मध्य प्रदेश)")?.trim() ?: "मंदसौर (मध्य प्रदेश)"
        val cleanLocation = if (village.isNotBlank()) "📍 $village, $district" else "📍 $district"

        tvUserName?.apply {
            text = resolvedName
            setTextColor(Color.parseColor("#1B5E20"))
            visibility = View.VISIBLE
        }
        tvUserContact?.apply {
            text = resolvedEmail
            setTextColor(Color.parseColor("#424242"))
            visibility = View.VISIBLE
        }
        tvUserLocation?.apply {
            text = cleanLocation
            setTextColor(Color.parseColor("#616161"))
            visibility = View.VISIBLE
        }

        // यदि टॉप कार्ड में TextViews न मिलें तो 'K' अवतार के बगल वाले कंटेनर में सीधे नाम दिखाना
        injectIdentityNextToAvatarIfMissing(root, resolvedName, resolvedEmail, cleanLocation)
    }

    private fun injectIdentityNextToAvatarIfMissing(root: View, name: String, contact: String, location: String) {
        if (tvUserName != null && tvUserName?.text?.isNotBlank() == true) return
        val avatarView = findTextViewWithExactText(root, "K") ?: return
        val parent = avatarView.parent as? ViewGroup ?: return

        avatarView.text = name.firstOrNull()?.uppercase() ?: "K"

        // चेक करें कि पहले से इंजेक्ट तो नहीं किया गया
        if (parent.findViewWithTag<View>("injected_identity_box") != null) return

        val infoBox = LinearLayout(requireContext()).apply {
            tag = "injected_identity_box"
            orientation = LinearLayout.VERTICAL
            setPadding(28, 4, 16, 4)

            addView(TextView(context).apply {
                text = name
                textSize = 17f
                setTextColor(Color.parseColor("#1B5E20"))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = contact
                textSize = 13f
                setTextColor(Color.parseColor("#424242"))
            })
            addView(TextView(context).apply {
                text = location
                textSize = 12f
                setTextColor(Color.parseColor("#2E7D32"))
            })
        }
        parent.addView(infoBox)
    }

    private fun setupRoleRadioButtons() {
        val ctx = context ?: return
        val activeRole = RoleStateManager.getCurrentRole()

        when (activeRole) {
            RoleStateManager.ROLE_FARMER -> rbFarmer?.isChecked = true
            RoleStateManager.ROLE_BUYER -> rbBuyer?.isChecked = true
            else -> rbBoth?.isChecked = true
        }

        rgRoleMode?.setOnCheckedChangeListener { _, checkedId ->
            val newRole = when (checkedId) {
                rbFarmer?.id -> RoleStateManager.ROLE_FARMER
                rbBuyer?.id -> RoleStateManager.ROLE_BUYER
                else -> RoleStateManager.ROLE_BOTH
            }
            RoleStateManager.setRole(ctx, newRole)
            adaptQuickLinksForRole(newRole)

            val label = when (newRole) {
                RoleStateManager.ROLE_FARMER -> "🌾 किसान मोड सक्रिय (विक्रेता विकल्प चालू)"
                RoleStateManager.ROLE_BUYER -> "🛒 खरीदार मोड सक्रिय (खरीद विकल्प चालू)"
                else -> "🔄 दोनों मोड सक्रिय (किसान + खरीदार डबल विकल्प चालू)"
            }
            Toast.makeText(ctx, label, Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeRoleForQuickLinks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                RoleStateManager.roleFlow.collect { role ->
                    adaptQuickLinksForRole(role)
                }
            }
        }
    }

    /**
     * किसान चुनने पर किसान के विकल्प, खरीदार चुनने पर खरीदार के विकल्प,
     * और दोनों (BOTH) चुनने पर सारे विकल्प डबल दिखेंगे।
     */
    private fun adaptQuickLinksForRole(role: String) {
        when (role.trim().uppercase()) {
            RoleStateManager.ROLE_FARMER -> {
                btnManageCrops?.visibility = View.VISIBLE
                btnOrderHistory?.visibility = View.VISIBLE
                btnBuyerRequests?.visibility = View.GONE
            }
            RoleStateManager.ROLE_BUYER -> {
                btnManageCrops?.visibility = View.GONE
                btnOrderHistory?.visibility = View.VISIBLE
                btnBuyerRequests?.visibility = View.VISIBLE
            }
            else -> {
                // BOTH Mode: All Options Visible (Double)
                btnManageCrops?.visibility = View.VISIBLE
                btnOrderHistory?.visibility = View.VISIBLE
                btnBuyerRequests?.visibility = View.VISIBLE
            }
        }
    }

    private fun setupWorkingQuickLinks() {
        val root = view ?: return

        // अगर ID से न मिले तो टेक्स्ट से कार्ड ढूंढकर क्लिक चालू करना
        val ordersRow = btnOrderHistory ?: findClickableParentByText(root, "मेरे सभी ऑर्डर्स")
        val cropsRow = btnManageCrops ?: findClickableParentByText(root, "मेरी सूचीबद्ध फसलें")
        val requestsRow = btnBuyerRequests ?: findClickableParentByText(root, "मेरी मांगें")
        val logoutRow = btnLogout ?: findClickableParentByText(root, "सत्र समाप्त करें")

        ordersRow?.setOnClickListener {
            navigateSafe("ordersFragment", "nav_orders")
        }

        cropsRow?.setOnClickListener {
            navigateSafe("marketplaceFragment", "nav_marketplace")
        }

        requestsRow?.setOnClickListener {
            navigateSafe("buyerRequestsFragment", "nav_buyer_requests")
        }

        logoutRow?.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            GoogleAuthManager(ctx).signOut {
                ctx.getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE).edit().clear().apply()
                ctx.getSharedPreferences("mera_kisan_session", Context.MODE_PRIVATE).edit().clear().apply()
                RoleStateManager.setRole(ctx, RoleStateManager.ROLE_BOTH)
                Toast.makeText(ctx, "सत्र समाप्त (लॉगआउट सफल)", Toast.LENGTH_SHORT).show()
                navigateSafe("loginFragment")
            }
        }
    }

    private fun findClickableParentByText(root: View, snippet: String): View? {
        if (root is TextView && root.text?.toString()?.contains(snippet) == true) {
            return (root.parent as? View) ?: root
        }
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findClickableParentByText(root.getChildAt(i), snippet)
                if (found != null) return found
            }
        }
        return null
    }

    private fun findTextViewWithExactText(root: View, target: String): TextView? {
        if (root is TextView && root.text?.toString()?.trim() == target) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findTextViewWithExactText(root.getChildAt(i), target)
                if (found != null) return found
            }
        }
        return null
    }

    private fun <T : View> findFirstViewOfType(root: View, clazz: Class<T>): T? {
        if (clazz.isInstance(root)) return clazz.cast(root)
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findFirstViewOfType(root.getChildAt(i), clazz)
                if (found != null) return found
            }
        }
        return null
    }

    private fun navigateSafe(vararg destNames: String) {
        val ctx = context ?: return
        val pkg = ctx.packageName
        for (name in destNames) {
            val id = ctx.resources.getIdentifier(name, "id", pkg)
            if (id != 0) {
                try {
                    findNavController().navigate(id)
                    return
                } catch (_: Throwable) {}
            }
        }
    }

    private fun <T : View> findViewSafe(root: View, vararg idNames: String): T? {
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
