// app/src/main/java/in/merakisan/app/core/security/SessionManager.kt
package `in`.merakisan.app.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * MERA KISAN — ENCRYPTED SESSION & ROLE STATE MANAGER
 * Eliminates Platform declaration clash between instance and companion methods.
 */
class SessionManager(private val context: Context) {

    data class UserSession(
        val uid: String = "",
        val name: String = "",
        val phone: String = "",
        val email: String = "",
        val role: String = "farmer",
        val activeRole: String = "farmer",
        val activeMode: String = "FARMER",
        val roles: List<String> = listOf("farmer", "buyer"),
        val village: String = "",
        val district: String = "",
        val state: String = "",
        val pincode: String = "",
        val token: String = "",
        val idToken: String = "",
        val authToken: String = "",
        val fcmToken: String = "",
        val isVerified: Boolean = false,
        val organicVerified: Boolean = false,
        val verificationStatus: String = "farmer_declared",
        val isLoggedIn: Boolean = false,
        val isAuthenticated: Boolean = false,
        val isOnboarded: Boolean = false,
        val farmStory: String = "",
        val farmingMethod: String = ""
    ) {
        val userId: String get() = uid
        val id: String get() = uid
        val userName: String get() = name
        val mobile: String get() = phone
        val userPhone: String get() = phone
        val location: String
            get() = if (village.isNotBlank() && district.isNotBlank()) "$village, $district" else district.ifBlank { village }

        operator fun Boolean.invoke(): Boolean = this

        fun isFarmer(): Boolean =
            activeMode.equals("FARMER", ignoreCase = true) || role.equals("farmer", ignoreCase = true)

        fun isBuyer(): Boolean =
            activeMode.equals("BUYER", ignoreCase = true) || role.equals("buyer", ignoreCase = true)
    }

    fun getUserSession(): UserSession = Companion.getUserSession(context)

    fun saveSession(session: UserSession) = Companion.saveSession(context, session)

    @JvmOverloads
    fun saveSession(
        uid: String = getUserUid() ?: "",
        name: String = getUserName(),
        phone: String = getSelfDeclaredPhone(),
        email: String? = getUserEmail(),
        role: String = getActiveRole(),
        activeRole: String? = null,
        activeMode: String? = null,
        roles: List<String>? = null,
        village: String? = getUserVillage(),
        district: String? = getUserDistrict(),
        state: String? = getUserState(),
        pincode: String? = null,
        token: String? = null,
        idToken: String? = null,
        isVerified: Boolean = false,
        organicVerified: Boolean = false,
        verificationStatus: String = getVerificationStatus()
    ) {
        val resolvedMode = (activeMode ?: activeRole ?: role).let {
            if (it.equals("buyer", ignoreCase = true)) "BUYER" else "FARMER"
        }
        val resolvedRoles = roles ?: if (role.equals("both", ignoreCase = true)) {
            listOf("farmer", "buyer")
        } else {
            listOf(role.lowercase())
        }
        val finalToken = idToken ?: token
        if (!finalToken.isNullOrBlank()) {
            saveAuthToken(finalToken)
        }
        Companion.saveOnboardedUserSession(
            context = context,
            uid = uid,
            name = name,
            phone = phone,
            roles = resolvedRoles,
            activeMode = resolvedMode,
            village = village,
            district = district,
            state = state,
            verificationStatus = if (organicVerified) "organic_certified" else if (isVerified) "verified_farmer" else verificationStatus,
            email = email,
            pincode = pincode,
            idToken = finalToken
        )
    }

    @JvmOverloads
    fun saveOnboardedUserSession(
        uid: String,
        name: String,
        phone: String,
        roles: List<String> = listOf("farmer", "buyer"),
        activeMode: String = "FARMER",
        village: String? = null,
        district: String? = null,
        state: String? = null,
        verificationStatus: String = "farmer_declared",
        email: String? = null,
        pincode: String? = null,
        idToken: String? = null,
        role: String? = null,
        isVerified: Boolean = false
    ) = Companion.saveOnboardedUserSession(
        context = context,
        uid = uid,
        name = name,
        phone = phone,
        roles = if (!role.isNullOrBlank() && roles.isEmpty()) listOf(role.lowercase()) else roles,
        activeMode = activeMode.ifBlank { role ?: "FARMER" },
        village = village,
        district = district,
        state = state,
        verificationStatus = if (isVerified && verificationStatus == "farmer_declared") "verified_farmer" else verificationStatus,
        email = email,
        pincode = pincode,
        idToken = idToken
    )

