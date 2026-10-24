// app/src/main/java/in/merakisan/app/core/config/FeatureManager.kt
package `in`.merakisan.app.core.config

import android.content.Context
import `in`.merakisan.app.core.network.ApiClient
import org.json.JSONObject

/**
 * MERA KISAN — CENTRAL FEATURE FLAG & REMOTE CONFIG ENGINE
 * नियम: AdMob और सभी HOLD फीचर्स डिफ़ॉल्ट रूप से OFF रहेंगे तथा demo_fallback_mode स्थायी रूप से OFF रहेगा।
 */
object FeatureManager {

    private const val PREFS_NAME = "mera_kisan_remote_feature_flags_v2"
    private const val KEY_FLAGS_JSON = "flags_json"
    private const val KEY_PARAMS_JSON = "params_json"

    // Core P0/P1 फीचर्स जो डिफ़ॉल्ट रूप से चालू हैं
    private val DEFAULT_ENABLED_FEATURES = setOf(
        "authentication",
        "farmer_mode",
        "buyer_mode",
        "product_listing",
        "product_search",
        "product_filter",
        "small_quantity",
        "nearby_products",
        "fresh_today",
        "seeds_marketplace",
        "agri_inputs_marketplace",
        "agri_dealer_portal",
        "pickup_points",
        "buyer_request",
        "make_offer",
        "counter_offer",
        "negotiation",
        "favorites",
        "follow_farmer",
        "ratings",
        "reviews",
        "farmer_verification",
        "organic_verification",
        "lab_verification",
        "orders",
        "cod",
        "whatsapp",
        "call",
        "share",
        "qr_share",
        "ai_poster",
        "mandi_price",
        "price_history",
        "fair_price",
        "notifications"
    )

    @Volatile
    private var memoryFlags: JSONObject = JSONObject()

    @Volatile
    private var memoryParams: JSONObject = JSONObject()

    fun initFromLocalCache(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val rawFlags = prefs.getString(KEY_FLAGS_JSON, null)
            if (!rawFlags.isNullOrBlank()) {
                memoryFlags = JSONObject(rawFlags)
            }
            val rawParams = prefs.getString(KEY_PARAMS_JSON, null)
            if (!rawParams.isNullOrBlank()) {
                memoryParams = JSONObject(rawParams)
            }
        } catch (_: Exception) {
            memoryFlags = JSONObject()
            memoryParams = JSONObject()
        }
    }

    /**
     * एकल कनेक्शन केंद्र (ApiClient) के माध्यम से सर्वर से लाइव फीचर फ्लैग्स और रिमोट पैरामीटर्स सिंक करें।
     */
    suspend fun syncFromServer(context: Context): Boolean {
        initFromLocalCache(context)
        val result = ApiClient.request(context, "/config", method = "GET")
        if (!result.isSuccess || result.dataObject == null) {
            return false
        }

        val data = result.dataObject
        val serverFeatures = data.optJSONObject("features") ?: JSONObject()
        val serverParams = data.optJSONObject("parameters") ?: JSONObject()

        // सुरक्षा नियम: डमी डेटा फॉलबैक हमेशा OFF रहेगा
        serverParams.put("demo_fallback_mode", "OFF")

        memoryFlags = serverFeatures
        memoryParams = serverParams

        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FLAGS_JSON, serverFeatures.toString())
            .putString(KEY_PARAMS_JSON, serverParams.toString())
            .apply()

        return true
    }

    fun isEnabled(featureKey: String): Boolean {
        val key = featureKey.trim().lowercase()
        if (key == "demo_fallback_mode") return false

        if (memoryFlags.has(key)) {
            val raw = memoryFlags.opt(key)
            when (raw) {
                is Boolean -> return raw
                is String -> return raw.uppercase() in listOf("ON", "ENABLED", "TRUE", "BETA")
                is JSONObject -> {
                    if (raw.optBoolean("kill_switch", false)) return false
                    val st = raw.optString("status", "OFF").uppercase()
                    return st == "ON" || st == "ENABLED" || st == "BETA"
                }
            }
        }

        return DEFAULT_ENABLED_FEATURES.contains(key)
    }

    fun getHomeBannerText(): String {
        val remote = memoryParams.optString("home_banner_hi", "").trim()
        return if (remote.isNotEmpty()) {
            remote
        } else {
            "किसान से सीधे आपकी जरूरत तक — ताज़ा व देशी कृषि उत्पाद"
        }
    }

    fun getSupportWhatsApp(): String =
        memoryParams.optString("support_whatsapp", "").trim()

    fun isMaintenanceMode(): Boolean =
        memoryParams.optString("maintenance_mode", "OFF").uppercase() == "ON"
}
