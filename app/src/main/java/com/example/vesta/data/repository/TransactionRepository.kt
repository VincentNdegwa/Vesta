package com.example.vesta.data.repository

import com.example.vesta.data.local.FinvestaDatabase
import com.example.vesta.data.local.entities.TransactionEntity
import com.example.vesta.data.preferences.PreferencesManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val database: FinvestaDatabase,
    private val firestore: FirebaseFirestore,
    private val preferencesManager: PreferencesManager,
    private val networkManager: NetworkManager
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
        startDate: Instant,
        endDate: Instant
    ): List<TransactionEntity> {
        return transactionDao.getTransactionsByDateRange(userId, startDate, endDate)
    }
    
    suspend fun getTransactionsByCategory(userId: String, category: String): List<TransactionEntity> {
        return transactionDao.getTransactionsByCategory(userId, category)
    }
    
    suspend fun getTotalIncomeForPeriod(userId: String, startDate: Instant, endDate: Instant): Double {
        return transactionDao.getTotalIncomeForPeriod(userId, startDate, endDate) ?: 0.0
    }
    
    suspend fun getTotalExpenseForPeriod(userId: String, startDate: Instant, endDate: Instant): Double {
        return transactionDao.getTotalExpenseForPeriod(userId, startDate, endDate) ?: 0.0
    }
    
    suspend fun addTransaction(
        userId: String,
        amount: Double,
        type: String,
        category: String,
        subcategory: String? = null,
        description: String? = null,
        notes: String? = null,
        date: Instant = Clock.System.now(),
        accountId: String? = null,
        paymentMethod: String? = null,
        location: String? = null,
        tags: List<String> = emptyList()
    ): Result<TransactionEntity> {
        return try {
            val now = Clock.System.now()
            val transactionId = UUID.randomUUID().toString()
            
            val transaction = TransactionEntity(
                id = transactionId,
                userId = userId,
                amount = amount,
                type = type,
                category = category,
                subcategory = subcategory,
                description = description,
                notes = notes,
                date = date,
                accountId = accountId,
                paymentMethod = paymentMethod,
                location = location,
                tags = tags,
                recurringId = null,
                createdAt = now,
                updatedAt = now,
                lastSyncedAt = null,
                isDeleted = false,
                needsSync = true
            )
            
            // Always save to local database first
            transactionDao.insertTransaction(transaction)
            
            // Try to sync to Firebase if online
            if (networkManager.isOnline()) {
                syncTransactionToFirebase(transaction)
            }
            
            // Update account balance if account is specified
            accountId?.let { 
                updateAccountBalance(it, amount, type == "EXPENSE")
            }
            
            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateTransaction(
        transactionId: String,
        amount: Double? = null,
        type: String? = null,
        category: String? = null,
        subcategory: String? = null,
        description: String? = null,
        notes: String? = null,
        date: Instant? = null,
        accountId: String? = null,
        paymentMethod: String? = null,
        location: String? = null,
        tags: List<String>? = null
    ): Result<TransactionEntity> {
        return try {
            val existingTransaction = transactionDao.getTransaction(transactionId)
                ?: throw Exception("Transaction not found")
            
            val updatedTransaction = existingTransaction.copy(
                amount = amount ?: existingTransaction.amount,
                type = type ?: existingTransaction.type,
                category = category ?: existingTransaction.category,
                subcategory = subcategory ?: existingTransaction.subcategory,
                description = description ?: existingTransaction.description,
                notes = notes ?: existingTransaction.notes,
                date = date ?: existingTransaction.date,
                accountId = accountId ?: existingTransaction.accountId,
                paymentMethod = paymentMethod ?: existingTransaction.paymentMethod,
                location = location ?: existingTransaction.location,
                tags = tags ?: existingTransaction.tags,
                updatedAt = Clock.System.now(),
                needsSync = true
            )
            
            transactionDao.updateTransaction(updatedTransaction)
            
            // Try to sync to Firebase if online
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
            val now = Clock.System.now()
            transactionDao.softDeleteTransaction(transactionId, now)
            
            // Try to sync deletion to Firebase if online
            if (networkManager.isOnline()) {
                firestore.collection("transactions")
                    .document(transactionId)
                    .update(
                        mapOf(
                            "isDeleted" to true,
                            "updatedAt" to now.toEpochMilliseconds()
                        )
                    )
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
                "subcategory" to transaction.subcategory,
                "description" to transaction.description,
                "notes" to transaction.notes,
                "date" to transaction.date.toEpochMilliseconds(),
                "accountId" to transaction.accountId,
                "paymentMethod" to transaction.paymentMethod,
                "location" to transaction.location,
                "receiptUrl" to transaction.receiptUrl,
                "tags" to transaction.tags,
                "recurringId" to transaction.recurringId,
                "createdAt" to transaction.createdAt.toEpochMilliseconds(),
                "updatedAt" to transaction.updatedAt.toEpochMilliseconds(),
                "isDeleted" to transaction.isDeleted
            )
            
            firestore.collection("transactions")
                .document(transaction.id)
                .set(transactionMap)
                .addOnSuccessListener {
                    // Mark as synced in local database
                    // This should be done in a coroutine
                }
                .addOnFailureListener {
                    // Handle sync failure
                }
        } catch (e: Exception) {
            // Handle offline case
        }
    }
    
    private suspend fun updateAccountBalance(accountId: String, amount: Double, isExpense: Boolean) {
        try {
            val account = database.accountDao().getAccount(accountId)
            account?.let {
                val newBalance = if (isExpense) {
                    it.balance - amount
                } else {
                    it.balance + amount
                }
                database.accountDao().updateBalance(accountId, newBalance, Clock.System.now())
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    // Sync functions for when app comes back online
    suspend fun syncPendingTransactions(): Result<Unit> {
        return try {
            if (!networkManager.isOnline()) {
                return Result.failure(Exception("Device is offline"))
            }
            
            val unsyncedTransactions = transactionDao.getUnsyncedTransactions()
            val now = Clock.System.now()
            
            unsyncedTransactions.forEach { transaction ->
                syncTransactionToFirebase(transaction)
            }
            
            // Mark all as synced
            val transactionIds = unsyncedTransactions.map { it.id }
            transactionDao.markAsSynced(transactionIds, now)
            
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
