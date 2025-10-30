package com.easyledger.app.feature.auth
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easyledger.app.core.auth.AuthState
import com.easyledger.app.core.auth.SessionManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    val authState: StateFlow<AuthState> = SessionManager.authState

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
        dateOfBirth: String?,
        country: String?,
        countryCode: String?,
        phone: String?
    ) {
        viewModelScope.launch { SessionManager.signUpWithEmail(username, email, password, dateOfBirth, country, countryCode, phone) }
    }

    fun signOut() {
        viewModelScope.launch { SessionManager.signOut() }
    }
}
