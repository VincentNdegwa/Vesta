package com.example.vesta.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.util.UUID

enum class BudgetPeriod { DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM }

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val category: String, // Should match Transaction.category
    val targetAmount: Double = 0.0,
    val spentAmount: Double = 0.0,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val startDate: Long,
    val endDate: Long,
    val resetOn: Long? = null, // Optional custom reset date
    val lastCalculated: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val isSynced: Boolean = false
)

// Room type converters for BudgetPeriod
class BudgetPeriodConverter {
    @TypeConverter
    fun fromBudgetPeriod(period: BudgetPeriod): String = period.name
    @TypeConverter
    fun toBudgetPeriod(period: String): BudgetPeriod = BudgetPeriod.valueOf(period)
}
