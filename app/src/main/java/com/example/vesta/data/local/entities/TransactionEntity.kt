package com.example.vesta.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val amount: Double,
    val type: String, // INCOME, EXPENSE, TRANSFER
    val category: String,
    val subcategory: String?,
    val description: String?,
    val notes: String?,
    val date: Instant,
    val accountId: String?,
    val paymentMethod: String?, // CASH, CARD, BANK_TRANSFER, DIGITAL_WALLET
    val location: String?,
    val receiptUrl: String?,
    val tags: List<String> = emptyList(),
    val recurringId: String?, // For recurring transactions
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastSyncedAt: Instant?,
    val isDeleted: Boolean = false,
    val needsSync: Boolean = false
)
