package com.example.vesta.data.local.dao

import androidx.room.*
import com.example.vesta.data.local.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    fun getTransactionsFlow(userId: String): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    suspend fun getTransactions(userId: String): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransaction(id: String): TransactionEntity?
    
    @Query("SELECT * FROM transactions WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getTransactionsByDateRange(userId: String, startDate: Long, endDate: Long): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE userId = :userId AND category = :category ORDER BY date DESC")
    suspend fun getTransactionsByCategory(userId: String, category: String): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE userId = :userId AND type = :type ORDER BY date DESC")
    suspend fun getTransactionsByType(userId: String, type: String): List<TransactionEntity>
    
    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = 'income' AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalIncomeForPeriod(userId: String, startDate: Long, endDate: Long): Double?
    
    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = 'expense' AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalExpenseForPeriod(userId: String, startDate: Long, endDate: Long): Double?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)
    
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)
    
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String)
    
    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>
    
    @Query("UPDATE transactions SET isSynced = 1, updatedAt = :syncTime WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>, syncTime: Long)
    
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
