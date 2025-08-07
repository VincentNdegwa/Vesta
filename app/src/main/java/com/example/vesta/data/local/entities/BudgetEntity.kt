package com.example.vesta.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val category: String,
    val targetAmount: Double,
    val spentAmount: Double = 0.0,
    val period: String, // WEEKLY, MONTHLY, YEARLY, CUSTOM
    val startDate: Instant,
    val endDate: Instant,
    val alertThreshold: Double = 0.8, // Alert when 80% of budget is used
    val isActive: Boolean = true,
    val color: String?, // Hex color for UI representation
    val icon: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastSyncedAt: Instant?,
    val isDeleted: Boolean = false,
    val needsSync: Boolean = false
)
