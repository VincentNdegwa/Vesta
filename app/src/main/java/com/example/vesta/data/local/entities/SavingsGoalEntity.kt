package com.example.vesta.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.util.UUID

enum class ContributionFrequency {
    DAILY, WEEKLY, MONTHLY, CUSTOM
}

enum class GoalStatus {
    ACTIVE, COMPLETED, PAUSED
}

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val deadline: Long,
    val startDate: Long = System.currentTimeMillis(),
    val priority: Int = 2, // 1: High, 2: Medium, 3: Low
    val categoryId: String? = null,
    val contributionFrequency: ContributionFrequency = ContributionFrequency.MONTHLY,
    val autoContribute: Boolean = false,
    val autoContributeAmount: Double? = null,
    val autoContributePercentage: Double? = null,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val achievedMilestones: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

// Room type converters
class ContributionFrequencyConverter {
    @TypeConverter
    fun fromContributionFrequency(frequency: ContributionFrequency): String = frequency.name

    @TypeConverter
    fun toContributionFrequency(value: String): ContributionFrequency = 
        ContributionFrequency.valueOf(value)
}

class GoalStatusConverter {
    @TypeConverter
    fun fromGoalStatus(status: GoalStatus): String = status.name

    @TypeConverter
    fun toGoalStatus(value: String): GoalStatus = GoalStatus.valueOf(value)
}

// Using the main Converters class for List<String> conversion
