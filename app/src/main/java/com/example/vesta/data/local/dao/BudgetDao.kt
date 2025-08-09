package com.example.vesta.data.local.dao

import androidx.room.*
import com.example.vesta.data.local.entities.BudgetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE userId = :userId AND startDate <= :now AND endDate >= :now ORDER BY createdAt DESC")
    fun getCurrentPeriodBudgetsFlow(userId: String, now: Long): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND startDate <= :now AND endDate >= :now ORDER BY createdAt DESC")
    suspend fun getCurrentPeriodBudgets(userId: String, now: Long): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE userId = :userId ORDER BY createdAt DESC")
    fun getBudgetsFlow(userId: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getBudgets(userId: String): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudget(id: String): BudgetEntity?

    // Get budgets by categoryId
    @Query("SELECT * FROM budgets WHERE userId = :userId AND categoryId = :categoryId ORDER BY createdAt DESC")
    suspend fun getBudgetsByCategory(userId: String, categoryId: String): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Query("UPDATE budgets SET spentAmount = :spentAmount, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSpentAmount(id: String, spentAmount: Double, updatedAt: Long)
    
    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun hardDeleteBudget(id: String)
    
    @Query("SELECT * FROM budgets WHERE isSynced = 0")
    suspend fun getUnsyncedBudgets(): List<BudgetEntity>

    @Query("UPDATE budgets SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}
