// app/src/main/java/in/merakisan/app/core/security/SessionManager.kt
package `in`.merakisan.app.core.security

import android.content.Context
import android.content.SharedPreferences

data class UserSession(
    val uid: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val role: String = "both"
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
        private const val KEY_FCM_TOKEN = "fcm_token"

        @JvmStatic
        fun getAuthToken(context: Context): String? =
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)

        @JvmStatic
        fun getUserUid(context: Context): String? =
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_USER_ID, null)

        @JvmStatic
        fun getUserId(context: Context): String = getUserUid(context) ?: ""

        @JvmStatic
        fun getUserRole(context: Context): String =
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_USER_ROLE, "buyer") ?: "buyer"

        @JvmStatic
        fun saveFcmToken(context: Context, token: String) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putString(KEY_FCM_TOKEN, token).apply()
        }

        @JvmStatic
        @JvmOverloads
        fun saveSession(
            context: Context? = null,
            token: String = "",
            uid: String = "",
            userId: String = uid,
            name: String = "",
            phone: String = "",
            role: String = "both"
        ) {
            val ctx = context ?: return
            val finalUid = if (uid.isNotBlank()) uid else userId
            val editor = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_USER_ID, finalUid)
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_PHONE, phone)
                .putString(KEY_USER_ROLE, role)
            if (token.isNotBlank()) editor.putString(KEY_TOKEN, token)
            editor.apply()
        }
    }

    fun saveAuthToken(token: String) = prefs.edit().putString(KEY_TOKEN, token).apply()
    fun getAuthToken(): String? = prefs.getString(KEY_TOKEN, null)

    @JvmOverloads
    fun saveSession(
        token: String = "",
        uid: String = "",
        userId: String = uid,
        name: String = "",
        phone: String = "",
        role: String = "both"
    ) {
        val finalUid = if (uid.isNotBlank()) uid else userId
        val editor = prefs.edit()
            .putString(KEY_USER_ID, finalUid)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_PHONE, phone)
            .putString(KEY_USER_ROLE, role)
        if (token.isNotBlank()) editor.putString(KEY_TOKEN, token)
        editor.apply()
    }

    fun saveUserSession(userId: String = "", name: String = "", phone: String = "", role: String = "both") =
        saveSession(userId = userId, name = name, phone = phone, role = role)

    fun getUserSession(): UserSession =
        UserSession(getUserId(), getUserName(), getUserPhone(), prefs.getString(KEY_USER_EMAIL, "") ?: "", getUserRole())

    fun getUserId(): String = prefs.getString(KEY_USER_ID, "") ?: ""
    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""
    fun getUserPhone(): String = prefs.getString(KEY_USER_PHONE, "") ?: ""
    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, "buyer") ?: "buyer"
    fun clearSession() = prefs.edit().clear().apply()
}
