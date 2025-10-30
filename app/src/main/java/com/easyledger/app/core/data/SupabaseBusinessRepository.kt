package com.easyledger.app.core.data

import com.easyledger.app.core.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CreateBusinessParams(
	val name: String,
	val logoUrl: String?,
	val primaryCurrency: String,
	val secondaryCurrency: String?,
	val currencySymbol: String?,
	val currencyFormat: String?
)

// Basic implementation for creating and listing businesses
class SupabaseBusinessRepository: BusinessRepository {
	suspend fun createBusiness(params: CreateBusinessParams): Result<Unit> = withContext(Dispatchers.IO) {
		runCatching {
			val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id
				?: error("Not authenticated")
			val body = buildMap<String, Any?> {
				put("owner_id", userId)
				put("name", params.name)
				put("logo_url", params.logoUrl)
				put("currency_primary", params.primaryCurrency)
				put("currency_secondary", params.secondaryCurrency)
				put("currency_symbol", params.currencySymbol)
				put("currency_format", params.currencyFormat)
			}
			SupabaseProvider.client.postgrest["businesses"].insert(body)
		}.fold(
			onSuccess = { Result.success(Unit) },
			onFailure = { Result.failure(it) }
		)
	}

	suspend fun listBusinesses(): Result<List<Map<String, Any?>>> = withContext(Dispatchers.IO) {
		runCatching {
			SupabaseProvider.client.postgrest["businesses"].select().decodeList<Map<String, Any?>>()
		}.fold(
			onSuccess = { Result.success(it) },
			onFailure = { Result.failure(it) }
		)
	}
}
