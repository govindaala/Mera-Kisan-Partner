// app/src/main/java/in/merakisan/app/ui/auth/AuthViewModel.kt
package `in`.merakisan.app.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import `in`.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class GoogleLinked(val account: GoogleSignInAccount) : AuthUiState()
    data class Success(val role: String, val name: String = "किसान साथी") : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val firebaseAuth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var currentAccount: GoogleSignInAccount? = null

    fun resetError() {
        _uiState.value = AuthUiState.Idle
    }

    // असली Firebase Google Auth इंटीग्रेशन
    fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val idToken = account.idToken
        if (idToken.isNullOrBlank()) {
            _uiState.value = AuthUiState.Error("Google idToken प्राप्त नहीं हो सका")
            return
        }

        currentAccount = account
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    val uid = firebaseUser.uid
                    val name = firebaseUser.displayName ?: account.displayName ?: "किसान साथी"
                    val email = firebaseUser.email ?: account.email ?: ""

                    // असली Firebase टोकन और क्रेडेंशियल सहेजना
                    sessionManager.saveAuthToken(idToken)
                    sessionManager.saveSession(
                        token = idToken,
                        uid = uid,
                        name = name,
                        phone = firebaseUser.phoneNumber.orEmpty(),
                        role = "both"
                    )

                    _uiState.value = AuthUiState.GoogleLinked(account)
                } else {
                    _uiState.value = AuthUiState.Error("Firebase यूज़र सत्यापन विफल रहा")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Firebase प्रमाणीकरण त्रुटि")
            }
        }
    }

    fun finalizeAuthentication(phone: String) {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null) {
            _uiState.value = AuthUiState.Error("सत्र समाप्त हुआ। कृपया Google से पुनः साइन इन करें।")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val uid = firebaseUser.uid
            val name = firebaseUser.displayName ?: "किसान साथी"
            val token = sessionManager.getAuthToken().orEmpty()

            sessionManager.saveSession(
                token = token,
                uid = uid,
                name = name,
                phone = phone,
                role = "both"
            )

            _uiState.value = AuthUiState.Success("both", name)
        }
    }

    fun continueAsGuest() {
        val guestUid = "guest_${System.currentTimeMillis()}"
        sessionManager.saveAuthToken("guest_session")
        sessionManager.saveSession(
            token = "guest_session",
            uid = guestUid,
            name = "अतिथि किसान",
            phone = "",
            role = "buyer"
        )
        _uiState.value = AuthUiState.Success("buyer", "अतिथि किसान")
    }
}
