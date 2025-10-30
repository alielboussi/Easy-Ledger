package com.easyledger.app.feature.auth

import android.app.Activity
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

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            SessionManager.startGoogleSignIn(activity)
        }
    }

    fun signOut() {
        viewModelScope.launch { SessionManager.signOut() }
    }
}
