package com.example.vesta.data.local.dao

import androidx.room.*
import com.example.vesta.data.local.entities.GoalEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface GoalDao {
    
    @Query("SELECT * FROM goals WHERE userId = :userId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getGoalsFlow(userId: String): Flow<List<GoalEntity>>
    
    @Query("SELECT * FROM goals WHERE userId = :userId AND isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getGoals(userId: String): List<GoalEntity>
    
    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoal(id: String): GoalEntity?
    
    @Query("SELECT * FROM goals WHERE userId = :userId AND isActive = 1 AND isCompleted = 0 AND isDeleted = 0 ORDER BY priority DESC, targetDate ASC")
    suspend fun getActiveGoals(userId: String): List<GoalEntity>
    
    @Query("SELECT * FROM goals WHERE userId = :userId AND isCompleted = 1 AND isDeleted = 0 ORDER BY completedAt DESC")
    suspend fun getCompletedGoals(userId: String): List<GoalEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<GoalEntity>)
    
    @Update
    suspend fun updateGoal(goal: GoalEntity)
    
    @Query("UPDATE goals SET currentAmount = :currentAmount, updatedAt = :updatedAt, needsSync = 1 WHERE id = :id")
    suspend fun updateProgress(id: String, currentAmount: Double, updatedAt: Instant)
    
    @Query("UPDATE goals SET isCompleted = 1, completedAt = :completedAt, updatedAt = :updatedAt, needsSync = 1 WHERE id = :id")
    suspend fun markAsCompleted(id: String, completedAt: Instant, updatedAt: Instant)
    
    @Query("UPDATE goals SET isDeleted = 1, updatedAt = :deletedAt, needsSync = 1 WHERE id = :id")
    suspend fun softDeleteGoal(id: String, deletedAt: Instant)
    
    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun hardDeleteGoal(id: String)
    
    @Query("SELECT * FROM goals WHERE needsSync = 1")
    suspend fun getUnsyncedGoals(): List<GoalEntity>
    
    @Query("UPDATE goals SET needsSync = 0, lastSyncedAt = :syncTime WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>, syncTime: Instant)
}
