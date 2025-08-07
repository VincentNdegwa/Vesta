package com.example.vesta.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val type: String, // CHECKING, SAVINGS, CREDIT_CARD, CASH, INVESTMENT
    val balance: Double,
    val currency: String = "USD",
    val bankName: String?,
    val accountNumber: String?, // Last 4 digits only for security
    val color: String?,
    val icon: String?,
    val isActive: Boolean = true,
    val isIncludeInTotal: Boolean = true,
    val creditLimit: Double?, // For credit cards
    val interestRate: Double?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastSyncedAt: Instant?,
    val isDeleted: Boolean = false,
    val needsSync: Boolean = false
)
