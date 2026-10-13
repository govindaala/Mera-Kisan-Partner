// app/src/main/java/in/merakisan/app/core/security/SessionManager.kt
package `in`.merakisan.app.core.security

import android.content.Context
import android.content.SharedPreferences

data class UserSession(
    val uid: String,
    val name: String,
    val phone: String,
    val role: String,
    val isAuthenticated: Boolean
)

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(
            "mera_kisan_secure_session",
            Context.MODE_PRIVATE
        )

    companion object {
        private const val KEY_TOKEN = "auth_id_token"
        private const val KEY_UID = "user_uid"
        private const val KEY_NAME = "user_name"
        private const val KEY_PHONE = "user_phone"
        private const val KEY_ROLE = "user_role"
        private const val KEY_FCM_TOKEN = "fcm_token"

        private fun preferences(
            context: Context
        ): SharedPreferences {
            return context.applicationContext.getSharedPreferences(
                "mera_kisan_secure_session",
                Context.MODE_PRIVATE
            )
        }

        fun saveFcmToken(
            context: Context,
            token: String
        ) {
            if (token.isNotBlank()) {
                preferences(context)
                    .edit()
                    .putString(KEY_FCM_TOKEN, token)
                    .apply()
            }
        }

        fun getUserUid(
            context: Context
        ): String? {
            return preferences(context)
                .getString(KEY_UID, null)
                ?.takeIf { it.isNotBlank() }
        }
    }

    fun saveAuthToken(token: String) {
        if (
            token.isNotBlank() &&
            token != "guest_session" &&
            !token.startsWith("guest_")
        ) {
            prefs.edit()
                .putString(KEY_TOKEN, token)
                .apply()
        }
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun saveUserSession(
        uid: String,
        name: String,
        phone: String,
        role: String
    ) {
        prefs.edit()
            .putString(KEY_UID, uid)
            .putString(KEY_NAME, name)
            .putString(KEY_PHONE, phone)
            .putString(KEY_ROLE, role)
            .apply()
    }

    fun getUserSession(): UserSession {
        val uid = prefs.getString(KEY_UID, "") ?: ""
        val token = getAuthToken()

        return UserSession(
            uid = uid,
            name = prefs.getString(KEY_NAME, "") ?: "",
            phone = prefs.getString(KEY_PHONE, "") ?: "",
            role = prefs.getString(KEY_ROLE, "BUYER") ?: "BUYER",
            isAuthenticated = uid.isNotBlank() &&
                !token.isNullOrBlank()
        )
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
