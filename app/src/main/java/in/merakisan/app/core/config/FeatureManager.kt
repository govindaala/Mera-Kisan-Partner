// app/src/main/java/in/merakisan/app/core/config/FeatureManager.kt
package `in`.merakisan.app.core.config

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object FeatureManager {

    private const val PREF_CONFIG = "mera_kisan_config"
    private const val PREF_PARAMS = "mera_kisan_params"
    private const val PREF_CRASH = "mera_kisan_diagnostics"

    private val memoryFlags = mutableMapOf<String, Boolean>()
    private val memoryParams = mutableMapOf<String, String>()
    private var lastCrashSummaryMemory: String? = null
    private var isCacheLoaded = false

    private var maintenanceActive = false
    private var maintenanceMessage = ""
    private var minAppVersion = "1.0.0"
    private var latestAppVersion = "1.1.0"
    private var forceUpdate = false

    // P0/P1 Core Marketplace Features (Master Spec Sec 16 & 28)
    private val coreDefaultOnFeatures = setOf(
        "online_order",
        "make_offer",
        "offers",
        "negotiation",
        "buy_now",
        "small_quantity",
        "verified_farmer",
        "organic_verified",
        "farmer_rating",
        "buyer_rating",
        "reviews",
        "crop_boost",
        "boost",
        "whatsapp",
        "call",
        "distance_filter",
        "location_search",
        "mandi_price",
        "mandi_prices",
        "mandi",
        "buyer_request",
        "demand_board",
        "wishlist",
        "favorites",
        "ai_poster",
        "ai_recommendation",
        "oil_marketplace",
        "farm_box",
        "batch_traceability"
    )

    // Master Spec Sec 17: AdMob is strictly OFF by default
    private val defaultStates = mapOf(
        "admob" to false,
        "mandi_price" to true,
        "mandi_prices" to true,
        "buyer_request" to true,
        "demand_board" to true,
        "make_offer" to true,
        "small_quantity" to true,
        "crop_boost" to true,
        "ai_poster" to true,
        "oil_marketplace" to true,
        "farm_box" to true,
        "batch_traceability" to true
    )

    private val defaultParameters = mapOf(
        "boost_price" to "99",
        "boost_days" to "3",
        "boost_slots" to "4",
        "youtube_daily_limit" to "6",
        "request_radius" to "50",
        "max_requests_per_day" to "5",
        "token_percent" to "10",
        "commission_percent" to "2",
        "farm_box_price" to "399"
    )

    // ============================================================
    // 1. RemoteConfigRepository.kt (Line 43 & 87) Compatibility
    // ============================================================

    fun isCoreFeatureDefaultOn(key: String): Boolean {
        val clean = key.trim().lowercase(Locale.ROOT)
        if (clean == "admob") return false
        return coreDefaultOnFeatures.contains(clean) || (defaultStates[clean] ?: true)
    }

    fun isCoreFeatureDefaultOn(context: Context?, key: String): Boolean {
        return isCoreFeatureDefaultOn(key)
    }

    // ============================================================
    // 2. ApiClient.kt (Line 40 & 46) All Overloads Compatibility
    // ============================================================

    fun consumeLastCrashSummary(): String? {
        val summary = lastCrashSummaryMemory
        lastCrashSummaryMemory = null
        return summary
    }

    fun consumeLastCrashSummary(context: Context?): String? {
        val summary = lastCrashSummaryMemory
        lastCrashSummaryMemory = null
        if (context != null) {
            try {
                val prefs = context.getSharedPreferences(PREF_CRASH, Context.MODE_PRIVATE)
                val saved = prefs.getString("last_crash_summary", null)
                if (!saved.isNullOrBlank()) {
                    prefs.edit().remove("last_crash_summary").apply()
                    return saved
                }
            } catch (_: Throwable) {}
        }
        return summary
    }

    fun recordCrashSummary(summary: String?) {
        if (summary.isNullOrBlank()) return
        lastCrashSummaryMemory = summary.take(500)
    }

    fun recordCrashSummary(error: Throwable?) {
        if (error == null) return
        val msg = "${error.javaClass.simpleName}:${error.message ?: "unknown"}"
        lastCrashSummaryMemory = msg.take(500)
    }

    fun recordCrashSummary(tagOrEndpoint: String?, error: Throwable?) {
        val msg = "${tagOrEndpoint ?: "API"} -> ${error?.javaClass?.simpleName ?: "Error"}: ${error?.message ?: ""}"
        lastCrashSummaryMemory = msg.take(500)
    }

    fun recordCrashSummary(tagOrEndpoint: String?, summary: String?) {
        val msg = "${tagOrEndpoint ?: "API"}: ${summary ?: ""}"
        lastCrashSummaryMemory = msg.take(500)
    }

    fun recordCrashSummary(context: Context?, summary: String?) {
        if (summary.isNullOrBlank()) return
        val trimmed = summary.take(500)
        lastCrashSummaryMemory = trimmed
        saveCrashToPrefs(context, trimmed)
    }

    fun recordCrashSummary(context: Context?, error: Throwable?) {
        if (error == null) return
        val trimmed = "${error.javaClass.simpleName}:${error.message ?: ""}".take(500)
        lastCrashSummaryMemory = trimmed
        saveCrashToPrefs(context, trimmed)
    }

    fun recordCrashSummary(context: Context?, tagOrEndpoint: String?, error: Throwable?) {
        val trimmed = "${tagOrEndpoint ?: "API"} -> ${error?.javaClass?.simpleName}: ${error?.message ?: ""}".take(500)
        lastCrashSummaryMemory = trimmed
        saveCrashToPrefs(context, trimmed)
    }

    private fun saveCrashToPrefs(context: Context?, trimmed: String) {
        if (context == null) return
        try {
            context.getSharedPreferences(PREF_CRASH, Context.MODE_PRIVATE)
                .edit()
                .putString("last_crash_summary", trimmed)
                .apply()
        } catch (_: Throwable) {}
    }

    // ============================================================
    // 3. Remote Config Sync & Cache Management
    // ============================================================

    private fun ensureCacheLoaded(context: Context?) {
        if (isCacheLoaded || context == null) return
        try {
            val flagPrefs = context.getSharedPreferences(PREF_CONFIG, Context.MODE_PRIVATE)
            maintenanceActive = flagPrefs.getBoolean("maintenance_mode", false)
            maintenanceMessage = flagPrefs.getString("maintenance_message", "") ?: ""
            minAppVersion = flagPrefs.getString("min_app_version", "1.0.0") ?: "1.0.0"
            latestAppVersion = flagPrefs.getString("latest_app_version", "1.1.0") ?: "1.1.0"
            forceUpdate = flagPrefs.getBoolean("force_update", false)

            for ((k, v) in flagPrefs.all) {
                if (v is Boolean && k != "maintenance_mode" && k != "force_update") {
                    memoryFlags[k.lowercase(Locale.ROOT)] = v
                }
            }

            val paramPrefs = context.getSharedPreferences(PREF_PARAMS, Context.MODE_PRIVATE)
            for ((k, v) in paramPrefs.all) {
                if (v != null) {
                    memoryParams[k.lowercase(Locale.ROOT)] = v.toString()
                }
            }
            isCacheLoaded = true
        } catch (_: Throwable) {}
    }

    suspend fun syncRemoteConfig(context: Context) {
        ensureCacheLoaded(context)

        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://mera-kisan-backend.vercel.app/api/v1/config")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode in 200..299) {
                    val raw = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(raw)
                    val data = root.optJSONObject("data") ?: root

                    val maintNode = data.opt("maintenance_mode")
                    when (maintNode) {
                        is Boolean -> {
                            maintenanceActive = maintNode
                            maintenanceMessage = data.optString("banner_text", "")
                        }
                        is JSONObject -> {
                            maintenanceActive = maintNode.optBoolean("active", false) ||
                                maintNode.optString("status").equals("ON", ignoreCase = true)
                            maintenanceMessage = maintNode.optString(
                                "message",
                                data.optString("banner_text", "")
                            )
                        }
                        else -> {
                            maintenanceActive = false
                        }
                    }

                    minAppVersion = data.optString("min_app_version", "1.0.0")
                    latestAppVersion = data.optString("latest_app_version", "1.1.0")
                    forceUpdate = data.optBoolean("force_update", false)

                    val flagPrefs = context.getSharedPreferences(PREF_CONFIG, Context.MODE_PRIVATE)
                    val flagEditor = flagPrefs.edit()
                    flagEditor.putBoolean("maintenance_mode", maintenanceActive)
                    flagEditor.putString("maintenance_message", maintenanceMessage)
                    flagEditor.putString("min_app_version", minAppVersion)
                    flagEditor.putString("latest_app_version", latestAppVersion)
                    flagEditor.putBoolean("force_update", forceUpdate)

                    val featuresObj = data.optJSONObject("features")
                    if (featuresObj != null) {
                        val keys = featuresObj.keys()
                        while (keys.hasNext()) {
                            val rawKey = keys.next()
                            val cleanKey = rawKey.trim().lowercase(Locale.ROOT)
                            val node = featuresObj.opt(rawKey)
                            val isEnabled = parseFeatureNode(node, isCoreFeatureDefaultOn(cleanKey))
                            memoryFlags[cleanKey] = isEnabled
                            flagEditor.putBoolean(cleanKey, isEnabled)
                        }
                    }
                    flagEditor.apply()

                    val paramsObj = data.optJSONObject("parameters")
                    if (paramsObj != null) {
                        val paramPrefs = context.getSharedPreferences(PREF_PARAMS, Context.MODE_PRIVATE)
                        val paramEditor = paramPrefs.edit()
                        val pKeys = paramsObj.keys()
                        while (pKeys.hasNext()) {
                            val pk = pKeys.next().trim().lowercase(Locale.ROOT)
                            val pv = paramsObj.opt(pk)?.toString() ?: ""
                            if (pv.isNotEmpty()) {
                                memoryParams[pk] = pv
                                paramEditor.putString(pk, pv)
                            }
                        }
                        paramEditor.apply()
                    }
                }
            } catch (t: Throwable) {
                recordCrashSummary("syncRemoteConfig", t)
            }
        }
    }

    private fun parseFeatureNode(node: Any?, defaultVal: Boolean = true): Boolean {
        return when (node) {
            is Boolean -> node
            is Number -> node.toInt() > 0
            is String -> {
                val s = node.trim().uppercase(Locale.ROOT)
                s == "TRUE" || s == "ON" || s == "BETA" || s == "ENABLED" || s == "1"
            }
            is JSONObject -> {
                if (node.optBoolean("kill_switch", false)) return false
                if (node.optInt("rollout", 100) <= 0) return false
                if (node.has("enabled")) {
                    node.optBoolean("enabled", defaultVal)
                } else {
                    val status = node.optString("status", if (defaultVal) "ON" else "OFF").trim().uppercase(Locale.ROOT)
                    status == "ON" || status == "BETA" || status == "ENABLED"
                }
            }
            else -> defaultVal
        }
    }

    fun isFeatureEnabled(key: String): Boolean {
        val normKey = normalizeKey(key)
        if (normKey == "admob") {
            return memoryFlags["admob"] ?: false
        }
        return memoryFlags[normKey] ?: isCoreFeatureDefaultOn(normKey)
    }

    fun isFeatureEnabled(context: Context, key: String): Boolean {
        ensureCacheLoaded(context)
        val normKey = normalizeKey(key)
        val prefs = context.getSharedPreferences(PREF_CONFIG, Context.MODE_PRIVATE)
        if (prefs.contains(normKey)) {
            return prefs.getBoolean(normKey, isCoreFeatureDefaultOn(normKey))
        }
        return isFeatureEnabled(normKey)
    }

    fun isEnabled(key: String): Boolean = isFeatureEnabled(key)

    fun isEnabled(context: Context, key: String): Boolean = isFeatureEnabled(context, key)

    fun getParamInt(context: Context?, key: String, fallback: Int): Int {
        ensureCacheLoaded(context)
        val normKey = key.trim().lowercase(Locale.ROOT)
        val raw = memoryParams[normKey] ?: defaultParameters[normKey]
        return raw?.toDoubleOrNull()?.toInt() ?: fallback
    }

    fun isMaintenanceMode(context: Context): Boolean {
        ensureCacheLoaded(context)
        val prefs = context.getSharedPreferences(PREF_CONFIG, Context.MODE_PRIVATE)
        return prefs.getBoolean("maintenance_mode", maintenanceActive)
    }

    fun getMaintenanceMessage(context: Context): String {
        ensureCacheLoaded(context)
        val prefs = context.getSharedPreferences(PREF_CONFIG, Context.MODE_PRIVATE)
        val msg = prefs.getString("maintenance_message", maintenanceMessage) ?: ""
        return msg.ifBlank { "सिस्टम रखरखाव चल रहा है। कृपया कुछ समय बाद प्रयास करें।" }
    }

    private fun normalizeKey(raw: String): String {
        return when (val k = raw.trim().lowercase(Locale.ROOT)) {
            "mandi_prices", "mandi" -> "mandi_price"
            "demand_board", "requests" -> "buyer_request"
            "offers", "negotiation" -> "make_offer"
            "boost", "boost_feature" -> "crop_boost"
            "payment_enabled", "online_payment" -> "payment"
            "traceability", "qr_traceability" -> "batch_traceability"
            else -> k
        }
    }
}
