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
        val existingCategories = getCategories(userId)
        
        // Create sets of existing category names by type for easy lookup
        val existingExpenseCategoryNames = existingCategories
            .filter { it.type.equals("EXPENSE", ignoreCase = true) && it.isSystem }
            .map { it.name.lowercase() }
            .toSet()
            
        val existingIncomeCategoryNames = existingCategories
            .filter { it.type.equals("INCOME", ignoreCase = true) && it.isSystem }
            .map { it.name.lowercase() }
            .toSet()
        
        val now = System.currentTimeMillis()
        
        // Only add default expense categories that don't already exist
        val newExpenseCategories = com.example.vesta.data.local.entities.DefaultExpenseCategories
            .filterNot { it.lowercase() in existingExpenseCategoryNames }
            .map {
                CategoryEntity(
                    userId = userId, 
                    name = it, 
                    type = "EXPENSE", 
                    createdAt = now, 
                    updatedAt = now, 
                    isSystem = true
                )
            }
            
        // Only add default income categories that don't already exist
        val newIncomeCategories = com.example.vesta.data.local.entities.DefaultIncomeCategories
            .filterNot { it.lowercase() in existingIncomeCategoryNames }
            .map {
                CategoryEntity(
                    userId = userId, 
                    name = it, 
                    type = "INCOME", 
                    createdAt = now, 
                    updatedAt = now, 
                    isSystem = true
                )
            }
        
        // Only insert if we have new categories to add
        if (newExpenseCategories.isNotEmpty() || newIncomeCategories.isNotEmpty()) {
            categoryDao.insertCategories(newExpenseCategories + newIncomeCategories)
        }
    }
}
