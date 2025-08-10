package com.example.vesta.data.repository

import com.example.vesta.data.local.FinvestaDatabase
import com.example.vesta.data.local.dao.BillReminderDao
import com.example.vesta.data.local.entities.BillReminderEntity
import com.example.vesta.data.local.entities.RecurrenceType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillReminderRepository @Inject constructor(
    private val database: FinvestaDatabase
) {
    private val billReminderDao = database.billReminderDao()

    fun getBillRemindersFlow(userId: String): Flow<List<BillReminderEntity>> {
        return billReminderDao.getBillRemindersFlow(userId)
    }

    suspend fun getActiveBillRemindersFlow(userId: String): List<BillReminderEntity> {
        return billReminderDao.getBillReminders(userId)
    }

    suspend fun addBillReminder(billReminder: BillReminderEntity): Result<Unit> {
        return try {
            // Calculate next due date if it's a recurring bill
            val reminderWithNextDueDate = if (billReminder.recurrenceType != RecurrenceType.NONE) {
                billReminder.copy(nextDueDate = calculateNextDueDate(billReminder))
            } else {
                billReminder
            }

            billReminderDao.insertBillReminder(reminderWithNextDueDate)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAsPaid(billId: String): Result<Unit> {
        return try {
            val bill = billReminderDao.getBillReminder(billId) ?: 
                return Result.failure(Exception("Bill reminder not found"))
            
            val now = System.currentTimeMillis()
            
            // Mark as paid
            billReminderDao.markAsPaid(billId, now)
            
            // If recurring, create next due date
            if (bill.recurrenceType != RecurrenceType.NONE) {
                val updatedBill = bill.copy(
                    isPaid = true,
                    lastPaidDate = now,
                    nextDueDate = calculateNextDueDate(bill, now)
                )
                billReminderDao.updateBillReminder(updatedBill)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBillReminder(billId: String): Result<Unit> {
        return try {
            billReminderDao.hardDeleteBillReminder(billId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calculate the next due date based on recurrence type, interval count, and times per period
     */
    private fun calculateNextDueDate(bill: BillReminderEntity, fromDate: Long? = null): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = fromDate ?: bill.dueDate
        
        when (bill.recurrenceType) {
            RecurrenceType.DAILY -> {
                calendar.add(Calendar.DAY_OF_MONTH, bill.intervalCount)
            }
            RecurrenceType.WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, bill.intervalCount)
                
                // Handle multiple times per week
                if (bill.timesPerPeriod != null && bill.timesPerPeriod > 1) {
                    // For simplicity, we'll just use the first occurrence in the calculation
                    // In a real app, you'd need to track which occurrence within the week this is
                }
            }
            RecurrenceType.MONTHLY -> {
                calendar.add(Calendar.MONTH, bill.intervalCount)
                
                // Handle multiple times per month
                if (bill.timesPerPeriod != null && bill.timesPerPeriod > 1) {
                    // For simplicity, we'll just use the first occurrence in the calculation
                }
            }
            RecurrenceType.YEARLY -> {
                calendar.add(Calendar.YEAR, bill.intervalCount)
            }
            RecurrenceType.CUSTOM -> {
                // Custom recurrence would need specific logic based on your requirements
            }
            RecurrenceType.NONE -> {
                // No recurrence, so no next due date
                return 0
            }
        }
        
        return calendar.timeInMillis
    }

    /**
     * For bills with multiple occurrences in a period, calculate all due dates within the period
     */
    fun calculateMultipleOccurrences(bill: BillReminderEntity): List<Long> {
        if (bill.timesPerPeriod == null || bill.timesPerPeriod <= 1) {
            return listOf(bill.dueDate)
        }
        
        val result = mutableListOf<Long>()
        result.add(bill.dueDate) // First occurrence is the specified due date
        
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = bill.dueDate
        
        val periodDurationMillis = when (bill.recurrenceType) {
            RecurrenceType.DAILY -> 24 * 60 * 60 * 1000L // 1 day in milliseconds
            RecurrenceType.WEEKLY -> 7 * 24 * 60 * 60 * 1000L // 1 week in milliseconds
            RecurrenceType.MONTHLY -> {
                // Approximate a month as 30 days
                30 * 24 * 60 * 60 * 1000L
            }
            RecurrenceType.YEARLY -> {
                // Approximate a year as 365 days
                365 * 24 * 60 * 60 * 1000L
            }
            else -> return listOf(bill.dueDate) // Only one occurrence for other types
        }
        
        // Calculate time interval between occurrences
        val intervalMillis = periodDurationMillis / bill.timesPerPeriod
        
        // Calculate remaining occurrences
        for (i in 1 until bill.timesPerPeriod) {
            val nextOccurrence = bill.dueDate + (i * intervalMillis)
            result.add(nextOccurrence)
        }
        
        return result
    }
}
