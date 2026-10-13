// app/src/main/java/in/merakisan/app/ui/auth/AuthViewModel.kt
package `in`.merakisan.app.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import `in`.merakisan.app.core.security.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val message: String) : AuthUiState()
    data class Error(val errorMessage: String) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val firebaseAuth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun handleFirebaseTokenSuccess(onComplete: (Boolean) -> Unit) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val user = firebaseAuth.currentUser
                if (user != null) {
                    val tokenResult = user.getIdToken(true).await()
                    val idToken = tokenResult.token

                    if (!idToken.isNullOrBlank()) {
                        sessionManager.saveAuthToken(idToken)
                        sessionManager.saveUserSession(
                            uid = user.uid,
                            name = user.displayName ?: "किसान साथी",
                            phone = user.phoneNumber ?: "",
                            role = "both"
                        )
                        _uiState.value = AuthUiState.Success("लॉगिन सफल")
                        onComplete(true)
                        return@launch
                    }
                }
                _uiState.value = AuthUiState.Error("प्रमाणीकरण टोकन प्राप्त नहीं हुआ।")
                onComplete(false)
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "लॉगिन विफल")
                onComplete(false)
            }
        }
    }

    fun continueAsGuest(onComplete: () -> Unit) {
        sessionManager.clearSession()
        sessionManager.saveUserSession(
            uid = "guest_${System.currentTimeMillis()}",
            name = "अतिथि खरीदार",
            phone = "",
            role = "BUYER"
        )
        onComplete()
    }
}
