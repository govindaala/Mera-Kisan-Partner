package `in`.merakisan.app.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import `in`.merakisan.app.R
import `in`.merakisan.app.core.config.FeatureManager
import `in`.merakisan.app.core.state.RoleStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class HomeFragment : Fragment() {

    // Farmer Center Views (Master Spec Sec 5)
    private var layoutFarmerCenter: View? = null
    private var tvGreeting: TextView? = null
    private var tvUserLocation: TextView? = null
    private var btnAddCrop: View? = null
    private var tvActiveCropsCount: TextView? = null
    private var tvPendingOrdersCount: TextView? = null

    // Buyer Center Views
    private var layoutBuyerCenter: View? = null
    private var cardMandiBhav: View? = null
    private var cardDemandBoard: View? = null
    private var progressBarFreshToday: ProgressBar? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        initViews(root)
        renderGreetingAndUserData(root)
        setupClickListeners()
        observeRoleState()
        loadFeatureControls()
        loadFarmerCounters()

        return root
    }

    private fun initViews(v: View) {
        layoutFarmerCenter = findViewSafe(v, "layout_farmer_center", "layoutFarmerCenter", "card_farmer_dashboard", "farmerDashboardCard")
        tvGreeting = findViewSafe(v, "tv_greeting", "tvGreeting", "tvUserGreeting", "greetingTextView")
        tvUserLocation = findViewSafe(v, "tv_user_location", "tvUserLocation", "locationTextView")
        btnAddCrop = findViewSafe(v, "btn_add_crop", "btnAddCrop", "btn_add_product", "btnAddProduct")
        tvActiveCropsCount = findViewSafe(v, "tv_active_crops_count", "tvActiveCropsCount", "tvActiveCrops")
        tvPendingOrdersCount = findViewSafe(v, "tv_pending_orders_count", "tvPendingOrdersCount", "tvPendingOrders")

        layoutBuyerCenter = findViewSafe(v, "layout_buyer_center", "layoutBuyerCenter", "search_container", "layoutSearch")
        cardMandiBhav = findViewSafe(v, "card_mandi_bhav", "cardMandiBhav", "cardMandi")
        cardDemandBoard = findViewSafe(v, "card_demand_board", "cardDemandBoard", "cardDemand")
        progressBarFreshToday = findViewSafe(v, "progressBarFreshToday", "progressBarHome", "progress_bar")
    }

    private fun renderGreetingAndUserData(root: View) {
        val context = context ?: return
        val prefs = context.getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)

        var name: String? = null
        try {
            name = GoogleSignIn.getLastSignedInAccount(context)?.displayName?.trim()
        } catch (_: Throwable) {}

        if (name.isNullOrBlank()) {
            name = prefs.getString("user_name", "किसान साथी") ?: "किसान साथी"
        }

        val district = prefs.getString("user_district", "मध्य प्रदेश") ?: "मध्य प्रदेश"

        tvGreeting?.text = "नमस्ते, $name"
        tvUserLocation?.text = district
    }

    /**
     * Master Spec Sec 5: Active Role ke mutabik Home screen ka roop badalna
     */
    private fun observeRoleState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                RoleStateManager.roleFlow.collect { role ->
                    adaptHomeLayoutForRole(role)
                }
            }
        }
    }

    private fun adaptHomeLayoutForRole(role: String) {
        when (role.trim().uppercase()) {
            RoleStateManager.ROLE_FARMER -> {
                // Farmer Mode: Farmer Center VISIBLE, Buyer search GONE
                layoutFarmerCenter?.visibility = View.VISIBLE
                btnAddCrop?.visibility = View.VISIBLE
                layoutBuyerCenter?.visibility = View.GONE
            }
            RoleStateManager.ROLE_BUYER -> {
                // Buyer Mode: Farmer Center GONE, Buyer search VISIBLE
                layoutFarmerCenter?.visibility = View.GONE
                btnAddCrop?.visibility = View.GONE
                layoutBuyerCenter?.visibility = View.VISIBLE
            }
            else -> {
                // BOTH Mode: Options Double (Both sections visible)
                layoutFarmerCenter?.visibility = View.VISIBLE
                btnAddCrop?.visibility = View.VISIBLE
                layoutBuyerCenter?.visibility = View.VISIBLE
            }
        }
    }

    private fun setupClickListeners() {
        // '+ नई फसल जोड़ें (ADD CROP)' Button action
        btnAddCrop?.setOnClickListener {
            navigateSafe("addProductFragment", "addCropFragment", "marketplaceFragment")
        }

        // Mandi card par tap karte hi MandiPricesFragment khulega
        cardMandiBhav?.setOnClickListener {
            navigateSafe("mandiPricesFragment", "mandiFragment")
        }

        // Buyer demand card par tap
        cardDemandBoard?.setOnClickListener {
            navigateSafe("buyerRequestsFragment")
        }
    }

    private fun loadFeatureControls() {
        val isMandiOn = try {
            FeatureManager.isFeatureEnabled("mandi_price") || FeatureManager.isFeatureEnabled("mandi_prices")
        } catch (_: Throwable) { true }

        val isDemandOn = try {
            FeatureManager.isFeatureEnabled("buyer_request") || FeatureManager.isFeatureEnabled("demand_board")
        } catch (_: Throwable) { true }

        cardMandiBhav?.visibility = if (isMandiOn) View.VISIBLE else View.GONE
        cardDemandBoard?.visibility = if (isDemandOn) View.VISIBLE else View.GONE

        // Master Spec Sec 24: Fail Gracefully — Spinner kabhi freeze na rahe
        progressBarFreshToday?.visibility = View.GONE
    }

    private fun loadFarmerCounters() {
        val context = context ?: return
        val prefs = context.getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
        val uid = prefs.getString("user_uid", "") ?: ""

        if (uid.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://mera-kisan-backend.vercel.app/api/v1/products?farmer_uid=$uid")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode in 200..299) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(text)
                    val dataArr = json.optJSONArray("data")
                    val count = dataArr?.length() ?: 0
                    withContext(Dispatchers.Main) {
                        tvActiveCropsCount?.text = count.toString()
                    }
                }
            } catch (_: Throwable) {
            } finally {
                withContext(Dispatchers.Main) {
                    progressBarFreshToday?.visibility = View.GONE
                }
            }
        }
    }

    private fun navigateSafe(vararg destinationNames: String) {
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
