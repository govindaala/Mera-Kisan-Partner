// app/src/main/java/in/merakisan/app/core/config/FeatureManager.kt
package `in`.merakisan.app.core.config

import android.content.Context

object FeatureManager {

    // Master Spec v1.1 ke anusaar Approved P0/P1 Core Features jo hamesha default ON rahenge
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
        "ai_poster"
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

    fun isEnabled(featureKey: String): Boolean {
        return isCoreFeatureDefaultOn(featureKey)
    }

    fun isCoreFeatureDefaultOn(featureKey: String): Boolean {
        return DEFAULT_ENABLED_CORE_FEATURES.contains(featureKey.trim().lowercase())
    }

    fun isMaintenanceMode(context: Context): Boolean {
        return try {
            val repo = RemoteConfigRepository(context.applicationContext)
            repo.isMaintenanceMode()
        } catch (_: Throwable) {
            false
        }
    }
}
