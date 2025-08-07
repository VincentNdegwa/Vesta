package com.example.vesta.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val description: String?,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDate: Instant?,
    val category: String?,
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH
    val isActive: Boolean = true,
    val isCompleted: Boolean = false,
    val completedAt: Instant?,
    val reminderEnabled: Boolean = true,
    val reminderFrequency: String = "WEEKLY", // DAILY, WEEKLY, MONTHLY
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastSyncedAt: Instant?,
    val isDeleted: Boolean = false,
    val needsSync: Boolean = false
)
