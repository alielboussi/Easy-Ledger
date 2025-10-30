package com.easyledger.app.core.data

import com.easyledger.app.core.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlin.time.Duration.Companion.seconds
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

data class UpdateBusinessParams(
	val name: String? = null,
	val logoUrl: String? = null,
	val primaryCurrency: String? = null,
	val secondaryCurrency: String? = null,
	val currencySymbol: String? = null,
	val currencyFormat: String? = null
)

// Basic implementation for creating and listing businesses
class SupabaseBusinessRepository: BusinessRepository {
	suspend fun createBusiness(params: CreateBusinessParams, explicitId: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
		runCatching {
			val userId = SupabaseProvider.client.auth.currentUserOrNull()?.id
				?: error("Not authenticated")
			val body = buildMap<String, Any?> {
				if (explicitId != null) put("id", explicitId)
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

	/**
	 * Uploads a business logo image to the private 'logos' bucket under
	 * user-scoped path: {userId}/businesses/{businessId}.{ext}
	 * Returns the storage path string if successful.
	 */
	suspend fun uploadBusinessLogo(
		userId: String,
		businessId: String,
		bytes: ByteArray,
		mimeType: String
	): Result<String> = withContext(Dispatchers.IO) {
		runCatching {
			val ext = when {
				mimeType.contains("png", ignoreCase = true) -> "png"
				mimeType.contains("jpeg", ignoreCase = true) || mimeType.contains("jpg", ignoreCase = true) -> "jpg"
				mimeType.contains("webp", ignoreCase = true) -> "webp"
				else -> "jpg"
			}
			val path = "$userId/businesses/$businessId.$ext"
			SupabaseProvider.client.storage.from("logos").upload(
				path = path,
				data = bytes
			) {
				upsert = true
				contentType = ContentType.parse(mimeType)
			}
			path
		}.fold(
			onSuccess = { Result.success(it) },
			onFailure = { Result.failure(it) }
		)
	}

	/** Upload bytes to an explicit logos path (upsert) */
	suspend fun uploadLogoToPath(
		path: String,
		bytes: ByteArray,
		mimeType: String
	): Result<Unit> = withContext(Dispatchers.IO) {
		runCatching {
			SupabaseProvider.client.storage.from("logos").upload(
				path = path,
				data = bytes
			) {
				upsert = true
				// Content type may be set by caller if needed; omitted here.
			}
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

	suspend fun getBusiness(id: String): Result<Map<String, Any?>> = withContext(Dispatchers.IO) {
		runCatching {
			SupabaseProvider.client.postgrest["businesses"].select {
				filter { eq("id", id) }
			}.decodeSingle<Map<String, Any?>>()
		}.fold(
			onSuccess = { Result.success(it) },
			onFailure = { Result.failure(it) }
		)
	}

	suspend fun updateBusiness(id: String, params: UpdateBusinessParams): Result<Unit> = withContext(Dispatchers.IO) {
		runCatching {
			val body = buildMap<String, Any?> {
				params.name?.let { put("name", it) }
				if (params.logoUrl != null) put("logo_url", params.logoUrl)
				params.primaryCurrency?.let { put("currency_primary", it) }
				if (params.secondaryCurrency != null) put("currency_secondary", params.secondaryCurrency)
				if (params.currencySymbol != null) put("currency_symbol", params.currencySymbol)
				if (params.currencyFormat != null) put("currency_format", params.currencyFormat)
			}
			if (body.isNotEmpty()) {
				SupabaseProvider.client.postgrest["businesses"].update(body) {
					filter { eq("id", id) }
				}
			}
		}.fold(
			onSuccess = { Result.success(Unit) },
			onFailure = { Result.failure(it) }
		)
	}

	suspend fun deleteBusiness(id: String): Result<Unit> = withContext(Dispatchers.IO) {
		runCatching {
			SupabaseProvider.client.postgrest["businesses"].delete {
				filter { eq("id", id) }
			}
		}.fold(
			onSuccess = { Result.success(Unit) },
			onFailure = { Result.failure(it) }
		)
	}

	// Sub-businesses
	suspend fun listSubBusinesses(businessId: String): Result<List<Map<String, Any?>>> = withContext(Dispatchers.IO) {
		runCatching {
			SupabaseProvider.client.postgrest["sub_businesses"].select {
				filter { eq("business_id", businessId) }
			}.decodeList<Map<String, Any?>>()
		}.fold(
			onSuccess = { Result.success(it) },
			onFailure = { Result.failure(it) }
		)
	}

	suspend fun createSubBusiness(businessId: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
		runCatching {
			SupabaseProvider.client.postgrest["sub_businesses"].insert(mapOf(
				"business_id" to businessId,
				"name" to name
			))
		}.fold(
			onSuccess = { Result.success(Unit) },
			onFailure = { Result.failure(it) }
		)
	}

	suspend fun updateSubBusiness(id: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
		runCatching {
			SupabaseProvider.client.postgrest["sub_businesses"].update(mapOf("name" to name)) {
				filter { eq("id", id) }
			}
		}.fold(
			onSuccess = { Result.success(Unit) },
			onFailure = { Result.failure(it) }
		)
	}

	suspend fun deleteSubBusiness(id: String): Result<Unit> = withContext(Dispatchers.IO) {
		runCatching {
			SupabaseProvider.client.postgrest["sub_businesses"].delete {
				filter { eq("id", id) }
			}
		}.fold(
			onSuccess = { Result.success(Unit) },
			onFailure = { Result.failure(it) }
		)
	}

	// Categories
	suspend fun listCategoriesForBusiness(businessId: String): Result<List<Map<String, Any?>>> = withContext(Dispatchers.IO) {
		runCatching {
			SupabaseProvider.client.postgrest["categories"].select {
				filter { eq("business_id", businessId) }
			}.decodeList<Map<String, Any?>>()
		}.fold(
			onSuccess = { Result.success(it) },
			onFailure = { Result.failure(it) }
		)
	}

	suspend fun listCategoriesForSubBusiness(subBusinessId: String): Result<List<Map<String, Any?>>> = withContext(Dispatchers.IO) {
		runCatching {
			SupabaseProvider.client.postgrest["categories"].select {
				filter { eq("sub_business_id", subBusinessId) }
			}.decodeList<Map<String, Any?>>()
		}.fold(
			onSuccess = { Result.success(it) },
			onFailure = { Result.failure(it) }
		)
	}

	suspend fun createCategory(
		name: String,
		type: String,
		businessId: String?,
		subBusinessId: String?
	): Result<Unit> = withContext(Dispatchers.IO) {
		require(!(businessId == null && subBusinessId == null)) { "Either businessId or subBusinessId must be provided" }
		runCatching {
			val body = buildMap<String, Any?> {
				put("name", name)
				put("type", type)
				put("business_id", businessId)
				put("sub_business_id", subBusinessId)
			}
			SupabaseProvider.client.postgrest["categories"].insert(body)
		}.fold(
			onSuccess = { Result.success(Unit) },
			onFailure = { Result.failure(it) }
		)
	}

	suspend fun updateCategoryName(id: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
		runCatching {
			SupabaseProvider.client.postgrest["categories"].update(mapOf("name" to name)) {
				filter { eq("id", id) }
			}
		}.fold(
			onSuccess = { Result.success(Unit) },
			onFailure = { Result.failure(it) }
		)
	}

	suspend fun deleteCategory(id: String): Result<Unit> = withContext(Dispatchers.IO) {
		runCatching {
			SupabaseProvider.client.postgrest["categories"].delete {
				filter { eq("id", id) }
			}
		}.fold(
			onSuccess = { Result.success(Unit) },
			onFailure = { Result.failure(it) }
		)
	}

	/** Create a short-lived signed URL for a private logo path */
	suspend fun createLogoSignedUrl(path: String, expiresInSeconds: Int = 3600): Result<String> = withContext(Dispatchers.IO) {
		runCatching {
			SupabaseProvider.client.storage.from("logos").createSignedUrl(path, expiresInSeconds.seconds)
		}.fold(
			onSuccess = { Result.success(it) },
			onFailure = { Result.failure(it) }
		)
	}
}
