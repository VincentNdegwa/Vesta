package com.example.vesta.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index // <-- Import Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "savings_contributions",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["goalId"])]
)
data class SavingsContributionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val goalId: String, // This is the foreign key column that needs indexing
    val userId: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val transactionId: String? = null,
    val type: String, // MANUAL, AUTO, MILESTONE_REWARD
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)