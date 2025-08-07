package com.example.vesta.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "bill_reminders")
data class BillReminderEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val description: String?,
    val amount: Double,
    val category: String,
    val dueDate: Instant,
    val reminderDays: List<Int> = listOf(3, 1), // Remind 3 days and 1 day before
    val isRecurring: Boolean = false,
    val recurringPeriod: String?, // WEEKLY, MONTHLY, YEARLY
    val paymentUrl: String?, // Link to pay the bill
    val companyName: String?,
    val accountNumber: String?,
    val isPaid: Boolean = false,
    val paidAt: Instant?,
    val paidAmount: Double?,
    val nextDueDate: Instant?, // For recurring bills
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastSyncedAt: Instant?,
    val isDeleted: Boolean = false,
    val needsSync: Boolean = false
)
