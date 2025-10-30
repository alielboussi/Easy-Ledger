package com.easyledger.app.feature.reports

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun ReportsScreen(navController: NavController) {
    Column(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        Text("Reports: filtered totals and PDF generation with charts")
        // TODO: Date filter, generate PDF, upload to Supabase Storage bucket 'reports'
    }
}
