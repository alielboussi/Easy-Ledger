package com.easyledger.app.feature.auth

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.easyledger.app.core.auth.AuthState
import com.easyledger.app.core.auth.SessionManager

@Composable
fun AuthScreen(onAuthenticated: () -> Unit, viewModel: AuthViewModel = viewModel()) {
    val state by viewModel.authState.collectAsState()
    val activity = (LocalActivityResultRegistryOwner.current as? androidx.activity.ComponentActivity)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        if (state is AuthState.SignedIn) onAuthenticated()
    }

    // Show auth errors as snackbars
    LaunchedEffect(Unit) {
        SessionManager.authErrors.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { _ ->
        Column(modifier = androidx.compose.ui.Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            when (state) {
                AuthState.Loading -> Text("Loading...")
                AuthState.SignedOut -> Button(onClick = { activity?.let { viewModel.signInWithGoogle(it) } }) { Text("Sign in with Google") }
                is AuthState.SignedIn -> Text("Signed in. Redirecting...")
            }
        }
    }
}
