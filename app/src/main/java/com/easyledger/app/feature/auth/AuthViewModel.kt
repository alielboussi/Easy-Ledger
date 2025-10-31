package com.easyledger.app.feature.auth
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easyledger.app.core.auth.AuthState
import com.easyledger.app.core.auth.SessionManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    val authState: StateFlow<AuthState> = SessionManager.authState
    private var pendingEmail: String? = null
    private var pendingPassword: String? = null

    init {
        SessionManager.initialize()
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            SessionManager.startGoogleSignIn()
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch { SessionManager.signInWithEmail(email, password) }
    }

    fun signInWithUsername(username: String, password: String) {
        viewModelScope.launch { SessionManager.signInWithUsername(username, password) }
    }

    fun signUpWithEmail(
        username: String,
        email: String,
        password: String,
        country: String?,
        countryCode: String?,
        phone: String?
    ) {
        pendingEmail = email
        pendingPassword = password
        viewModelScope.launch { SessionManager.signUpWithEmail(username, email, password, country, countryCode, phone) }
    }

    fun completePendingEmailSignIn() {
        val e = pendingEmail ?: return
        val p = pendingPassword ?: return
        viewModelScope.launch { SessionManager.signInWithEmail(e, p) }
        pendingEmail = null
        pendingPassword = null
    }

    fun signOut() {
        viewModelScope.launch { SessionManager.signOut() }
    }
}
