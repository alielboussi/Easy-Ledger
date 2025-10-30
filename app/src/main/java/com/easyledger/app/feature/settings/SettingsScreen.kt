package com.easyledger.app.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun SettingsScreen(navController: NavController) {
    Column(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        Text("Settings: App version V1.0, Theme, Currency symbol/format")
        // TODO: Implement dark/light/system theme toggle, app version display, currency settings
    }
}
