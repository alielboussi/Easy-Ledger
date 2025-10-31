package com.easyledger.app.core.supabase

import com.easyledger.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.ExternalAuthAction
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.functions.functions

object SupabaseProvider {
    private val SUPABASE_URL: String get() = BuildConfig.SUPABASE_URL
    private val SUPABASE_ANON: String get() = BuildConfig.SUPABASE_ANON_KEY

    val client: SupabaseClient by lazy {
        createSupabaseClient(SUPABASE_URL, SUPABASE_ANON) {
            install(Auth) {
                // Configure PKCE + Android deeplink callback
                scheme = "easyledger"
                host = "auth-callback"
                flowType = FlowType.PKCE
                // Open OAuth in Chrome Custom Tabs on Android
                defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
                // NOTE: In Supabase Dashboard, enable the Google provider and add the redirect URL:
                // easyledger://auth-callback
            }
            install(io.github.jan.supabase.postgrest.Postgrest)
            install(io.github.jan.supabase.storage.Storage)
            install(io.github.jan.supabase.functions.Functions)
        }
    }
}
