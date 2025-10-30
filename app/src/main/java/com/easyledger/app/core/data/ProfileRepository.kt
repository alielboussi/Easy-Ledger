package com.easyledger.app.core.data

import com.easyledger.app.core.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val email: String,
    val username: String,
    @SerialName("date_of_birth") val date_of_birth: String? = null,
    val country: String? = null,
    @SerialName("country_code") val country_code: String? = null,
    val phone: String? = null,
)

@Serializable
data class ProfileUpdate(
    val username: String? = null,
    @SerialName("date_of_birth") val date_of_birth: String? = null,
    val country: String? = null,
    @SerialName("country_code") val country_code: String? = null,
    val phone: String? = null,
)

class ProfileRepository {
    private val client get() = SupabaseProvider.client

    suspend fun getCurrent(): Profile? {
        val user = client.auth.currentUserOrNull() ?: return null
        val rows = client.postgrest["profiles"].select {
            filter { eq("id", user.id) }
        }.decodeList<Profile>()
        return rows.firstOrNull()
    }

    suspend fun update(update: ProfileUpdate): Profile? {
        val user = client.auth.currentUserOrNull() ?: return null
        client.postgrest["profiles"].update(update) {
            filter { eq("id", user.id) }
        }
        return getCurrent()
    }
}
