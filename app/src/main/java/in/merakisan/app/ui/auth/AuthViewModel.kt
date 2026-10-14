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
    object GoogleLinked : AuthUiState()

    data class Success(
        val name: String
    ) : AuthUiState()

    data class Error(
        val message: String
    ) : AuthUiState()
}

class AuthViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val firebaseAuth = FirebaseAuth.getInstance()

    private val _uiState =
        MutableStateFlow<AuthUiState>(AuthUiState.Idle)

    val uiState: StateFlow<AuthUiState> =
        _uiState.asStateFlow()

    fun resetError() {
        _uiState.value = AuthUiState.Idle
    }

    fun firebaseAuthWithGoogle(
        account: GoogleSignInAccount
    ) {
        val idToken = account.idToken

        if (idToken.isNullOrBlank()) {
            _uiState.value = AuthUiState.Error(
                "Google से प्रमाणीकरण टोकन नहीं मिला।"
            )
            return
        }

        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                val credential =
                    GoogleAuthProvider.getCredential(idToken, null)

                val authResult =
                    firebaseAuth.signInWithCredential(credential).await()

                val user = authResult.user
                    ?: throw IllegalStateException(
                        "Firebase उपयोगकर्ता नहीं मिला।"
                    )

                val tokenResult = user.getIdToken(true).await()
                val firebaseToken = tokenResult.token

                if (firebaseToken.isNullOrBlank()) {
                    throw IllegalStateException(
                        "Firebase प्रमाणीकरण टोकन नहीं मिला।"
                    )
                }

                val name = user.displayName
                    ?: account.displayName
                    ?: "किसान साथी"

                sessionManager.saveSession(
                    token = firebaseToken,
                    uid = user.uid,
                    name = name,
                    phone = user.phoneNumber.orEmpty(),
                    email = user.email.orEmpty(),
                    role = "BUYER"
                )

                _uiState.value = AuthUiState.GoogleLinked

            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(
                    e.localizedMessage ?: "Google लॉगिन विफल हुआ।"
                )
            }
        }
    }

    fun handleFirebaseTokenSuccess(
        onComplete: (Boolean) -> Unit
    ) {
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                val user = firebaseAuth.currentUser

                if (user == null) {
                    _uiState.value = AuthUiState.Error(
                        "Firebase उपयोगकर्ता लॉगिन नहीं है।"
                    )
                    onComplete(false)
                    return@launch
                }

                val tokenResult = user.getIdToken(true).await()
                val token = tokenResult.token

                if (token.isNullOrBlank()) {
                    _uiState.value = AuthUiState.Error(
                        "प्रमाणीकरण टोकन प्राप्त नहीं हुआ।"
                    )
                    onComplete(false)
                    return@launch
                }

                val name = user.displayName ?: "किसान साथी"

                sessionManager.saveSession(
                    token = token,
                    uid = user.uid,
                    name = name,
                    phone = user.phoneNumber.orEmpty(),
                    email = user.email.orEmpty(),
                    role = "BUYER"
                )

                _uiState.value = AuthUiState.Success(name)
                onComplete(true)

            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(
                    e.localizedMessage ?: "लॉगिन विफल हुआ।"
                )
                onComplete(false)
            }
        }
    }

    fun continueAsGuest() {
        sessionManager.clearSession()

        sessionManager.saveUserSession(
            uid = "guest_${System.currentTimeMillis()}",
            name = "अतिथि खरीदार",
            phone = "",
            role = "BUYER",
            email = ""
        )

        _uiState.value = AuthUiState.Success("अतिथि खरीदार")
    }

    fun continueAsGuest(onComplete: () -> Unit) {
        continueAsGuest()
        onComplete()
    }
}
