// app/src/main/java/in/merakisan/app/core/config/FeatureManager.kt
package `in`.merakisan.app.core.config

import android.content.Context

object FeatureManager {

    // Master Spec v1.1 के अनुसार Approved P0/P1 Core Features जो डिफ़ॉल्ट रूप से ON रहेंगे
    // (Future/HOLD फ़ीचर्स जैसे admob, auction, wallet, chat डिफ़ॉल्ट OFF रहेंगे)
    private val DEFAULT_ENABLED_CORE_FEATURES = setOf(
        "authentication",
        "farmer_mode",
        "buyer_mode",
        "product_listing",
        "product_search",
        "product_filter",
        "nearby_products",
        "small_quantity",
        "fresh_today",
        "buyer_request",
        "make_offer",
        "counter_offer",
        "negotiation",
        "favorites",
        "follow_farmer",
        "ratings",
        "reviews",
        "farmer_verification",
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
        "media_upload",
        "video_upload",
        "photo_upload"
    )

    suspend fun syncRemoteConfig(context: Context): Boolean {
        return try {
            val repo = RemoteConfigRepository(context.applicationContext)
            val result = repo.fetchAndCacheConfig()
            result.isSuccess
        } catch (_: Throwable) {
            false
        }
    }

    fun isFeatureEnabled(
        context: Context,
        featureKey: String,
        defaultValue: Boolean = isCoreFeatureDefaultOn(featureKey)
    ): Boolean {
        return try {
            val normalizedKey = featureKey.trim().lowercase()
            val fallback = defaultValue || isCoreFeatureDefaultOn(normalizedKey)
            val repo = RemoteConfigRepository(context.applicationContext)
            repo.isFeatureActive(normalizedKey, fallback)
        } catch (_: Throwable) {
            defaultValue || isCoreFeatureDefaultOn(featureKey)
        }
    }

    // Adapters, Fragments और Workers के लिए Context के साथ isEnabled ओवरलोड
    fun isEnabled(
        context: Context,
        featureKey: String,
        defaultValue: Boolean = isCoreFeatureDefaultOn(featureKey)
    ): Boolean {
        return isFeatureEnabled(context, featureKey, defaultValue)
    }

    // बिना Context के कॉल करने वाले हिस्सों के लिए ओवरलोड
    fun isEnabled(
        featureKey: String,
        defaultValue: Boolean = isCoreFeatureDefaultOn(featureKey)
    ): Boolean {
        return defaultValue || isCoreFeatureDefaultOn(featureKey)
    }

    fun isCoreFeatureDefaultOn(featureKey: String): Boolean {
        return DEFAULT_ENABLED_CORE_FEATURES.contains(featureKey.trim().lowercase())
    }

    fun isMaintenanceMode(context: Context? = null): Boolean {
        if (context == null) return false
        return try {
            val repo = RemoteConfigRepository(context.applicationContext)
            repo.isMaintenanceMode()
        } catch (_: Throwable) {
            false
        }
    }

    // MainActivity.kt:92 के लिए आवश्यक मेथड
    fun getMaintenanceMessage(context: Context? = null): String {
        if (context != null) {
            try {
                val repo = RemoteConfigRepository(context.applicationContext)
                val banner = repo.getBannerText()
                if (!banner.isNullOrBlank()) return banner
            } catch (_: Throwable) {
            }
        }
        return "Mera Kisan पर रखरखाव (Maintenance) कार्य चल रहा है। कृपया कुछ समय बाद प्रयास करें।"
    }
}
