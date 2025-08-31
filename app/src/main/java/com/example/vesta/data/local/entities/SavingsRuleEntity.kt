package com.example.vesta.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

enum class RuleType {
    PERCENTAGE_OF_INCOME,      // e.g., 10% of every income
    FIXED_AMOUNT,             // e.g., $200 from each salary
    ROUND_UP,                 // Round up expenses and save difference
    SMART_SAVE               // AI-suggested amount based on spending patterns
}

enum class RuleFrequency {
    EVERY_INCOME,           // Apply on every income transaction
    DAILY,                  // Daily fixed contribution
    WEEKLY,                // Weekly fixed contribution
    MONTHLY,               // Monthly fixed contribution
    ON_EXPENSE             // For round-up rules
}

@Entity(
    tableName = "savings_rules",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index("goalId") // Add index for foreign key
    ]
)
data class SavingsRuleEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val goalId: String,
    val type: RuleType,
    val frequency: RuleFrequency,
    val amount: Double? = null,          // For FIXED_AMOUNT
    val percentage: Double? = null,      // For PERCENTAGE_OF_INCOME
    val isEnabled: Boolean = true,
    val lastExecuted: Long? = null,
    val nextScheduled: Long? = null,
    val minimumIncomeThreshold: Double? = null,  // Only apply rule if income is above this
    val maximumContribution: Double? = null,     // Cap the contribution at this amount
    val description: String,                     // Human-readable description of the rule
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
