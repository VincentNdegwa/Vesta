package com.example.vesta.data.local.dao

import androidx.room.*
import com.example.vesta.data.local.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    fun getTransactionsFlow(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransaction(id: String): TransactionEntity?
    
    @Query("SELECT * FROM transactions WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getTransactionsByDateRange(userId: String, startDate: Long, endDate: Long): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE userId = :userId AND categoryId = :categoryId ORDER BY date DESC")
    suspend fun getTransactionsByCategory(userId: String, categoryId: String): List<TransactionEntity>
    
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


    // Get total expense by category for a period
    @Query("SELECT categoryId, SUM(amount) as total FROM transactions WHERE userId = :userId AND type = 'expense' AND date BETWEEN :startDate AND :endDate GROUP BY categoryId")
    suspend fun getExpenseByCategoryForPeriod(userId: String, startDate: Long, endDate: Long): List<CategoryExpenseSum>

    // Get total income by category for a period
    @Query("SELECT categoryId, SUM(amount) as total FROM transactions WHERE userId = :userId AND type = 'income' AND date BETWEEN :startDate AND :endDate GROUP BY categoryId")
    suspend fun getIncomeByCategoryForPeriod(userId: String, startDate: Long, endDate: Long): List<CategoryExpenseSum>

    @Query("SELECT COUNT(*) FROM transactions WHERE userId = :userId")
    suspend fun getCount(userId: String): Int

    // Helper data class for category sum
    data class CategoryExpenseSum(
        val categoryId: String,
        val total: Double
    )

}
