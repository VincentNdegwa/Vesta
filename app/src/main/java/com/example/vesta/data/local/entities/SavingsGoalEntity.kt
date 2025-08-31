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
    val description: String? = null,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val deadline: Long,
    val startDate: Long = System.currentTimeMillis(),
    val priority: Int = 2, // 1: High, 2: Medium, 3: Low
    val categoryId: String? = null,
    val status: GoalStatus = GoalStatus.ACTIVE,
    
    // Progress tracking
    val lastContributionDate: Long? = null,
    val totalContributions: Int = 0,
    val consistentContributionStreak: Int = 0,
    val achievedMilestones: List<String> = emptyList(),
    val nextMilestone: String? = null,
    
    // Smart features
    val suggestedMonthlyAmount: Double? = null,
    val sustainabilityScore: Int? = null, // 0-100, indicates how sustainable the goal is based on income/expenses
    val riskLevel: Int = 2, // 1: Low, 2: Medium, 3: High - Based on deadline proximity and progress
    
    // Achievement system
    val achievements: List<String> = emptyList(),
    val streakAchievements: List<String> = emptyList(),
    val milestoneAchievements: List<String> = emptyList(),
    
    // Auto-contribution settings
    val autoContribute: Boolean = false,
    val autoContributeAmount: Double? = null,
    val autoContributePercentage: Double? = null,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    
    // Analysis
    val averageMonthlyContribution: Double? = null,
    val projectedCompletionDate: Long? = null,
    val progressRate: Double? = null, // Actual progress rate vs required rate
    val nextSuggestedContribution: Double? = null
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
