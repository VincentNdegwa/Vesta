package com.example.vesta.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.vesta.data.local.FinvestaDatabase
import com.example.vesta.data.local.entities.TransactionEntity
import com.example.vesta.data.preferences.PreferencesManager
import com.example.vesta.data.sync.TransactionSyncWorker
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val database: FinvestaDatabase,
    private val firestore: FirebaseFirestore,
    private val preferencesManager: PreferencesManager,
    private val networkManager: NetworkManager,
    @ApplicationContext private val context: Context
) {
    
    private val transactionDao = database.transactionDao()
    
    fun getTransactions(userId: String): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsFlow(userId)
    }
    
    suspend fun getTransaction(id: String): TransactionEntity? {
        return transactionDao.getTransaction(id)
    }
    
    suspend fun getTransactionsByDateRange(
        userId: String,
        startDate: Long,
        endDate: Long
    ): List<TransactionEntity> {
        return transactionDao.getTransactionsByDateRange(userId, startDate, endDate)
    }
    
    suspend fun getTransactionsByCategory(userId: String, category: String): List<TransactionEntity> {
        return transactionDao.getTransactionsByCategory(userId, category)
    }
    
    suspend fun getTotalIncomeForPeriod(userId: String, startDate: Long, endDate: Long): Double {
        return transactionDao.getTotalIncomeForPeriod(userId, startDate, endDate) ?: 0.0
    }
    
    suspend fun getTotalExpenseForPeriod(userId: String, startDate: Long, endDate: Long): Double {
        return transactionDao.getTotalExpenseForPeriod(userId, startDate, endDate) ?: 0.0
    }
    
    suspend fun addTransaction(transaction: TransactionEntity): Result<Unit> {
        return try {
            // Always save to local database first
            transactionDao.insertTransaction(transaction)
            
            // Schedule background sync to Firebase
            scheduleTransactionSync()
            
            if (networkManager.isOnline()) {
                syncTransactionToFirebase(transaction)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun scheduleTransactionSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = OneTimeWorkRequestBuilder<TransactionSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "transaction_sync",
                ExistingWorkPolicy.REPLACE,
                syncWorkRequest
            )
    }

    suspend fun updateTransaction(
        transactionId: String,
        amount: Double? = null,
        type: String? = null,
        category: String? = null,
        description: String? = null,
        date: Long? = null
    ): Result<TransactionEntity> {
        return try {
            val existingTransaction = transactionDao.getTransaction(transactionId)
                ?: throw Exception("Transaction not found")
            
            val updatedTransaction = existingTransaction.copy(
                amount = amount ?: existingTransaction.amount,
                type = type ?: existingTransaction.type,
                category = category ?: existingTransaction.category,
                description = description ?: existingTransaction.description,
                date = date ?: existingTransaction.date,
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
            
            transactionDao.updateTransaction(updatedTransaction)
            
            // Schedule background sync to Firebase
            scheduleTransactionSync()
            
            // Try immediate sync if online
            if (networkManager.isOnline()) {
                syncTransactionToFirebase(updatedTransaction)
            }
            
            Result.success(updatedTransaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        return try {
            val transaction = transactionDao.getTransaction(transactionId)
                ?: return Result.failure(Exception("Transaction not found"))
            
            // Delete from local database
            transactionDao.deleteTransaction(transactionId)
            
            // Schedule background sync to Firebase for deletion
            scheduleTransactionSync()
            
            // Try to sync deletion to Firebase if online
            if (networkManager.isOnline()) {
                firestore.collection("users")
                    .document(transaction.userId)
                    .collection("transactions")
                    .document(transactionId)
                    .delete()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun syncTransactionToFirebase(transaction: TransactionEntity) {
        try {
            val transactionMap = mapOf(
                "id" to transaction.id,
                "userId" to transaction.userId,
                "amount" to transaction.amount,
                "type" to transaction.type,
                "category" to transaction.category,
                "description" to transaction.description,
                "date" to transaction.date,
                "createdAt" to transaction.createdAt,
                "updatedAt" to transaction.updatedAt
            )
            
            firestore.collection("users")
                .document(transaction.userId)
                .collection("transactions")
                .document(transaction.id)
                .set(transactionMap)
                .await()
            
             val syncedTransaction = transaction.copy(
                 isSynced = true,
                 updatedAt = System.currentTimeMillis()
             )
             transactionDao.updateTransaction(syncedTransaction)
            
        } catch (e: Exception) {
            // Handle sync failure - transaction remains unsynced
            println("Failed to sync transaction ${transaction.id}: ${e.message}")
        }
    }
    
    // Sync functions for when app comes back online
    suspend fun syncPendingTransactions(): Result<Unit> {
        return try {
            if (!networkManager.isOnline()) {
                return Result.failure(Exception("Device is offline"))
            }
            
            val unsyncedTransactions = transactionDao.getUnsyncedTransactions()
            
            unsyncedTransactions.forEach { transaction ->
                syncTransactionToFirebase(transaction)
            }
            
            // Mark all as synced
            val transactionIds = unsyncedTransactions.map { it.id }
            transactionDao.markAsSynced(transactionIds, System.currentTimeMillis())
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun syncFromFirebase(userId: String): Result<Unit> {
        return try {
            if (!networkManager.isOnline()) {
                return Result.failure(Exception("Device is offline"))
            }
            
            // This would fetch transactions from Firebase and update local database
            // Implementation would involve Firebase query and local database updates
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
