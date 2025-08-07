package com.example.vesta.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val type: String, // INCOME, EXPENSE
    val parentCategoryId: String?, // For subcategories
    val color: String?,
    val icon: String?,
    val isDefault: Boolean = false, // System default categories
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastSyncedAt: Instant?,
    val isDeleted: Boolean = false,
    val needsSync: Boolean = false
)
