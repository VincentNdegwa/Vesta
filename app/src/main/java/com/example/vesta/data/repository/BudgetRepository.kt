package com.example.vesta.data.repository

import com.example.vesta.data.local.FinvestaDatabase
import com.example.vesta.data.local.entities.BudgetEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val database: FinvestaDatabase
) {
    private val budgetDao = database.budgetDao()

    fun getBudgetsFlow(userId: String): Flow<List<BudgetEntity>> =
        budgetDao.getBudgetsFlow(userId)

    fun getCurrentPeriodBudgetsFlow(userId: String, now: Long) =
        budgetDao.getCurrentPeriodBudgetsFlow(userId, now)

    suspend fun getCurrentPeriodBudgets(userId: String, now: Long) =
        budgetDao.getCurrentPeriodBudgets(userId, now)

    suspend fun getBudgets(userId: String): List<BudgetEntity> =
        budgetDao.getBudgets(userId)

    suspend fun getBudget(id: String): BudgetEntity? =
        budgetDao.getBudget(id)

    suspend fun insertBudget(budget: BudgetEntity) =
        budgetDao.insertBudget(budget)

    suspend fun updateBudget(budget: BudgetEntity) =
        budgetDao.updateBudget(budget)

    suspend fun updateSpentAmount(id: String, spentAmount: Double) =
        budgetDao.updateSpentAmount(id, spentAmount, System.currentTimeMillis())

    suspend fun deleteBudget(id: String) =
        budgetDao.hardDeleteBudget(id)

    suspend fun getUnsyncedBudgets(): List<BudgetEntity> =
        budgetDao.getUnsyncedBudgets()

    suspend fun markAsSynced(ids: List<String>) =
        budgetDao.markAsSynced(ids)

    suspend fun getBudgetsByCategory(userId: String, categoryId: String): List<BudgetEntity> =
        budgetDao.getBudgetsByCategory(userId, categoryId)

    suspend fun findActiveBudgetsForCategoryId(userId: String, categoryId: String, date: Long): List<BudgetEntity> {
        val budgets = getBudgetsByCategory(userId, categoryId)
        return budgets.filter { b ->
            b.isActive && date in b.startDate..b.endDate
        }
    }

    suspend fun addExpenseToBudgetByCategoryId(userId: String, categoryId: String, amount: Double, date: Long) {
        val budgets = findActiveBudgetsForCategoryId(userId, categoryId, date)
        budgets.forEach { budget ->
            val newSpent = (budget.spentAmount ?: 0.0) + amount
            updateSpentAmount(budget.id, newSpent)
        }
    }
}
