package com.easyledger.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.easyledger.app.ui.theme.EasyLedgerTheme
import com.easyledger.app.navigation.AppNavHost
import android.content.Intent
import com.easyledger.app.core.auth.SessionManager
import com.easyledger.app.core.supabase.SupabaseProvider
import io.github.jan.supabase.handleDeeplinks

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Let Supabase SDK parse and finalize OAuth/OTP sessions from deeplinks
        runCatching { SupabaseProvider.client.handleDeeplinks(intent) }
        setContent {
            EasyLedgerTheme {
                AppNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        runCatching { SupabaseProvider.client.handleDeeplinks(intent) }
    }
}
