// app/src/main/java/in/merakisan/app/core/security/SessionManager.kt
package `in`.merakisan.app.core.security

import android.content.Context
import android.content.SharedPreferences

data class UserSession(
    val uid: String,
    val name: String,
    val phone: String,
    val role: String,
    val isAuthenticated: Boolean,
    val email: String = ""
)

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    companion object {
        private const val PREFS_NAME = "mera_kisan_secure_session"

        private const val KEY_TOKEN = "auth_id_token"
        private const val KEY_UID = "user_uid"
        private const val KEY_NAME = "user_name"
        private const val KEY_PHONE = "user_phone"
        private const val KEY_ROLE = "user_role"
        private const val KEY_EMAIL = "user_email"

        fun getAuthToken(context: Context): String? {
            return SessionManager(context).getAuthToken()
        }

        fun getUserUid(context: Context): String? {
            return SessionManager(context).getUserUid()
        }

        fun getUserRole(context: Context): String {
            return SessionManager(context).getUserSession().role
        }

        fun saveSession(
            context: Context,
            token: String,
            uid: String,
            role: String,
            name: String = "",
            phone: String = "",
            email: String = ""
        ) {
            SessionManager(context).saveSession(
                token = token,
                uid = uid,
                name = name,
                phone = phone,
                role = role,
                email = email
            )
        }
                private const val KEY_FCM_TOKEN = "fcm_registration_token"

        fun saveFcmToken(context: Context, token: String) {
            if (token.isBlank()) return

            context.applicationContext
                .getSharedPreferences(
                    "mera_kisan_secure_session",
                    Context.MODE_PRIVATE
                )
                .edit()
                .putString(KEY_FCM_TOKEN, token)
                .apply()
        }

        fun getFcmToken(context: Context): String? {
            return context.applicationContext
                .getSharedPreferences(
                    "mera_kisan_secure_session",
                    Context.MODE_PRIVATE
                )
                .getString(KEY_FCM_TOKEN, null)
        }
    }

    fun saveAuthToken(token: String) {
        if (token.isNotBlank() &&
            token != "guest_session" &&
            !token.startsWith("guest_")
        ) {
            prefs.edit().putString(KEY_TOKEN, token).apply()
        }
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun getUserUid(): String {
        return prefs.getString(KEY_UID, "").orEmpty()
    }

    fun getUserId(): String {
        return getUserUid()
    }

    fun saveUserSession(
        uid: String,
        name: String,
        phone: String,
        role: String,
        email: String = ""
    ) {
        prefs.edit()
            .putString(KEY_UID, uid)
            .putString(KEY_NAME, name)
            .putString(KEY_PHONE, phone)
            .putString(KEY_ROLE, role.uppercase())
            .putString(KEY_EMAIL, email)
            .apply()
    }

    fun saveSession(
        token: String,
        uid: String,
        name: String,
        phone: String,
        role: String,
        email: String = ""
    ) {
        if (token.isNotBlank() &&
            token != "guest_session" &&
            !token.startsWith("guest_")
        ) {
            saveAuthToken(token)
        }

        saveUserSession(
            uid = uid,
            name = name,
            phone = phone,
            role = role,
            email = email
        )
    }

    fun getUserSession(): UserSession {
        val uid = getUserUid()
        val token = getAuthToken()

        return UserSession(
            uid = uid,
            name = prefs.getString(KEY_NAME, "").orEmpty(),
            phone = prefs.getString(KEY_PHONE, "").orEmpty(),
            role = prefs.getString(KEY_ROLE, "BUYER")
                .orEmpty()
                .ifBlank { "BUYER" },
            isAuthenticated = uid.isNotBlank() &&
                !token.isNullOrBlank(),
            email = prefs.getString(KEY_EMAIL, "").orEmpty()
        )
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
