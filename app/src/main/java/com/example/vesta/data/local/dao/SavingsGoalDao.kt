package com.example.vesta.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.vesta.data.local.entities.SavingsGoalEntity
import com.example.vesta.data.local.entities.GoalStatus

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals WHERE userId = :userId AND status = :status ORDER BY priority ASC, createdAt DESC")
    fun getSavingsGoalsByStatus(userId: String, status: GoalStatus): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllSavingsGoals(userId: String): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :goalId")
    fun getSavingsGoalById(goalId: String): Flow<SavingsGoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoalEntity)

    @Update
    suspend fun updateSavingsGoal(goal: SavingsGoalEntity)

    @Delete
    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity)

    // Progress tracking
    @Query("""
        UPDATE savings_goals 
        SET currentAmount = currentAmount + :amount,
            lastContributionDate = :timestamp,
            totalContributions = totalContributions + 1,
            updatedAt = :timestamp
        WHERE id = :goalId
    """)
    suspend fun updateGoalProgress(goalId: String, amount: Double, timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE savings_goals 
        SET consistentContributionStreak = 
            CASE 
                WHEN ((:timestamp - lastContributionDate) <= 2592000000) THEN consistentContributionStreak + 1
                ELSE 1
            END,
        lastContributionDate = :timestamp
        WHERE id = :goalId
    """)
    suspend fun updateContributionStreak(goalId: String, timestamp: Long = System.currentTimeMillis())

    // Achievement system
    @Query("UPDATE savings_goals SET achievements = :achievements WHERE id = :goalId")
    suspend fun updateAchievements(goalId: String, achievements: List<String>)

    @Query("UPDATE savings_goals SET streakAchievements = :achievements WHERE id = :goalId")
    suspend fun updateStreakAchievements(goalId: String, achievements: List<String>)

    @Query("UPDATE savings_goals SET milestoneAchievements = :achievements WHERE id = :goalId")
    suspend fun updateMilestoneAchievements(goalId: String, achievements: List<String>)

    @Query("UPDATE savings_goals SET achievedMilestones = :milestones, nextMilestone = :nextMilestone WHERE id = :goalId")
    suspend fun updateMilestones(goalId: String, milestones: List<String>, nextMilestone: String?)

    // Smart features
    @Query("""
        UPDATE savings_goals 
        SET sustainabilityScore = :score,
            suggestedMonthlyAmount = :suggestedAmount,
            riskLevel = :riskLevel,
            projectedCompletionDate = :projectedDate,
            progressRate = :progressRate,
            nextSuggestedContribution = :nextContribution,
            averageMonthlyContribution = :avgContribution
        WHERE id = :goalId
    """)
    suspend fun updateSmartMetrics(
        goalId: String,
        score: Int,
        suggestedAmount: Double,
        riskLevel: Int,
        projectedDate: Long,
        progressRate: Double,
        nextContribution: Double,
        avgContribution: Double
    )

    // Analysis queries
    @Query("""
        SELECT * FROM savings_goals 
        WHERE userId = :userId 
        AND status = 'ACTIVE'
        AND (currentAmount / targetAmount) < 
            ((strftime('%s','now') - startDate) / (deadline - startDate))
    """)
    fun getBehindScheduleGoals(userId: String): Flow<List<SavingsGoalEntity>>

    @Query("""
        SELECT * FROM savings_goals 
        WHERE userId = :userId 
        AND status = 'ACTIVE'
        AND deadline < :timestamp
    """)
    fun getOverdueGoals(userId: String, timestamp: Long = System.currentTimeMillis()): Flow<List<SavingsGoalEntity>>

    @Query("""
        SELECT * FROM savings_goals 
        WHERE userId = :userId 
        AND status = 'ACTIVE'
        AND consistentContributionStreak >= 3
    """)
    fun getConsistentGoals(userId: String): Flow<List<SavingsGoalEntity>>

    @Query("UPDATE savings_goals SET achievedMilestones = :milestones WHERE id = :goalId")
    suspend fun updateAchievedMilestones(goalId: String, milestones: List<String>)

    @Query("""
        SELECT * FROM savings_goals 
        WHERE userId = :userId 
        AND status = 'ACTIVE' 
        AND (autoContributeAmount IS NOT NULL OR autoContributePercentage IS NOT NULL)
    """)
    fun getAutoContributeGoals(userId: String): Flow<List<SavingsGoalEntity>>
}
