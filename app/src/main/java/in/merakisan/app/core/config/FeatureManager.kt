// app/src/main/java/in/merakisan/app/core/config/FeatureManager.kt
package `in`.merakisan.app.core.config

import android.content.Context
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.core.security.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

/**
 * MERA KISAN — COMPLETE CENTRAL FEATURE CONTROL & REMOTE CONFIG ENGINE
 * Includes `consumeLastCrashSummary` for HomeViewModel.kt:54 compatibility.
 */
object FeatureManager {

    private const val PREFS_NAME = "mera_kisan_master_feature_config_v2"
    private const val KEY_FLAGS_JSON = "feature_flags_json"
    private const val KEY_PARAMS_JSON = "remote_parameters_json"
    private const val KEY_SERVICES_STATUS_JSON = "services_status_json"
    private const val KEY_LAST_SYNC_MS = "last_sync_timestamp_ms"
    private const val KEY_LAST_CRASH_SUMMARY = "last_crash_summary"

    const val CURRENT_APP_VERSION = "1.3.0"

    enum class FeatureStatus {
        ON,
        BETA,
        COMING_SOON,
        OFF
    }

    private val DEFAULT_ON_FEATURES = setOf(
        "authentication",
        "farmer_mode",
        "buyer_mode",
        "product_listing",
        "product_search",
        "product_filter",
        "small_quantity",
        "nearby_products",
        "nearby_farmers",
        "fresh_today",
        "location_search",
        "village_filter",
        "district_filter",
        "seeds_marketplace",
        "agri_inputs_marketplace",
        "agri_dealer_portal",
        "pickup_points",
        "pickup",
        "buyer_request",
        "make_offer",
        "counter_offer",
        "negotiation",
        "offers",
        "favorites",
        "wishlist",
        "follow_farmer",
        "ratings",
        "farmer_rating",
        "buyer_rating",
        "reviews",
        "farmer_verification",
        "verified_farmer",
        "buyer_verification",
        "organic_verification",
        "organic_verified",
        "organic",
        "lab_verification",
        "orders",
        "online_order",
        "cod",
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
        "mandi_price",
        "mandi",
        "price_history",
        "fair_price",
        "compare",
        "analytics"
    )

    private val DEFAULT_OFF_HOLD_FEATURES = setOf(
        "payment",
        "payments",
        "online_payment",
        "token_payment",
        "escrow",
        "refund",
        "payout",
        "crop_boost",
        "boost",
        "featured_product",
        "sponsored_product",
        "advertisement",
        "admob",
        "chat",
        "sms",
        "email",
        "map_view",
        "maps",
        "location",
        "farm_box",
        "group_buying",
        "pre_order",
        "delivery",
        "collection_center",
        "oil_marketplace",
        "oil_mill",
        "oil_batch",
        "traceability",
        "price_alert",
        "price_recommendation",
        "demand_analytics",
        "saved_search",
        "trust_score",
        "subscription",
        "premium_farmer",
        "premium",
        "buyer_membership",
        "referral",
        "coupon",
        "wallet",
        "auction",
        "ai",
        "ai_search",
        "ai_recommendation",
        "ai_price",
        "ai_demand",
        "ai_image_analysis",
        "ai_description",
        "ai_moderation",
        "ai_assistant",
        "video",
        "youtube",
        "maintenance",
        "force_update",
        "demo_fallback_mode"
    )

    private val FEATURE_KEY_ALIASES = mapOf(
        "boost" to "crop_boost",
        "boost_feature" to "crop_boost",
        "payment_enabled" to "online_payment",
        "payment" to "online_payment",
        "payments" to "online_payment",
        "wishlist" to "favorites",
        "verified_farmer" to "farmer_verification",
        "organic_verified" to "organic_verification",
        "organic" to "organic_verification",
        "mandi" to "mandi_price",
        "oil_mill" to "oil_marketplace",
        "maps" to "map_view"
    )

    @Volatile
    private var memoryFlags: JSONObject = JSONObject()

    @Volatile
    private var memoryParams: JSONObject = JSONObject()

    @Volatile
    private var memoryServicesStatus: JSONObject = JSONObject()

    @Volatile
    private var isInitialized: Boolean = false

    @Volatile
    private var lastCrashInMemory: String? = null

