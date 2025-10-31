package com.easyledger.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun OtpScreen(navController: NavController, emailArg: String) {
    val vm: OtpViewModel = viewModel()
    val authVm: AuthViewModel = viewModel()
    val red = Color(0xFFB00020)
    val glow = Color(0x66B00020)
    val (code, setCode) = remember { mutableStateOf("") }

    LaunchedEffect(emailArg) { vm.send(emailArg) }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Verify your email", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            Text("We sent a 4-character code to $emailArg")
            Spacer(Modifier.height(8.dp))
            // Status banner
            val statusColor = Color(0xFF006400)
            val errorColor = Color(0xFFB00020)
            val state = vm.state
            androidx.compose.runtime.CompositionLocalProvider {
                when (state.value) {
                    is OtpState.Sending -> Text("Sending code...", color = statusColor)
                    is OtpState.Sent -> Text("Code sent", color = statusColor)
                    is OtpState.Verifying -> Text("Verifying...", color = statusColor)
                    is OtpState.Error -> Text((state.value as OtpState.Error).message, color = errorColor)
                    else -> {}
                }
            }
            Spacer(Modifier.height(24.dp))
            Card(colors = CardDefaults.cardColors(containerColor = glow)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 4) setCode(it.uppercase().filter { ch -> ch.isLetterOrDigit() }) },
                    label = { Text("Enter code") },
                    modifier = Modifier.fillMaxWidth().background(Color.White).padding(8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = red,
                        unfocusedBorderColor = red,
                        focusedLabelColor = red,
                        cursorColor = red
                    )
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { vm.verify(emailArg, code) }, modifier = Modifier.fillMaxWidth()) {
                Text("Verify")
            }
            Spacer(Modifier.height(8.dp))
            var countdown by remember { mutableIntStateOf(0) }
            LaunchedEffect(state.value) {
                if (state.value is OtpState.Sent) {
                    countdown = 60
                    while (countdown > 0) {
                        delay(1000)
                        countdown -= 1
                    }
                }
            }
            Button(onClick = { if (countdown == 0) vm.send(emailArg) }, enabled = countdown == 0, modifier = Modifier.fillMaxWidth()) {
                if (countdown == 0) Text("Resend code") else Text("Resend in ${countdown}s")
            }
        }
    }

    // Navigate on success and complete sign in with stored pending password
    LaunchedEffect(Unit) {
        vm.state.collect { s ->
            if (s is OtpState.Success) {
                authVm.completePendingEmailSignIn()
                navController.navigate(com.easyledger.app.navigation.Routes.Dashboard.route) {
                    popUpTo(com.easyledger.app.navigation.Routes.Auth.route) { inclusive = true }
                }
            }
        }
    }
}
