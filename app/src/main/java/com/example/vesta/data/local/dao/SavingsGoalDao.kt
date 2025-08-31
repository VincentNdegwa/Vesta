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

    @Query("UPDATE savings_goals SET currentAmount = currentAmount + :amount, updatedAt = :timestamp WHERE id = :goalId")
    suspend fun updateGoalProgress(goalId: String, amount: Double, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM savings_goals WHERE userId = :userId AND autoContribute = 1")
    suspend fun getAutoContributeGoals(userId: String): List<SavingsGoalEntity>

    @Query("UPDATE savings_goals SET achievedMilestones = :milestones WHERE id = :goalId")
    suspend fun updateAchievedMilestones(goalId: String, milestones: List<String>)
}