    fun initFromLocalCache(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            prefs.getString(KEY_FLAGS_JSON, null)?.takeIf { it.isNotBlank() }?.let {
                memoryFlags = JSONObject(it)
            }
            prefs.getString(KEY_PARAMS_JSON, null)?.takeIf { it.isNotBlank() }?.let {
                memoryParams = JSONObject(it)
            }
            prefs.getString(KEY_SERVICES_STATUS_JSON, null)?.takeIf { it.isNotBlank() }?.let {
                memoryServicesStatus = JSONObject(it)
            }
            isInitialized = true
        } catch (_: Exception) {
            memoryFlags = JSONObject()
            memoryParams = JSONObject()
            memoryServicesStatus = JSONObject()
        }
    }

    /**
     * Resolves `HomeViewModel.kt:54:43 Unresolved reference: consumeLastCrashSummary`
     */
    @JvmStatic
    @JvmOverloads
    fun consumeLastCrashSummary(context: Context? = null): String? {
        val inMem = lastCrashInMemory
        if (!inMem.isNullOrBlank()) {
            lastCrashInMemory = null
            return inMem
        }
        if (context != null) {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs.getString(KEY_LAST_CRASH_SUMMARY, null)
            if (!saved.isNullOrBlank()) {
                prefs.edit().remove(KEY_LAST_CRASH_SUMMARY).apply()
                return saved
            }
            return SessionManager.consumeLastCrashSummary(context)
        }
        return null
    }

    @JvmStatic
    @JvmOverloads
    fun saveCrashSummary(summary: String, context: Context? = null) {
        if (summary.isBlank()) return
        lastCrashInMemory = summary.trim()
        if (context != null) {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_CRASH_SUMMARY, summary.trim())
                .apply()
        }
    }

    suspend fun syncFromServer(context: Context): Boolean {
        if (!isInitialized) {
            initFromLocalCache(context)
        }

        val result = ApiClient.request(context, "/config", method = "GET")
        if (!result.isSuccess || result.dataObject == null) {
            return false
        }

        val data = result.dataObject
        val serverFeatures = data.optJSONObject("features") ?: JSONObject()
        val serverParams = data.optJSONObject("parameters") ?: JSONObject()
        val serverServices = data.optJSONObject("services_status") ?: JSONObject()

        serverParams.put("demo_fallback_mode", "OFF")

        memoryFlags = serverFeatures
        memoryParams = serverParams
        memoryServicesStatus = serverServices
        isInitialized = true

        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FLAGS_JSON, serverFeatures.toString())
            .putString(KEY_PARAMS_JSON, serverParams.toString())
            .putString(KEY_SERVICES_STATUS_JSON, serverServices.toString())
            .putLong(KEY_LAST_SYNC_MS, System.currentTimeMillis())
            .apply()

        return true
    }

    suspend fun syncRemoteConfig(context: Context): Boolean =
        syncFromServer(context)

    @JvmStatic
    fun isCoreFeatureDefaultOn(rawKey: String): Boolean {
        val key = resolveCanonicalKey(rawKey)
        if (key == "demo_fallback_mode" || key == "admob") return false
        return DEFAULT_ON_FEATURES.contains(key)
    }

    data class EvaluatedFeatureRule(
        val key: String,
        val status: FeatureStatus,
        val rolloutPercent: Int = 100,
        val minAppVersion: String? = null,
        val maxAppVersion: String? = null,
        val allowedRoles: List<String> = emptyList(),
        val allowedDistricts: List<String> = emptyList(),
        val betaUsers: List<String> = emptyList(),
        val killSwitch: Boolean = false
    )

    private fun resolveCanonicalKey(rawKey: String): String {
        val clean = rawKey.trim().lowercase()
        return FEATURE_KEY_ALIASES[clean] ?: clean
    }

    fun getEvaluatedFeatureRule(rawKey: String): EvaluatedFeatureRule {
        val key = resolveCanonicalKey(rawKey)
        if (key == "demo_fallback_mode") {
            return EvaluatedFeatureRule(key = key, status = FeatureStatus.OFF, rolloutPercent = 0, killSwitch = true)
        }

        val rawValue = if (memoryFlags.has(key)) {
            memoryFlags.opt(key)
        } else if (memoryFlags.has(rawKey.trim().lowercase())) {
            memoryFlags.opt(rawKey.trim().lowercase())
        } else {
            null
        }

        if (rawValue == null) {
            val defaultStatus = if (DEFAULT_ON_FEATURES.contains(key)) FeatureStatus.ON else FeatureStatus.OFF
            return EvaluatedFeatureRule(
                key = key,
                status = defaultStatus,
                rolloutPercent = if (defaultStatus == FeatureStatus.ON) 100 else 0
            )
        }

        return when (rawValue) {
            is Boolean -> EvaluatedFeatureRule(
                key = key,
                status = if (rawValue) FeatureStatus.ON else FeatureStatus.OFF,
                rolloutPercent = if (rawValue) 100 else 0
            )
            is String -> {
                val st = when (rawValue.trim().uppercase()) {
                    "ON", "ENABLED", "TRUE" -> FeatureStatus.ON
                    "BETA" -> FeatureStatus.BETA
                    "COMING_SOON", "SOON" -> FeatureStatus.COMING_SOON
                    else -> FeatureStatus.OFF
                }
                EvaluatedFeatureRule(
                    key = key,
                    status = st,
                    rolloutPercent = if (st == FeatureStatus.OFF) 0 else 100
                )
            }
            is JSONObject -> {
                val killed = rawValue.optBoolean("kill_switch", false)
                val statusStr = rawValue.optString("status", if (rawValue.optBoolean("enabled", false)) "ON" else "OFF").uppercase()
                val parsedStatus = if (killed) {
                    FeatureStatus.OFF
                } else {
                    when (statusStr) {
                        "ON", "ENABLED", "TRUE" -> FeatureStatus.ON
                        "BETA" -> FeatureStatus.BETA
                        "COMING_SOON", "SOON" -> FeatureStatus.COMING_SOON
                        else -> FeatureStatus.OFF
                    }
                }
                val rollout = rawValue.optInt("rollout", if (parsedStatus == FeatureStatus.OFF) 0 else 100).coerceIn(0, 100)
                val minVer = rawValue.optString("min_app_version", "").takeIf { it.isNotBlank() }
                val maxVer = rawValue.optString("max_app_version", "").takeIf { it.isNotBlank() && it != "null" }

                EvaluatedFeatureRule(
                    key = key,
                    status = parsedStatus,
                    rolloutPercent = rollout,
                    minAppVersion = minVer,
                    maxAppVersion = maxVer,
                    allowedRoles = jsonArrayToStringList(rawValue.optJSONArray("allowed_roles")),
                    allowedDistricts = jsonArrayToStringList(rawValue.optJSONArray("allowed_districts")),
                    betaUsers = jsonArrayToStringList(rawValue.optJSONArray("beta_users")),
                    killSwitch = killed
                )
            }
            else -> {
                val defaultStatus = if (DEFAULT_ON_FEATURES.contains(key)) FeatureStatus.ON else FeatureStatus.OFF
                EvaluatedFeatureRule(key = key, status = defaultStatus)
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun isEnabled(featureKey: String, context: Context? = null): Boolean {
        if (context != null && !isInitialized) {
            initFromLocalCache(context)
        }

        val rule = getEvaluatedFeatureRule(featureKey)
        if (rule.killSwitch || rule.status == FeatureStatus.OFF || rule.status == FeatureStatus.COMING_SOON) {
            return false
        }

        if (!rule.minAppVersion.isNullOrBlank() && compareVersions(CURRENT_APP_VERSION, rule.minAppVersion) < 0) {
            return false
        }
        if (!rule.maxAppVersion.isNullOrBlank() && compareVersions(CURRENT_APP_VERSION, rule.maxAppVersion) > 0) {
            return false
        }

        if (context != null) {
            val uid = SessionManager.getUserUid(context) ?: ""
            val activeRole = SessionManager.getActiveMode(context).lowercase()
            val userDistrict = SessionManager.getUserDistrict(context).lowercase()

            if (rule.allowedRoles.isNotEmpty()) {
                val roleAllowed = rule.allowedRoles.any {
                    it.lowercase() == activeRole || it.lowercase() == "both" || it.lowercase() == "all"
                }
                if (!roleAllowed) return false
            }

            if (rule.allowedDistricts.isNotEmpty() && userDistrict.isNotBlank()) {
                val distAllowed = rule.allowedDistricts.any {
                    it.lowercase() == userDistrict || it.lowercase() == "all"
                }
                if (!distAllowed) return false
            }

            if (rule.status == FeatureStatus.BETA && rule.betaUsers.isNotEmpty()) {
                if (uid.isBlank() || !rule.betaUsers.contains(uid)) {
                    return false
                }
            }

            if (rule.rolloutPercent in 0..99) {
                if (rule.rolloutPercent <= 0) return false
                val seed = if (uid.isNotBlank()) "${rule.key}_$uid" else "${rule.key}_device"
                val bucket = abs(seed.hashCode()) % 100
                if (bucket >= rule.rolloutPercent) return false
            }
        } else {
            if (rule.rolloutPercent <= 0) return false
        }

        return true
    }

    @JvmStatic
    fun isEnabled(appContext: Context?, flagKey: String): Boolean =
        isEnabled(flagKey, appContext)

    @JvmStatic
    @JvmOverloads
    fun isFeatureEnabled(featureKey: String, context: Context? = null): Boolean =
        isEnabled(featureKey, context)

    @JvmStatic
    fun isFeatureEnabled(appContext: Context?, flagKey: String): Boolean =
        isEnabled(flagKey, appContext)

    fun isComingSoon(featureKey: String): Boolean =
        getEvaluatedFeatureRule(featureKey).status == FeatureStatus.COMING_SOON

    fun isBeta(featureKey: String): Boolean =
        getEvaluatedFeatureRule(featureKey).status == FeatureStatus.BETA

    fun getAllRegisteredFeatureKeys(): Set<String> =
        DEFAULT_ON_FEATURES + DEFAULT_OFF_HOLD_FEATURES

    // ========================================================================
    // CORE REMOTE PARAMETERS
    // ========================================================================

    fun getTokenPercent(): Int =
        memoryParams.optInt("token_percent", 10).coerceIn(0, 100)

    fun getCommissionPercent(): Int =
        memoryParams.optInt("commission_percent", 2).coerceIn(0, 25)

    fun getMinOrderRupees(): Int =
        memoryParams.optInt("min_order_rupees", 50).coerceAtLeast(1)

    fun getMaxOrderRupees(): Int =
        memoryParams.optInt("max_order_rupees", 500000).coerceAtLeast(100)

    fun getBoostPriceRupees(): Int =
        memoryParams.optInt("boost_price_rupees", 99).coerceIn(1, 10000)

    fun getBoostDurationDays(): Int =
        memoryParams.optInt("boost_duration_days", 3).coerceIn(1, 30)

    fun getBoostMaxSlots(): Int =
        memoryParams.optInt("boost_max_slots", 4).coerceIn(1, 20)

    fun getMaxBuyerRequestsPerDay(): Int =
        memoryParams.optInt("max_requests_per_day", 10).coerceIn(1, 50)

    fun getDefaultSearchRadiusKm(): Int =
        memoryParams.optInt("default_search_radius_km", 25).coerceIn(1, 500)

    fun getYoutubeDailyQuotaLimit(): Int =
        memoryParams.optInt("youtube_daily_quota_limit", 6).coerceIn(1, 50)

    // ========================================================================
    // ADMOB & MONETIZATION CONTROLS (Default OFF)
    // ========================================================================

    data class AdMobRemoteConfig(
        val enabled: Boolean,
        val appId: String,
        val bannerUnitId: String,
        val interstitialUnitId: String,
        val maxAdsPerUserPerDay: Int,
        val excludePremiumFarmers: Boolean,
        val emergencyKillSwitch: Boolean
    )

    fun getAdMobConfig(context: Context? = null): AdMobRemoteConfig {
        val flagEnabled = isEnabled("admob", context)
        val killSwitch = memoryParams.optBoolean("admob_kill_switch", false)
        val serviceConfigured = memoryServicesStatus.optJSONObject("admob")?.optBoolean("enabled", false) ?: false

        return AdMobRemoteConfig(
            enabled = flagEnabled && serviceConfigured && !killSwitch,
            appId = memoryParams.optString("admob_app_id", ""),
            bannerUnitId = memoryParams.optString("admob_banner_unit_id", ""),
            interstitialUnitId = memoryParams.optString("admob_interstitial_unit_id", ""),
            maxAdsPerUserPerDay = memoryParams.optInt("admob_daily_limit", 3).coerceIn(0, 20),
            excludePremiumFarmers = memoryParams.optBoolean("admob_exclude_premium", true),
            emergencyKillSwitch = killSwitch
        )
    }

    // ========================================================================
    // FUTURE BLANK SETTINGS INFRASTRUCTURE
    // ========================================================================

    data class AuctionSettings(
        val enabled: Boolean,
        val minimumBidRupees: Int,
        val bidIncrementRupees: Int,
        val durationHours: Int,
        val reservePriceEnabled: Boolean,
        val autoExtendEnabled: Boolean,
        val commissionPercent: Int
    )

    fun getAuctionSettings(context: Context? = null): AuctionSettings = AuctionSettings(
        enabled = isEnabled("auction", context),
        minimumBidRupees = memoryParams.optInt("auction_min_bid_rupees", 100),
        bidIncrementRupees = memoryParams.optInt("auction_bid_increment_rupees", 10),
        durationHours = memoryParams.optInt("auction_duration_hours", 24),
        reservePriceEnabled = memoryParams.optBoolean("auction_reserve_price_enabled", true),
        autoExtendEnabled = memoryParams.optBoolean("auction_auto_extend", false),
        commissionPercent = memoryParams.optInt("auction_commission_percent", 2)
    )

    data class WalletSettings(
        val enabled: Boolean,
        val minimumBalanceRupees: Int,
        val withdrawalMinimumRupees: Int,
        val commissionPercent: Int,
        val kycRequired: Boolean,
        val dailyLimitRupees: Int
    )

    fun getWalletSettings(context: Context? = null): WalletSettings = WalletSettings(
        enabled = isEnabled("wallet", context),
        minimumBalanceRupees = memoryParams.optInt("wallet_min_balance_rupees", 0),
        withdrawalMinimumRupees = memoryParams.optInt("wallet_withdrawal_min_rupees", 100),
        commissionPercent = memoryParams.optInt("wallet_commission_percent", 0),
        kycRequired = memoryParams.optBoolean("wallet_kyc_required", true),
        dailyLimitRupees = memoryParams.optInt("wallet_daily_limit_rupees", 25000)
    )

    data class SubscriptionSettings(
        val enabled: Boolean,
        val planName: String,
        val proPriceRupees: Int,
        val premiumPriceRupees: Int,
        val durationDays: Int,
        val boostCredits: Int,
        val featuredSlots: Int,
        val commissionDiscountPercent: Int
    )

    fun getSubscriptionSettings(context: Context? = null): SubscriptionSettings = SubscriptionSettings(
        enabled = isEnabled("subscription", context),
        planName = memoryParams.optString("subscription_plan_name", "Mera Kisan Pro"),
        proPriceRupees = memoryParams.optInt("subscription_pro_price_rupees", 199),
        premiumPriceRupees = memoryParams.optInt("subscription_premium_price_rupees", 499),
        durationDays = memoryParams.optInt("subscription_duration_days", 30),
        boostCredits = memoryParams.optInt("subscription_boost_credits", 2),
        featuredSlots = memoryParams.optInt("subscription_featured_slots", 1),
        commissionDiscountPercent = memoryParams.optInt("subscription_commission_discount", 1)
    )

    data class ReferralSettings(
        val enabled: Boolean,
        val farmerRewardRupees: Int,
        val buyerRewardRupees: Int,
        val minimumOrderRupees: Int,
        val rewardLimitPerUser: Int,
        val expiryDays: Int
    )

    fun getReferralSettings(context: Context? = null): ReferralSettings = ReferralSettings(
        enabled = isEnabled("referral", context),
        farmerRewardRupees = memoryParams.optInt("referral_farmer_reward_rupees", 50),
        buyerRewardRupees = memoryParams.optInt("referral_buyer_reward_rupees", 50),
        minimumOrderRupees = memoryParams.optInt("referral_min_order_rupees", 500),
        rewardLimitPerUser = memoryParams.optInt("referral_reward_limit", 10),
        expiryDays = memoryParams.optInt("referral_expiry_days", 30)
    )

    data class CouponSettings(
        val enabled: Boolean,
        val maxDiscountRupees: Int,
        val minimumOrderRupees: Int,
        val usageLimitPerUser: Int
    )

    fun getCouponSettings(context: Context? = null): CouponSettings = CouponSettings(
        enabled = isEnabled("coupon", context),
        maxDiscountRupees = memoryParams.optInt("coupon_max_discount_rupees", 100),
        minimumOrderRupees = memoryParams.optInt("coupon_min_order_rupees", 500),
        usageLimitPerUser = memoryParams.optInt("coupon_usage_limit", 1)
    )

    data class DeliverySettings(
        val enabled: Boolean,
        val farmerDeliveryEnabled: Boolean,
        val buyerPickupEnabled: Boolean,
        val partnerDeliveryEnabled: Boolean,
        val cityPickupPointsEnabled: Boolean,
        val baseFeeRupees: Int,
        val perKmFeeRupees: Int,
        val maxRadiusKm: Int,
        val freeDeliveryAboveRupees: Int
    )

    fun getDeliverySettings(context: Context? = null): DeliverySettings = DeliverySettings(
        enabled = isEnabled("delivery", context),
        farmerDeliveryEnabled = memoryParams.optBoolean("delivery_farmer_enabled", true),
        buyerPickupEnabled = memoryParams.optBoolean("delivery_buyer_pickup_enabled", true),
        partnerDeliveryEnabled = memoryParams.optBoolean("delivery_partner_enabled", false),
        cityPickupPointsEnabled = isEnabled("pickup_points", context),
        baseFeeRupees = memoryParams.optInt("delivery_base_fee_rupees", 30),
        perKmFeeRupees = memoryParams.optInt("delivery_per_km_rupees", 5),
        maxRadiusKm = memoryParams.optInt("delivery_max_radius_km", 50),
        freeDeliveryAboveRupees = memoryParams.optInt("delivery_free_above_rupees", 1000)
    )

    data class FarmBoxAndGroupBuySettings(
        val farmBoxEnabled: Boolean,
        val groupBuyingEnabled: Boolean,
        val preOrderEnabled: Boolean,
        val oilMarketplaceEnabled: Boolean,
        val batchTraceabilityEnabled: Boolean,
        val maxFarmersPerBox: Int,
        val defaultBoxPriceRupees: Int,
        val minGroupBuyers: Int,
        val maxPreOrderDays: Int
    )

    fun getFarmBoxAndGroupBuySettings(context: Context? = null): FarmBoxAndGroupBuySettings =
        FarmBoxAndGroupBuySettings(
            farmBoxEnabled = isEnabled("farm_box", context),
            groupBuyingEnabled = isEnabled("group_buying", context),
            preOrderEnabled = isEnabled("pre_order", context),
            oilMarketplaceEnabled = isEnabled("oil_marketplace", context),
            batchTraceabilityEnabled = isEnabled("traceability", context),
            maxFarmersPerBox = memoryParams.optInt("farm_box_max_farmers", 5),
            defaultBoxPriceRupees = memoryParams.optInt("farm_box_price_rupees", 399),
            minGroupBuyers = memoryParams.optInt("group_buying_min_buyers", 10),
            maxPreOrderDays = memoryParams.optInt("pre_order_max_days", 30)
        )

    fun isMaintenanceMode(): Boolean =
        memoryParams.optString("maintenance_mode", "OFF").uppercase() == "ON" ||
            isEnabled("maintenance", null)

    fun getMaintenanceMessageHi(): String {
        val custom = memoryParams.optString("maintenance_message_hi", "").trim()
        return if (custom.isNotEmpty()) {
            custom
        } else {
            "मेरा किसान पर अभी रखरखाव (Maintenance) चल रहा है। कृपया कुछ समय बाद प्रयास करें।"
        }
    }

    fun isForceUpdateRequired(): Boolean {
        val forceFlag = memoryParams.optString("force_update", "OFF").uppercase() == "ON"
        val minVer = memoryParams.optString("min_app_version", "1.0.0").trim()
        val isBelowMin = compareVersions(CURRENT_APP_VERSION, minVer) < 0
        return isBelowMin || (forceFlag && compareVersions(CURRENT_APP_VERSION, getLatestAppVersion()) < 0)
    }

    fun getMinRequiredAppVersion(): String =
        memoryParams.optString("min_app_version", "1.0.0").trim()

    fun getLatestAppVersion(): String =
        memoryParams.optString("latest_app_version", CURRENT_APP_VERSION).trim()

    fun getHomeBannerText(): String {
        val remote = memoryParams.optString("home_banner_hi", "").trim()
        return if (remote.isNotEmpty()) {
            remote
        } else {
            "किसान से सीधे आपकी जरूरत तक — ताज़ा व देशी कृषि उत्पाद"
        }
    }

    fun getAnnouncementText(): String =
        memoryParams.optString("announcement_hi", "").trim()

    fun getSupportWhatsApp(): String =
        memoryParams.optString("support_whatsapp", "").trim()

    fun getSupportPhone(): String =
        memoryParams.optString("support_phone", "").trim()

    fun isExternalServiceReady(serviceKey: String): Boolean {
        val item = memoryServicesStatus.optJSONObject(serviceKey.trim().lowercase()) ?: return false
        return item.optBoolean("enabled", false) && item.optBoolean("configured", false)
    }

    private fun jsonArrayToStringList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val v = arr.optString(i, "").trim()
            if (v.isNotEmpty()) list.add(v)
        }
        return list
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }
}
