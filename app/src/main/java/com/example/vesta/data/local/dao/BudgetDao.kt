package com.example.vesta.data.local.dao

import androidx.room.*
import com.example.vesta.data.local.entities.BudgetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface BudgetDao {
    
    @Query("SELECT * FROM budgets WHERE userId = :userId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getBudgetsFlow(userId: String): Flow<List<BudgetEntity>>
    
    @Query("SELECT * FROM budgets WHERE userId = :userId AND isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getBudgets(userId: String): List<BudgetEntity>
    
    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudget(id: String): BudgetEntity?
    
    @Query("SELECT * FROM budgets WHERE userId = :userId AND isActive = 1 AND isDeleted = 0 AND startDate <= :currentDate AND endDate >= :currentDate")
    suspend fun getActiveBudgets(userId: String, currentDate: Instant): List<BudgetEntity>
    
    @Query("SELECT * FROM budgets WHERE userId = :userId AND category = :category AND isActive = 1 AND isDeleted = 0 AND startDate <= :currentDate AND endDate >= :currentDate LIMIT 1")
    suspend fun getActiveBudgetForCategory(userId: String, category: String, currentDate: Instant): BudgetEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)
    
    @Update
    suspend fun updateBudget(budget: BudgetEntity)
    
    @Query("UPDATE budgets SET spentAmount = :spentAmount, updatedAt = :updatedAt, needsSync = 1 WHERE id = :id")
    suspend fun updateSpentAmount(id: String, spentAmount: Double, updatedAt: Instant)
    
    @Query("UPDATE budgets SET isDeleted = 1, updatedAt = :deletedAt, needsSync = 1 WHERE id = :id")
    suspend fun softDeleteBudget(id: String, deletedAt: Instant)
    
    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun hardDeleteBudget(id: String)
    
    @Query("SELECT * FROM budgets WHERE needsSync = 1")
    suspend fun getUnsyncedBudgets(): List<BudgetEntity>
    
    @Query("UPDATE budgets SET needsSync = 0, lastSyncedAt = :syncTime WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>, syncTime: Instant)
}
