package com.example.vesta.data.repository

import com.example.vesta.data.local.FinvestaDatabase
import com.example.vesta.data.local.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val database: FinvestaDatabase
) {
    private val categoryDao = database.categoryDao()

    fun getCategoriesFlow(userId: String): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesFlow(userId)

    suspend fun getCategories(userId: String): List<CategoryEntity> =
        categoryDao.getCategories(userId)

    suspend fun getCategoriesByType(userId: String, type: String): List<CategoryEntity> =
        getCategories(userId).filter { it.type.equals(type, ignoreCase = true) }

    suspend fun insertDefaultCategoriesIfNone(userId: String) {
        if (categoryDao.getCategoryCountForUser(userId) == 0) {
            val now = System.currentTimeMillis()
            val expense = com.example.vesta.data.local.entities.DefaultExpenseCategories.map {
                CategoryEntity(userId = userId, name = it, type = "EXPENSE", createdAt = now, updatedAt = now, isSystem=true)
            }
            val income = com.example.vesta.data.local.entities.DefaultIncomeCategories.map {
                CategoryEntity(userId = userId, name = it, type = "INCOME", createdAt = now, updatedAt = now, isSystem = true)
            }
            categoryDao.insertCategories(expense + income)
        }
    }
}
