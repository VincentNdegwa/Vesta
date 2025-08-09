package com.example.vesta.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val type: String, // INCOME, EXPENSE
    val color: String? = null,
    val icon: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSystem: Boolean = false,
    val isSynced: Boolean = false
)

val DefaultExpenseCategories = listOf(
    "Food & Dining",
    "Transportation",
    "Shopping",
    "Entertainment",
    "Bills & Utilities",
    "Healthcare",
    "Travel",
    "Education",
    "Groceries",
    "Other"
)

val DefaultIncomeCategories = listOf(
    "Salary",
    "Freelance",
    "Investment",
    "Business",
    "Gift",
    "Other"
)
