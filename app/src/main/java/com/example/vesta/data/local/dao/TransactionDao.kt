package com.example.vesta.data.local.dao

import androidx.room.*
import com.example.vesta.data.local.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions WHERE userId = :userId AND isDeleted = 0 ORDER BY date DESC")
    fun getTransactionsFlow(userId: String): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE userId = :userId AND isDeleted = 0 ORDER BY date DESC")
    suspend fun getTransactions(userId: String): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransaction(id: String): TransactionEntity?
    
    @Query("SELECT * FROM transactions WHERE userId = :userId AND date BETWEEN :startDate AND :endDate AND isDeleted = 0 ORDER BY date DESC")
    suspend fun getTransactionsByDateRange(userId: String, startDate: Instant, endDate: Instant): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE userId = :userId AND category = :category AND isDeleted = 0 ORDER BY date DESC")
    suspend fun getTransactionsByCategory(userId: String, category: String): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE userId = :userId AND type = :type AND isDeleted = 0 ORDER BY date DESC")
    suspend fun getTransactionsByType(userId: String, type: String): List<TransactionEntity>
    
    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = 'INCOME' AND date BETWEEN :startDate AND :endDate AND isDeleted = 0")
    suspend fun getTotalIncomeForPeriod(userId: String, startDate: Instant, endDate: Instant): Double?
    
    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = 'EXPENSE' AND date BETWEEN :startDate AND :endDate AND isDeleted = 0")
    suspend fun getTotalExpenseForPeriod(userId: String, startDate: Instant, endDate: Instant): Double?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)
    
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)
    
    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :deletedAt, needsSync = 1 WHERE id = :id")
    suspend fun softDeleteTransaction(id: String, deletedAt: Instant)
    
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun hardDeleteTransaction(id: String)
    
    @Query("SELECT * FROM transactions WHERE needsSync = 1")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>
    
    @Query("UPDATE transactions SET needsSync = 0, lastSyncedAt = :syncTime WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>, syncTime: Instant)
}
