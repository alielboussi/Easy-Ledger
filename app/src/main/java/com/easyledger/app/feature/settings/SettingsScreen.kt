package com.easyledger.app.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.compose.ui.unit.dp
import com.easyledger.app.navigation.Routes
import androidx.compose.ui.Modifier

@Composable
fun SettingsScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Settings: App version V1.0, Theme, Currency symbol/format")
        // TODO: Implement dark/light/system theme toggle, app version display, currency settings
        Spacer(Modifier.height(16.dp))
        Button(onClick = { navController.navigate(Routes.Profile.route) }) {
            Text("Profile")
        }
    }
}
