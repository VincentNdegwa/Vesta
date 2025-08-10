package com.example.vesta.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class RecurrenceType {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM
}

@Entity(tableName = "bill_reminders")
data class BillReminderEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val amount: Double,
    val categoryId: String, // Using category ID instead of name
//    val categoryName: String? = null,
    val dueDate: Long, // First due date
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val intervalCount: Int = 1, // e.g., every 2 weeks
    val timesPerPeriod: Int? = null, // e.g., 3 times in a week
    val isPaid: Boolean = false,
    val lastPaidDate: Long? = null,
    val nextDueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
