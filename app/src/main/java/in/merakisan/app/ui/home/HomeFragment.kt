package `in`.merakisan.app.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import `in`.merakisan.app.R
import `in`.merakisan.app.core.config.FeatureManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class HomeFragment : Fragment() {

    private var tvGreeting: TextView? = null
    private var tvUserLocation: TextView? = null
    private var btnAddCrop: View? = null
    private var cardMandiBhav: View? = null
    private var cardDemandBoard: View? = null
    private var tvActiveCropsCount: TextView? = null
    private var tvPendingOrdersCount: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        initViews(view)
        populateUserData(view)
        setupClickListeners()
        loadRemoteConfigAndCounters()

        return view
    }

    private fun initViews(v: View) {
        tvGreeting = findViewSafe(v, "tv_greeting", "tvGreeting", "greetingTextView", "tvUserGreeting")
        tvUserLocation = findViewSafe(v, "tv_user_location", "tvUserLocation", "tvLocation", "locationTextView")
        btnAddCrop = findViewSafe(v, "btn_add_crop", "btnAddCrop", "btnAddProduct", "btn_add_product")
        cardMandiBhav = findViewSafe(v, "card_mandi_bhav", "cardMandiBhav", "cardMandi", "mandiCard")
        cardDemandBoard = findViewSafe(v, "card_demand_board", "cardDemandBoard", "cardDemand", "demandCard")
        tvActiveCropsCount = findViewSafe(v, "tv_active_crops_count", "tvActiveCropsCount", "tvActiveCrops")
        tvPendingOrdersCount = findViewSafe(v, "tv_pending_orders_count", "tvPendingOrdersCount", "tvPendingOrders")
    }

    private fun populateUserData(root: View) {
        val prefs = requireContext().getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
        val userName = prefs.getString("user_name", "किसान साथी") ?: "किसान साथी"
        val district = prefs.getString("user_district", "मध्य प्रदेश") ?: "मध्य प्रदेश"

        // यदि आईडी से नहीं मिला तो स्क्रीन पर मौजूद टेक्स्ट से ग्रीटिंग ढूंढकर अपडेट करना
        if (tvGreeting != null) {
            tvGreeting?.text = "नमस्ते, $userName"
        } else {
            findTextViewByTextPrefix(root, "नमस्ते")?.text = "नमस्ते, $userName"
        }

        if (tvUserLocation != null) {
            tvUserLocation?.text = district
        }
    }

    private fun setupClickListeners() {
        btnAddCrop?.setOnClickListener {
            // सेफ नेविगेशन: जो भी डेस्टिनेशन ग्राफ में हो उसे खोलना
            navigateSafe("addProductFragment", "addCropFragment", "marketplaceFragment")
        }

        cardMandiBhav?.setOnClickListener {
            navigateSafe("mandiPricesFragment", "mandiFragment")
        }

        cardDemandBoard?.setOnClickListener {
            navigateSafe("buyerRequestsFragment", "demandBoardFragment")
        }
    }

    private fun loadRemoteConfigAndCounters() {
        val isMandiOn = try {
            FeatureManager.isFeatureEnabled("mandi_price") || FeatureManager.isFeatureEnabled("mandi_prices")
        } catch (_: Throwable) { true }

        val isDemandOn = try {
            FeatureManager.isFeatureEnabled("buyer_request") || FeatureManager.isFeatureEnabled("demand_board")
        } catch (_: Throwable) { true }

        cardMandiBhav?.visibility = if (isMandiOn) View.VISIBLE else View.GONE
        cardDemandBoard?.visibility = if (isDemandOn) View.VISIBLE else View.GONE

        // बैकएंड से किसान के काउंटर्स लोड करना
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = requireContext().getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
                val uid = prefs.getString("user_uid", "") ?: ""
                if (uid.isNotEmpty()) {
                    val url = URL("https://mera-kisan-backend.vercel.app/api/v1/products?farmer_uid=$uid")
                    val conn = url.openConnection() as HttpURLConnection
                    if (conn.responseCode == 200) {
                        val text = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(text)
                        val dataArr = json.optJSONArray("data")
                        val count = dataArr?.length() ?: 0
                        withContext(Dispatchers.Main) {
                            tvActiveCropsCount?.text = count.toString()
                        }
                    }
                }
            } catch (_: Exception) {}
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

    private fun findTextViewByTextPrefix(view: View, prefix: String): TextView? {
        if (view is TextView && view.text?.toString()?.startsWith(prefix) == true) {
            return view
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findTextViewByTextPrefix(view.getChildAt(i), prefix)
                if (found != null) return found
            }
        }
        return null
    }
}
