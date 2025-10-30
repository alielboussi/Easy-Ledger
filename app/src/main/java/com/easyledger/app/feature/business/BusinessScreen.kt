package com.easyledger.app.feature.business

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun BusinessScreen(navController: NavController) {
    Column(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        Text("Business management: main & sub businesses, categories, currency settings")
        // TODO: CRUD for main business, sub-businesses, categories
    }
}
