package com.easyledger.app.core.data

import com.easyledger.app.core.supabase.SupabaseProvider
import io.github.jan.supabase.functions.functions
import kotlinx.serialization.Serializable
import io.ktor.client.statement.bodyAsText

class OtpRepository {
    @Serializable
    data class OtpRequest(val email: String)

    @Serializable
    data class VerifyRequest(val email: String, val code: String)

    @Serializable
    data class OtpResponse(val ok: Boolean, val message: String? = null)

    suspend fun send(email: String): OtpResponse {
    val response = SupabaseProvider.client.functions.invoke("send-otp", OtpRequest(email))
    val result: String = response.bodyAsText()
        // Edge functions return simple text or JSON; attempt to parse else wrap text
        return try {
            kotlinx.serialization.json.Json.decodeFromString(OtpResponse.serializer(), result)
        } catch (_: Throwable) {
            OtpResponse(ok = result.trim().equals("ok", ignoreCase = true), message = result)
        }
    }

    suspend fun verify(email: String, code: String): OtpResponse {
    val response = SupabaseProvider.client.functions.invoke("verify-otp", VerifyRequest(email, code))
    val result: String = response.bodyAsText()
        return try {
            kotlinx.serialization.json.Json.decodeFromString(OtpResponse.serializer(), result)
        } catch (_: Throwable) {
            OtpResponse(ok = result.trim().equals("ok", ignoreCase = true), message = result)
        }
    }
}
