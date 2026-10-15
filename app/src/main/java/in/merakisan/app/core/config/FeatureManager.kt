package `in`.merakisan.app.core.config

import android.content.Context
import `in`.merakisan.app.core.network.model.AppConfigDto

object FeatureManager {

    private const val PREF_CRASH_STORE = "mera_kisan_crash_guard"
    private const val KEY_LAST_CRASH = "last_crash_summary"

    // Core features जो लॉन्च पर डिफ़ॉल्ट रूप से चालू (ON) रहेंगे
    private val coreDefaultOnFeatures = setOf(
        "authentication",
        "farmer_mode",
        "buyer_mode",
        "product_listing",
        "product_search",
        "product_filter",
        "nearby_products",
        "buyer_request",
        "make_offer",
        "counter_offer",
        "negotiation",
        "favorites",
        "follow_farmer",
        "ratings",
        "reviews",
        "orders",
        "cod",
        "crop_boost",
        "notifications",
        "whatsapp",
        "call",
        "share",
        "facebook_share",
        "instagram_share",
        "telegram_share",
        "sms_share",
        "qr_share",
        "ai_poster",
        "mandi_price"
    )

    private val memoryFlags = mutableMapOf<String, Boolean>()

    @Volatile
    private var cachedMaintenanceMode: Boolean = false

    @Volatile
    private var cachedMaintenanceMessage: String =
        "Mera Kisan पर रखरखाव (Maintenance) चल रहा है। कृपया कुछ समय बाद प्रयास करें।"

    fun isCoreFeatureDefaultOn(featureKey: String): Boolean {
        val cleanKey = featureKey.trim().lowercase()
        return coreDefaultOnFeatures.contains(cleanKey)
    }

    fun isEnabled(
        context: Context?,
        featureKey: String,
        defaultState: Boolean = isCoreFeatureDefaultOn(featureKey)
    ): Boolean {
        val cleanKey = featureKey.trim().lowercase()
        if (context != null) {
            return try {
                RemoteConfigRepository(context).isFeatureActive(cleanKey, defaultState)
            } catch (_: Throwable) {
                memoryFlags[cleanKey] ?: defaultState
            }
        }
        return memoryFlags[cleanKey] ?: defaultState
    }

    fun isEnabled(
        featureKey: String,
        defaultState: Boolean = isCoreFeatureDefaultOn(featureKey)
    ): Boolean {
        val cleanKey = featureKey.trim().lowercase()
        return memoryFlags[cleanKey] ?: defaultState
    }

    fun updateFlags(newFlags: Map<String, Boolean>) {
        newFlags.forEach { (k, v) ->
            memoryFlags[k.trim().lowercase()] = v
        }
    }

    suspend fun syncRemoteConfig(context: Context): Result<AppConfigDto> {
        val repository = RemoteConfigRepository(context)
        val result = repository.fetchAndCacheConfig()
        result.onSuccess { dto ->
            cachedMaintenanceMode = dto.maintenanceMode
            if (!dto.bannerText.isNullOrBlank()) {
                cachedMaintenanceMessage = dto.bannerText
            }
        }
        return result
    }

    fun isMaintenanceMode(context: Context? = null): Boolean {
        if (context != null) {
            return try {
                RemoteConfigRepository(context).isMaintenanceMode()
            } catch (_: Throwable) {
                cachedMaintenanceMode
            }
        }
        return cachedMaintenanceMode
    }

    fun getMaintenanceMessage(context: Context? = null): String {
        if (context != null) {
            try {
                val banner = RemoteConfigRepository(context).getBannerText()
                if (!banner.isNullOrBlank()) {
                    return banner
                }
            } catch (_: Throwable) {
            }
        }
        return cachedMaintenanceMessage
    }

    fun consumeLastCrashSummary(context: Context? = null): String? {
        if (context == null) return null
        return try {
            val prefs = context.applicationContext
                .getSharedPreferences(PREF_CRASH_STORE, Context.MODE_PRIVATE)
            val summary = prefs.getString(KEY_LAST_CRASH, null)
            if (!summary.isNullOrBlank()) {
                prefs.edit().remove(KEY_LAST_CRASH).apply()
            }
            summary
        } catch (_: Throwable) {
            null
        }
    }

    fun recordCrashSummary(context: Context?, summary: String) {
        if (context == null || summary.isBlank()) return
        try {
            val prefs = context.applicationContext
                .getSharedPreferences(PREF_CRASH_STORE, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LAST_CRASH, summary).apply()
        } catch (_: Throwable) {
        }
    }
}
