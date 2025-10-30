package com.easyledger.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.easyledger.app.ui.theme.EasyLedgerTheme
import com.easyledger.app.navigation.AppNavHost
import android.content.Intent
import com.easyledger.app.core.supabase.SupabaseProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
    // OAuth/OTP deeplink handling is configured in the Auth plugin; no explicit call required here.
        setContent {
            EasyLedgerTheme {
                AppNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    // If needed, forward new intents (e.g., OAuth redirect) to the Auth plugin here in the future.
    }
}
