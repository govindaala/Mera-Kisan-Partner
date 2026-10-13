// app/src/main/java/in/merakisan/app/core/security/AuthTokenProvider.kt
package `in`.merakisan.app.core.security

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthTokenProvider(context: Context) {

    private val sessionManager = SessionManager(context.applicationContext)
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    suspend fun getValidBearerToken(forceRefresh: Boolean = false): String? {
        val currentUser = firebaseAuth.currentUser

        if (currentUser == null) {
            val localToken = sessionManager.getAuthToken()
            return if (!localToken.isNullOrBlank() && !localToken.startsWith("guest_") && localToken != "guest_session") {
                localToken
            } else {
                null
            }
        }

        return try {
            val tokenResult = currentUser.getIdToken(forceRefresh).await()
            val freshToken = tokenResult.token

            if (!freshToken.isNullOrBlank()) {
                sessionManager.saveAuthToken(freshToken)
                freshToken
            } else {
                sessionManager.getAuthToken()
            }
        } catch (_: Exception) {
            sessionManager.getAuthToken()
        }
    }

    fun isAuthenticatedUser(): Boolean {
        val user = firebaseAuth.currentUser
        val session = sessionManager.getUserSession()
        return user != null && session.isAuthenticated && !session.uid.startsWith("guest_")
    }

    fun signOut() {
        firebaseAuth.signOut()
        sessionManager.clearSession()
    }
}
