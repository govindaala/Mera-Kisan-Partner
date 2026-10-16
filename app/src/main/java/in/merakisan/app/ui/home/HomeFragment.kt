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
        loadFeatureControlsAndCounters()

        return view
    }

    private fun initViews(v: View) {
        // सेफ व्यू-रिज़ॉल्वर: लेआउट में camelCase या snake_case जो भी हो, यह उसे सुरक्षित रूप से बाइंड करेगा
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
        // नई फसल जोड़ें बटन (Add Crop Screen खोलना)
        btnAddCrop?.setOnClickListener {
            navigateSafe("addProductFragment", "addCropFragment", "marketplaceFragment")
        }

        // मंडी भाव कार्ड पर क्लिक
        cardMandiBhav?.setOnClickListener {
            navigateSafe("mandiPricesFragment", "mandiFragment")
        }

        // मांग बोर्ड कार्ड पर क्लिक
        cardDemandBoard?.setOnClickListener {
            navigateSafe("buyerRequestsFragment", "demandBoardFragment")
        }
    }

    private fun loadFeatureControlsAndCounters() {
        val context = requireContext()

        // लचीला और सुरक्षित फीचर चेक: SharedPreferences और FeatureManager दोनों से सिंक
        val isMandiOn = checkFeatureActive(context, "mandi_price", "mandi_prices")
        val isDemandOn = checkFeatureActive(context, "buyer_request", "demand_board")

        cardMandiBhav?.visibility = if (isMandiOn) View.VISIBLE else View.GONE
        cardDemandBoard?.visibility = if (isDemandOn) View.VISIBLE else View.GONE

        // बैकएंड से किसान के वास्तविक लाइव काउंटर्स लोड करना
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.getSharedPreferences("mera_kisan_user", Context.MODE_PRIVATE)
                val uid = prefs.getString("user_uid", "") ?: ""
                if (uid.isNotEmpty()) {
                    val url = URL("https://mera-kisan-backend.vercel.app/api/v1/products?farmer_uid=$uid")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 6000
                    conn.readTimeout = 6000

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

    /**
     * सुरक्षित फीचर चेकर:
     * 1. पहले SharedPreferences से चेक करता है जहाँ FeatureManager.syncRemoteConfig डेटा स्टोर करता है।
     * 2. यदि SharedPreferences में नहीं है, तो रिफ्लेक्शन से FeatureManager के सही मेथड (isEnabled / isFeatureEnabled) को कॉल करता है।
     * 3. यदि कोई मेथड न मिले, तो डिफ़ॉल्ट true रखता है ताकि कोर फीचर्स व्यर्थ ही हाइड न हों।
     */
    private fun checkFeatureActive(context: Context, vararg featureKeys: String): Boolean {
        // 1. SharedPreferences चेक
        val featurePrefs = context.getSharedPreferences("mera_kisan_features", Context.MODE_PRIVATE)
        for (key in featureKeys) {
            if (featurePrefs.contains(key)) {
                return featurePrefs.getBoolean(key, true)
            }
        }

        val configPrefs = context.getSharedPreferences("mera_kisan_config", Context.MODE_PRIVATE)
        for (key in featureKeys) {
            if (configPrefs.contains(key)) {
                return configPrefs.getBoolean(key, true)
            }
        }

        // 2. FeatureManager ऑब्जेक्ट के मेथड्स का सुरक्षित रिफ्लेक्शन
        try {
            val fmClass = FeatureManager::class.java
            for (method in fmClass.methods) {
                if (method.name in listOf("isEnabled", "isFeatureEnabled", "isFeatureActive", "isAllowed")) {
                    for (key in featureKeys) {
                        val result = if (method.parameterTypes.size == 1 && method.parameterTypes[0] == String::class.java) {
                            method.invoke(FeatureManager, key) as? Boolean
                        } else if (method.parameterTypes.size == 2 && method.parameterTypes[0] == Context::class.java && method.parameterTypes[1] == String::class.java) {
                            method.invoke(FeatureManager, context, key) as? Boolean
                        } else null

                        if (result != null) return result
                    }
                }
            }
        } catch (_: Throwable) {}

        // कोर फीचर्स के लिए फॉलबैक
        return true
    }

    private fun navigateSafe(vararg destinationNames: String) {
        val context = context ?: return
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
