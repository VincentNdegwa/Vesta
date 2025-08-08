package com.example.vesta.data.local.dao

import androidx.room.*
import com.example.vesta.data.local.entities.BillReminderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface BillReminderDao {
    
    @Query("SELECT * FROM bill_reminders WHERE userId = :userId ORDER BY dueDate ASC")
    fun getBillRemindersFlow(userId: String): Flow<List<BillReminderEntity>>

    @Query("SELECT * FROM bill_reminders WHERE userId = :userId ORDER BY dueDate ASC")
    suspend fun getBillReminders(userId: String): List<BillReminderEntity>

    @Query("SELECT * FROM bill_reminders WHERE id = :id")
    suspend fun getBillReminder(id: String): BillReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillReminder(billReminder: BillReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillReminders(billReminders: List<BillReminderEntity>)

    @Update
    suspend fun updateBillReminder(billReminder: BillReminderEntity)

    @Query("UPDATE bill_reminders SET isPaid = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markAsPaid(id: String, updatedAt: Long)
    
    @Query("UPDATE bill_reminders SET isDeleted = 1, updatedAt = :deletedAt, needsSync = 1 WHERE id = :id")
    suspend fun softDeleteBillReminder(id: String, deletedAt: Instant)
    
    @Query("DELETE FROM bill_reminders WHERE id = :id")
    suspend fun hardDeleteBillReminder(id: String)
    
    @Query("SELECT * FROM bill_reminders WHERE needsSync = 1")
    suspend fun getUnsyncedBillReminders(): List<BillReminderEntity>
    
    @Query("UPDATE bill_reminders SET needsSync = 0, lastSyncedAt = :syncTime WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>, syncTime: Instant)
}
