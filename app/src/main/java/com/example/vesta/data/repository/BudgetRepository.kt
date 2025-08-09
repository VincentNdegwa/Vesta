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

    suspend fun findActiveBudgetForCategory(userId: String, category: String, date: Long): BudgetEntity? {
        val budgets = getBudgets(userId)
        return budgets.firstOrNull { b ->
            b.category.equals(category, ignoreCase = true)
                && b.isActive
                && date in b.startDate..b.endDate
        }
    }

    suspend fun addExpenseToBudget(userId: String, category: String, amount: Double, date: Long) {
        val budget = findActiveBudgetForCategory(userId, category, date)
        if (budget != null) {
            val newSpent = (budget.spentAmount ?: 0.0) + amount
            updateSpentAmount(budget.id, newSpent)
        }
    }
}