    @JvmOverloads
    fun saveUserSession(
        uid: String = "",
        name: String = "",
        phone: String = "",
        role: String = "farmer",
        district: String? = null,
        village: String? = null,
        state: String? = null,
        email: String? = null,
        pincode: String? = null,
        idToken: String? = null,
        token: String? = null,
        isVerified: Boolean = false
    ) = Companion.saveOnboardedUserSession(
        context = context,
        uid = uid,
        name = name,
        phone = phone,
        roles = if (role.equals("both", ignoreCase = true)) listOf("farmer", "buyer") else listOf(role.lowercase()),
        activeMode = if (role.equals("buyer", ignoreCase = true)) "BUYER" else "FARMER",
        village = village,
        district = district,
        state = state,
        verificationStatus = if (isVerified) "verified_farmer" else "farmer_declared",
        email = email,
        pincode = pincode,
        idToken = idToken ?: token
    )

    fun saveAuthToken(token: String) = Companion.saveAuthToken(context, token)
    fun getAuthToken(): String? = Companion.getAuthToken(context)

    fun saveFcmToken(token: String) = Companion.saveFcmToken(context, token)
    fun getFcmToken(): String? = Companion.getFcmToken(context)

    // Instance method (0 parameters)
    fun consumeLastCrashSummary(): String? = Companion.consumeLastCrashSummary(context)
    fun saveCrashSummary(summary: String) = Companion.saveCrashSummary(context, summary)

    fun isLoggedInAndOnboarded(): Boolean = Companion.isLoggedInAndOnboarded(context)
    fun isLoggedIn(): Boolean = Companion.isLoggedInAndOnboarded(context)
    fun isAuthenticated(): Boolean = Companion.isAuthenticated(context)

    fun getUserUid(): String? = Companion.getUserUid(context)
    fun getUid(): String? = Companion.getUserUid(context)
    fun getUserId(): String? = Companion.getUserUid(context)
    fun saveUserId(uid: String) = Companion.saveUserId(context, uid)

    fun getUserName(): String = Companion.getUserName(context)
    fun saveUserName(name: String) = Companion.saveUserName(context, name)

    fun getSelfDeclaredPhone(): String = Companion.getSelfDeclaredPhone(context)
    fun getUserPhone(): String = Companion.getSelfDeclaredPhone(context)
    fun getPhone(): String = Companion.getSelfDeclaredPhone(context)
    fun saveUserPhone(phone: String) = Companion.saveUserPhone(context, phone)

    fun getUserEmail(): String = Companion.getUserEmail(context)
    fun saveUserEmail(email: String) = Companion.saveUserEmail(context, email)

    fun getUserVillage(): String = Companion.getUserVillage(context)
    fun getVillage(): String = Companion.getUserVillage(context)

    fun getUserDistrict(): String = Companion.getUserDistrict(context)
    fun getDistrict(): String = Companion.getUserDistrict(context)
    fun saveUserDistrict(district: String) = Companion.saveUserDistrict(context, district)

    fun getUserState(): String = Companion.getUserState(context)
    fun getState(): String = Companion.getUserState(context)

    fun getVerificationStatus(): String = Companion.getVerificationStatus(context)
    fun getAllowedRoles(): List<String> = Companion.getAllowedRoles(context)

    fun getActiveMode(): String = Companion.getActiveMode(context)
    fun getActiveRole(): String = Companion.getActiveMode(context).lowercase()
    fun getUserRole(): String = Companion.getActiveMode(context).lowercase()

    fun setActiveMode(mode: String) = Companion.setActiveMode(context, mode)
    fun setActiveRole(role: String) = Companion.setActiveMode(context, role)
    fun saveUserRole(role: String) = Companion.setActiveMode(context, role)

