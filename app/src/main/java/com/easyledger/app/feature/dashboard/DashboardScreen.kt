package com.easyledger.app.feature.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.compose.material3.Button
import com.easyledger.app.navigation.Routes

@Composable
fun DashboardScreen(navController: NavController) {
    Column(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        Text("Dashboard: Totals across all main businesses")
        // TODO: Summary cards for income/expense per currency
        Button(onClick = { navController.navigate(Routes.Business.route + "/list") }) {
            Text("View My Businesses")
        }
        Button(onClick = { navController.navigate(Routes.Business.route + "/create") }) {
            Text("Create a Business")
        }
        Button(onClick = { navController.navigate(Routes.Profile.route) }) {
            Text("Edit Profile")
        }
    }
}
