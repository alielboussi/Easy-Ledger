package com.easyledger.app.core.auth

import android.content.Intent
import io.github.jan.supabase.auth.SessionStatus
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.handleDeeplinks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.easyledger.app.core.supabase.SupabaseProvider

sealed class AuthState {
	data object Loading: AuthState()
	data object SignedOut: AuthState()
	data class SignedIn(val userId: String): AuthState()
}

object SessionManager {
	private val scope = CoroutineScope(Dispatchers.IO)

	private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
	val authState: StateFlow<AuthState> = _authState.asStateFlow()

	// One-off error channel for auth failures
	private val _authErrors = MutableSharedFlow<String>(extraBufferCapacity = 1)
	val authErrors: SharedFlow<String> = _authErrors

	fun initialize() {
		scope.launch {
			runCatching {
				val session = SupabaseProvider.client.auth.currentSessionOrNull()
				if (session != null && session.user != null) {
					_authState.value = AuthState.SignedIn(session.user!!.id)
				} else {
					_authState.value = AuthState.SignedOut
				}
			}.onFailure {
				_authState.value = AuthState.SignedOut
			}
			// Also observe status changes
			SupabaseProvider.client.auth.sessionStatus.collect { status ->
				when (status) {
					is SessionStatus.Authenticated -> _authState.value = AuthState.SignedIn(status.session.user?.id ?: "")
					SessionStatus.NotAuthenticated -> _authState.value = AuthState.SignedOut
					is SessionStatus.LoadingFromStorage -> _authState.value = AuthState.Loading
				}
			}
		}
	}

	fun startGoogleSignIn() {
		// SDK-driven PKCE OAuth; SDK opens Custom Tabs automatically per Auth config
		scope.launch {
			runCatching {
				SupabaseProvider.client.auth.signInWith(Google)
			}.onFailure {
				_authErrors.tryEmit("Couldn't start Google sign-in: ${it.message ?: "Unknown error"}")
			}
		}
	}

	fun handleDeepLink(intent: Intent?) {
		// Prefer SDK deep link handling; will import session and update sessionStatus
		scope.launch {
			runCatching {
				SupabaseProvider.client.handleDeeplinks(intent)
			}.onFailure {
				_authErrors.tryEmit("Sign-in failed: ${it.message ?: "Unknown error"}")
			}
		}
	}

	// Removed Uri fallback; relying solely on SDK handleDeeplinks for uniform behavior across providers

	fun signOut() {
		scope.launch {
			runCatching { SupabaseProvider.client.auth.signOut() }
			_authState.value = AuthState.SignedOut
		}
	}
}
