// app/src/main/java/in/merakisan/app/core/security/SessionManager.kt
package `in`.merakisan.app.core.security

import android.content.Context
import android.content.SharedPreferences

data class UserSession(
    val uid: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val role: String = "buyer",
    val village: String = "",
    val district: String = "",
    val state: String = "Madhya Pradesh",
    val verificationStatus: String = "unverified"
)

class SessionManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "mera_kisan_prefs"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_VILLAGE = "user_village"
        private const val KEY_USER_DISTRICT = "user_district"
        private const val KEY_USER_STATE = "user_state"
        private const val KEY_USER_VERIFICATION = "user_verification"
        private const val KEY_FCM_TOKEN = "fcm_token"

        @JvmStatic
        fun getAuthToken(context: Context): String? {
            val p = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return p.getString(KEY_TOKEN, null)
        }

        @JvmStatic
        fun saveFcmToken(context: Context, token: String) {
            val p = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            p.edit().putString(KEY_FCM_TOKEN, token).apply()
        }

        @JvmStatic
        fun getFcmToken(context: Context): String? {
            val p = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return p.getString(KEY_FCM_TOKEN, null)
        }

        @JvmStatic
        fun getUserId(context: Context): String {
            val p = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return p.getString(KEY_USER_ID, "") ?: ""
        }

        @JvmStatic
        fun getUserRole(context: Context): String {
            val p = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return p.getString(KEY_USER_ROLE, "buyer") ?: "buyer"
        }

        @JvmStatic
        fun isLoggedIn(context: Context): Boolean {
            val p = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return !p.getString(KEY_TOKEN, null).isNullOrBlank()
        }
    }

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun saveUserSession(
        userId: String,
        name: String,
        phone: String,
        role: String,
        village: String = "",
        district: String = "",
        state: String = "Madhya Pradesh",
        verificationStatus: String = "unverified"
    ) {
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_PHONE, phone)
            .putString(KEY_USER_ROLE, role)
            .putString(KEY_USER_VILLAGE, village)
            .putString(KEY_USER_DISTRICT, district)
            .putString(KEY_USER_STATE, state)
            .putString(KEY_USER_VERIFICATION, verificationStatus)
            .apply()
    }

    fun saveUserSession(session: UserSession) {
        saveUserSession(
            userId = session.uid,
            name = session.name,
            phone = session.phone,
            role = session.role,
            village = session.village,
            district = session.district,
            state = session.state,
            verificationStatus = session.verificationStatus
        )
    }

    fun getUserSession(): UserSession {
        return UserSession(
            uid = getUserId(),
            name = getUserName(),
            phone = getUserPhone(),
            email = prefs.getString(KEY_USER_EMAIL, "") ?: "",
            role = getUserRole(),
            village = prefs.getString(KEY_USER_VILLAGE, "") ?: "",
            district = prefs.getString(KEY_USER_DISTRICT, "") ?: "",
            state = prefs.getString(KEY_USER_STATE, "Madhya Pradesh") ?: "Madhya Pradesh",
            verificationStatus = prefs.getString(KEY_USER_VERIFICATION, "unverified") ?: "unverified"
        )
    }

    fun getUserId(): String = prefs.getString(KEY_USER_ID, "") ?: ""
    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""
    fun getUserPhone(): String = prefs.getString(KEY_USER_PHONE, "") ?: ""
    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, "buyer") ?: "buyer"

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
