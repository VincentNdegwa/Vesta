package com.example.vesta.data.local.dao

import androidx.room.*
import com.example.vesta.data.local.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface CategoryDao {
    
    @Query("SELECT * FROM categories WHERE userId = :userId AND isDeleted = 0 ORDER BY sortOrder ASC, name ASC")
    fun getCategoriesFlow(userId: String): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE userId = :userId AND isDeleted = 0 ORDER BY sortOrder ASC, name ASC")
    suspend fun getCategories(userId: String): List<CategoryEntity>
    
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategory(id: String): CategoryEntity?
    
    @Query("SELECT * FROM categories WHERE userId = :userId AND type = :type AND isActive = 1 AND isDeleted = 0 ORDER BY sortOrder ASC, name ASC")
    suspend fun getCategoriesByType(userId: String, type: String): List<CategoryEntity>
    
    @Query("SELECT * FROM categories WHERE userId = :userId AND parentCategoryId = :parentId AND isDeleted = 0 ORDER BY sortOrder ASC, name ASC")
    suspend fun getSubcategories(userId: String, parentId: String): List<CategoryEntity>
    
    @Query("SELECT * FROM categories WHERE isDefault = 1 AND isDeleted = 0")
    suspend fun getDefaultCategories(): List<CategoryEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)
    
    @Update
    suspend fun updateCategory(category: CategoryEntity)
    
    @Query("UPDATE categories SET isDeleted = 1, updatedAt = :deletedAt, needsSync = 1 WHERE id = :id")
    suspend fun softDeleteCategory(id: String, deletedAt: Instant)
    
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun hardDeleteCategory(id: String)
    
    @Query("SELECT * FROM categories WHERE needsSync = 1")
    suspend fun getUnsyncedCategories(): List<CategoryEntity>
    
    @Query("UPDATE categories SET needsSync = 0, lastSyncedAt = :syncTime WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>, syncTime: Instant)
}
