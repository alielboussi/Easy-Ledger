package com.easyledger.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val email: String?,
    val displayName: String?
)

@Serializable
data class Business(
    val id: String,
    val ownerId: String,
    val name: String,
    val logoUrl: String?,
    val primaryCurrency: String,
    val secondaryCurrency: String? = null
)

@Serializable
data class SubBusiness(
    val id: String,
    val businessId: String,
    val name: String
)

@Serializable
data class Category(
    val id: String,
    val businessId: String?, // null if tied to subBusiness only
    val subBusinessId: String?,
    val name: String,
    val type: String // "income" or "expense"
)

@Serializable
data class Transaction(
    val id: String,
    val ownerId: String,
    val businessId: String,
    val subBusinessId: String?,
    val categoryId: String,
    val amount: Double,
    val currency: String,
    val type: String, // "income" or "expense"
    val occurredAt: String // ISO date
)

@Serializable
data class Rollup(
    val businessId: String,
    val currency: String,
    val totalIncome: Double,
    val totalExpense: Double
)