    fun isFarmerModeActive(): Boolean = Companion.isFarmerModeActive(context)
    fun isBuyerModeActive(): Boolean = Companion.isBuyerModeActive(context)

    fun clearSession() = Companion.clearAllSession(context)
    fun clearAllSession() = Companion.clearAllSession(context)
    fun logout() = Companion.clearAllSession(context)

    companion object {
        private const val ENCRYPTED_PREFS_FILE = "mera_kisan_secure_session_v2"
        private const val FALLBACK_PREFS_FILE = "mera_kisan_session_fallback_v2"

        private const val KEY_UID = "user_uid"
        private const val KEY_AUTH_TOKEN = "auth_id_token"
        private const val KEY_FCM_TOKEN = "fcm_push_token"
        private const val KEY_NAME = "user_name"
        private const val KEY_PHONE = "self_declared_phone"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_PINCODE = "user_pincode"
        private const val KEY_ROLES_CSV = "user_roles_csv"
        private const val KEY_ACTIVE_MODE = "active_role_mode"
        private const val KEY_ONBOARDING_DONE = "onboarding_completed"
        private const val KEY_VILLAGE = "user_village"
        private const val KEY_DISTRICT = "user_district"
        private const val KEY_STATE = "user_state"
        private const val KEY_VERIFICATION_STATUS = "verification_status"
        private const val KEY_LAST_CRASH_SUMMARY = "last_crash_summary"

        private const val KEY_DEALER_TOKEN = "agri_dealer_session_token"
        private const val KEY_DEALER_SHOP_NAME = "agri_dealer_shop_name"
        private const val KEY_DEALER_LOGIN_ID = "agri_dealer_login_id"

        private const val KEY_FREQUENT_MANDI_STATE = "frequent_mandi_state"
        private const val KEY_FREQUENT_MANDI_DISTRICT = "frequent_mandi_district"
        private const val KEY_FREQUENT_MANDI_CACHE_JSON = "frequent_mandi_rates_json"
        private const val KEY_FREQUENT_MANDI_CACHE_TIME = "frequent_mandi_rates_time"

        @Volatile
        private var cachedPrefs: SharedPreferences? = null

        @Volatile
        private var appContextHolder: Context? = null

        @JvmStatic
        fun getInstance(context: Context): SessionManager {
            appContextHolder = context.applicationContext
            return SessionManager(context.applicationContext)
        }

        private fun getSecurePrefs(context: Context): SharedPreferences {
            if (appContextHolder == null) {
                appContextHolder = context.applicationContext
            }
            cachedPrefs?.let { return it }
            synchronized(this) {
                cachedPrefs?.let { return it }
                val appCtx = context.applicationContext
                val prefs = try {
                    val masterKey = MasterKey.Builder(appCtx)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()
                    EncryptedSharedPreferences.create(
                        appCtx,
                        ENCRYPTED_PREFS_FILE,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                } catch (_: Exception) {
                    appCtx.getSharedPreferences(FALLBACK_PREFS_FILE, Context.MODE_PRIVATE)
                }
                cachedPrefs = prefs
                return prefs
            }
        }

        @JvmStatic
        fun getUserSession(context: Context): UserSession {
            val prefs = getSecurePrefs(context)
            val uid = prefs.getString(KEY_UID, "") ?: ""
            val name = prefs.getString(KEY_NAME, "") ?: ""
            val phone = prefs.getString(KEY_PHONE, "") ?: ""
            val email = prefs.getString(KEY_EMAIL, "") ?: ""
            val pincode = prefs.getString(KEY_PINCODE, "") ?: ""
            val activeMode = getActiveMode(context)
            val activeRole = activeMode.lowercase()
            val rolesList = getAllowedRoles(context).ifEmpty { listOf("farmer", "buyer") }
            val village = prefs.getString(KEY_VILLAGE, "") ?: ""
            val district = prefs.getString(KEY_DISTRICT, "") ?: ""
            val state = prefs.getString(KEY_STATE, "") ?: ""
            val token = prefs.getString(KEY_AUTH_TOKEN, "") ?: ""
            val fcm = prefs.getString(KEY_FCM_TOKEN, "") ?: ""
            val verif = prefs.getString(KEY_VERIFICATION_STATUS, "farmer_declared") ?: "farmer_declared"
            val onboarded = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
            val authenticated = uid.isNotBlank() || token.isNotBlank() || phone.isNotBlank()

            return UserSession(
                uid = uid,
                name = name,
                phone = phone,
                email = email,
                role = activeRole,
                activeRole = activeRole,
                activeMode = activeMode,
                roles = rolesList,
                village = village,
                district = district,
                state = state,
                pincode = pincode,
                token = token,
                idToken = token,
                authToken = token,
                fcmToken = fcm,
                isVerified = verif != "unverified" && verif != "farmer_declared",
                organicVerified = verif == "organic_certified",
                verificationStatus = verif,
                isLoggedIn = authenticated,
                isAuthenticated = authenticated,
                isOnboarded = onboarded
            )
        }

        @JvmStatic
        fun saveSession(context: Context, session: UserSession) {
            if (session.idToken.isNotBlank()) {
                saveAuthToken(context, session.idToken)
            } else if (session.token.isNotBlank()) {
                saveAuthToken(context, session.token)
            }
            if (session.fcmToken.isNotBlank()) {
                saveFcmToken(context, session.fcmToken)
            }
            val resolvedMode = when {
                session.activeMode.equals("BUYER", ignoreCase = true) -> "BUYER"
                session.activeRole.equals("buyer", ignoreCase = true) -> "BUYER"
                session.role.equals("buyer", ignoreCase = true) -> "BUYER"
                else -> "FARMER"
            }
            saveOnboardedUserSession(
                context = context,
                uid = session.uid,
                name = session.name,
                phone = session.phone,
                roles = session.roles.ifEmpty { listOf(session.role.lowercase()) },
                activeMode = resolvedMode,
                village = session.village,
                district = session.district,
                state = session.state,
                verificationStatus = session.verificationStatus,
                email = session.email,
                pincode = session.pincode,
                idToken = session.idToken.ifBlank { session.token }
            )
        }

        @JvmStatic
        fun saveAuthToken(context: Context, token: String) {
            if (token.isBlank()) return
            getSecurePrefs(context).edit().putString(KEY_AUTH_TOKEN, token.trim()).apply()
        }

        @JvmStatic
        fun getAuthToken(context: Context): String? =
            getSecurePrefs(context).getString(KEY_AUTH_TOKEN, null)

        @JvmStatic
        fun saveFcmToken(context: Context, token: String) {
            if (token.isBlank()) return
            getSecurePrefs(context).edit().putString(KEY_FCM_TOKEN, token.trim()).apply()
        }

        @JvmStatic
        fun getFcmToken(context: Context): String? =
            getSecurePrefs(context).getString(KEY_FCM_TOKEN, null)

        // Static companion method (1 parameter: Context).
        // No @JvmOverloads or default value, strictly avoiding JVM signature clash with instance method.
        @JvmStatic
        fun consumeLastCrashSummary(context: Context): String? {
            val prefs = getSecurePrefs(context)
            val summary = prefs.getString(KEY_LAST_CRASH_SUMMARY, null)
            if (!summary.isNullOrBlank()) {
                prefs.edit().remove(KEY_LAST_CRASH_SUMMARY).apply()
            }
            return summary
        }

        @JvmStatic
        fun saveCrashSummary(context: Context, summary: String) {
            if (summary.isBlank()) return
            getSecurePrefs(context).edit().putString(KEY_LAST_CRASH_SUMMARY, summary.trim()).apply()
        }

        @JvmStatic
        fun saveUserId(context: Context, uid: String) {
            getSecurePrefs(context).edit().putString(KEY_UID, uid.trim()).apply()
        }

        @JvmStatic
        fun saveUserName(context: Context, name: String) {
            getSecurePrefs(context).edit().putString(KEY_NAME, name.trim()).apply()
        }

        @JvmStatic
        fun saveUserPhone(context: Context, phone: String) {
            getSecurePrefs(context).edit().putString(KEY_PHONE, phone.trim()).apply()
        }

        @JvmStatic
        fun getUserEmail(context: Context): String =
            getSecurePrefs(context).getString(KEY_EMAIL, "") ?: ""

        @JvmStatic
        fun saveUserEmail(context: Context, email: String) {
            getSecurePrefs(context).edit().putString(KEY_EMAIL, email.trim()).apply()
        }

        @JvmStatic
        fun saveUserDistrict(context: Context, district: String) {
            getSecurePrefs(context).edit().putString(KEY_DISTRICT, district.trim()).apply()
        }

        @JvmStatic
        @JvmOverloads
        fun saveOnboardedUserSession(
            context: Context,
            uid: String,
            name: String,
            phone: String,
            roles: List<String> = listOf("farmer", "buyer"),
            activeMode: String = "FARMER",
            village: String? = null,
            district: String? = null,
            state: String? = null,
            verificationStatus: String = "farmer_declared",
            email: String? = null,
            pincode: String? = null,
            idToken: String? = null
        ) {
            val cleanRoles = roles.map { it.trim().lowercase() }.filter { it in listOf("farmer", "buyer") }.distinct()
            val resolvedMode = when (activeMode.trim().uppercase()) {
                "BUYER" -> "BUYER"
                "FARMER", "SELLER" -> "FARMER"
                else -> if (cleanRoles.contains("farmer")) "FARMER" else "BUYER"
            }

            getSecurePrefs(context).edit().apply {
                putString(KEY_UID, uid.trim())
                putString(KEY_NAME, name.trim())
                putString(KEY_PHONE, phone.trim())
                putString(KEY_ROLES_CSV, cleanRoles.joinToString(","))
                putString(KEY_ACTIVE_MODE, resolvedMode)
                putBoolean(KEY_ONBOARDING_DONE, true)
                if (!village.isNullOrBlank()) putString(KEY_VILLAGE, village.trim())
                if (!district.isNullOrBlank()) putString(KEY_DISTRICT, district.trim())
                if (!state.isNullOrBlank()) putString(KEY_STATE, state.trim())
                if (!email.isNullOrBlank()) putString(KEY_EMAIL, email.trim())
                if (!pincode.isNullOrBlank()) putString(KEY_PINCODE, pincode.trim())
                if (!idToken.isNullOrBlank()) putString(KEY_AUTH_TOKEN, idToken.trim())
                putString(KEY_VERIFICATION_STATUS, verificationStatus.trim().lowercase())
                apply()
            }
        }

        @JvmStatic
        fun isLoggedInAndOnboarded(context: Context): Boolean {
            val prefs = getSecurePrefs(context)
            val uid = prefs.getString(KEY_UID, null)
            val phone = prefs.getString(KEY_PHONE, null)
            val onboarded = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
            return !uid.isNullOrBlank() && !phone.isNullOrBlank() && onboarded
        }

        @JvmStatic
        fun isAuthenticated(context: Context): Boolean {
            val prefs = getSecurePrefs(context)
            val uid = prefs.getString(KEY_UID, null)
            val token = prefs.getString(KEY_AUTH_TOKEN, null)
            val phone = prefs.getString(KEY_PHONE, null)
            return !uid.isNullOrBlank() || !token.isNullOrBlank() || !phone.isNullOrBlank()
        }

        @JvmStatic
        fun getUserUid(context: Context): String? =
            getSecurePrefs(context).getString(KEY_UID, null)

        @JvmStatic
        fun getUserName(context: Context): String =
            getSecurePrefs(context).getString(KEY_NAME, "") ?: ""

        @JvmStatic
        fun getSelfDeclaredPhone(context: Context): String =
            getSecurePrefs(context).getString(KEY_PHONE, "") ?: ""

        @JvmStatic
        fun getUserVillage(context: Context): String =
            getSecurePrefs(context).getString(KEY_VILLAGE, "") ?: ""

        @JvmStatic
        fun getUserDistrict(context: Context): String =
            getSecurePrefs(context).getString(KEY_DISTRICT, "") ?: ""

        @JvmStatic
        fun getUserState(context: Context): String =
            getSecurePrefs(context).getString(KEY_STATE, "") ?: ""

        @JvmStatic
        fun getVerificationStatus(context: Context): String =
            getSecurePrefs(context).getString(KEY_VERIFICATION_STATUS, "farmer_declared") ?: "farmer_declared"

        @JvmStatic
        fun getAllowedRoles(context: Context): List<String> {
            val csv = getSecurePrefs(context).getString(KEY_ROLES_CSV, "") ?: ""
            return csv.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        }

        @JvmStatic
        fun getActiveMode(context: Context): String {
            val saved = getSecurePrefs(context).getString(KEY_ACTIVE_MODE, "FARMER") ?: "FARMER"
            return if (saved.uppercase() == "BUYER") "BUYER" else "FARMER"
        }

        @JvmStatic
        fun isFarmerModeActive(context: Context): Boolean =
            getActiveMode(context) == "FARMER"

        @JvmStatic
        fun isBuyerModeActive(context: Context): Boolean =
            getActiveMode(context) == "BUYER"

        @JvmStatic
        fun setActiveMode(context: Context, mode: String) {
            val resolved = if (mode.trim().uppercase() == "BUYER") "BUYER" else "FARMER"
            val currentRoles = getAllowedRoles(context).toMutableSet()
            currentRoles.add(resolved.lowercase())
            getSecurePrefs(context).edit()
                .putString(KEY_ACTIVE_MODE, resolved)
                .putString(KEY_ROLES_CSV, currentRoles.joinToString(","))
                .apply()
        }

        @JvmStatic
        fun saveDealerSession(context: Context, token: String, loginId: String, shopName: String) {
            getSecurePrefs(context).edit()
                .putString(KEY_DEALER_TOKEN, token.trim())
                .putString(KEY_DEALER_LOGIN_ID, loginId.trim())
                .putString(KEY_DEALER_SHOP_NAME, shopName.trim())
                .apply()
        }

        @JvmStatic
        fun getDealerToken(context: Context): String? =
            getSecurePrefs(context).getString(KEY_DEALER_TOKEN, null)

        @JvmStatic
        fun getDealerShopName(context: Context): String =
            getSecurePrefs(context).getString(KEY_DEALER_SHOP_NAME, "") ?: ""

        @JvmStatic
        fun clearDealerSession(context: Context) {
            getSecurePrefs(context).edit()
                .remove(KEY_DEALER_TOKEN)
                .remove(KEY_DEALER_LOGIN_ID)
                .remove(KEY_DEALER_SHOP_NAME)
                .apply()
        }

        @JvmStatic
        fun saveFrequentMandiCache(
            context: Context,
            state: String,
            district: String,
            ratesArray: JSONArray
        ) {
            if (ratesArray.length() == 0) return
            getSecurePrefs(context).edit()
                .putString(KEY_FREQUENT_MANDI_STATE, state)
                .putString(KEY_FREQUENT_MANDI_DISTRICT, district)
                .putString(KEY_FREQUENT_MANDI_CACHE_JSON, ratesArray.toString())
                .putLong(KEY_FREQUENT_MANDI_CACHE_TIME, System.currentTimeMillis())
                .apply()
        }

        @JvmStatic
        fun getFrequentMandiCache(context: Context): JSONObject? {
            val prefs = getSecurePrefs(context)
            val rawJson = prefs.getString(KEY_FREQUENT_MANDI_CACHE_JSON, null) ?: return null
            return try {
                JSONObject().apply {
                    put("state", prefs.getString(KEY_FREQUENT_MANDI_STATE, "") ?: "")
                    put("district", prefs.getString(KEY_FREQUENT_MANDI_DISTRICT, "") ?: "")
                    put("cached_at_ms", prefs.getLong(KEY_FREQUENT_MANDI_CACHE_TIME, 0L))
                    put("rates", JSONArray(rawJson))
                }
            } catch (_: Exception) {
                null
            }
        }

        @JvmStatic
        fun clearAllSession(context: Context) {
            getSecurePrefs(context).edit().clear().apply()
        }
    }
}
