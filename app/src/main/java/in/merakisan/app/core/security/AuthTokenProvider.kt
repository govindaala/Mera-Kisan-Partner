package `in`.merakisan.app.core.security

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * MERA KISAN — सुरक्षित ऑथेंटिकेशन टोकन प्रोवाइडर
 * यह इंजन सुनिश्चित करता है कि केवल वैध Firebase ID Token ही नेटवर्क कॉल्स में जाए।
 */
class AuthTokenProvider(context: Context) {

    private val sessionManager = SessionManager(context.applicationContext)
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * वैध और ताज़ा Firebase ID Token प्राप्त करता है।
     * यदि टोकन समाप्त होने वाला है, तो यह स्वतः फ़ायरबेस से नया टोकन लाता है।
     */
    suspend fun getValidBearerToken(forceRefresh: Boolean = false): String? {
        val currentUser = firebaseAuth.currentUser

        // 1. यदि यूज़र लॉग-इन नहीं है या गेस्ट मोड में है, तो कोई बियरर टोकन न भेजें
        if (currentUser == null) {
            val localToken = sessionManager.getAuthToken()
            return if (!localToken.isNullOrBlank() && !localToken.startsWith("guest_") && localToken != "guest_session") {
                localToken
            } else {
                null
            }
        }

        return try {
            // 2. Firebase से ताज़ा ID Token प्राप्त करें
            val tokenResult = currentUser.getIdToken(forceRefresh).await()
            val freshToken = tokenResult.token

            if (!freshToken.isNullOrBlank()) {
                sessionManager.saveAuthToken(freshToken)
                freshToken
            } else {
                sessionManager.getAuthToken()
            }
        } catch (_: Exception) {
            // नेटवर्क विफलता पर स्थानीय रूप से सुरक्षित अंतिम टोकन लौटाएं
            sessionManager.getAuthToken()
        }
    }

    /**
     * जांचता है कि वर्तमान सत्र सक्रिय और प्रमाणित है या नहीं।
     */
    fun isAuthenticatedUser(): Boolean {
        val user = firebaseAuth.currentUser
        val session = sessionManager.getUserSession()
        return user != null && session.isAuthenticated && !session.uid.startsWith("guest_")
    }

    /**
     * पूर्ण सुरक्षित लॉगआउट और सत्र सफ़ाई
     */
    fun signOut() {
        firebaseAuth.signOut()
        sessionManager.clearSession()
    }
}
