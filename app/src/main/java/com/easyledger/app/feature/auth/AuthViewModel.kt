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

    fun signOut() {
        viewModelScope.launch { SessionManager.signOut() }
    }
}
