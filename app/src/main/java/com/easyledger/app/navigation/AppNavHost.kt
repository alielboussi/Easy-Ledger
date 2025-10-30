package com.easyledger.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.easyledger.app.feature.auth.AuthScreen
import com.easyledger.app.feature.dashboard.DashboardScreen
import com.easyledger.app.feature.business.BusinessScreen
import com.easyledger.app.feature.reports.ReportsScreen
import com.easyledger.app.feature.settings.SettingsScreen

sealed class Routes(val route: String) {
    data object Auth: Routes("auth")
    data object Dashboard: Routes("dashboard")
    data object Business: Routes("business")
    data object Reports: Routes("reports")
    data object Settings: Routes("settings")
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.Auth.route) {
        composable(Routes.Auth.route) { AuthScreen(onAuthenticated = { navController.navigate(Routes.Dashboard.route) { popUpTo(Routes.Auth.route) { inclusive = true } } }) }
        composable(Routes.Dashboard.route) { DashboardScreen(navController) }
        composable(Routes.Business.route) { BusinessScreen(navController) }
        composable(Routes.Business.route + "/create") { com.easyledger.app.feature.business.CreateBusinessScreen(navController) }
        composable(Routes.Reports.route) { ReportsScreen(navController) }
        composable(Routes.Settings.route) { SettingsScreen(navController) }
    }
}
