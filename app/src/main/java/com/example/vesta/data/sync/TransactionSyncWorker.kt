package com.example.vesta.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.vesta.data.local.FinvestaDatabase
import com.google.firebase.firestore.FirebaseFirestore
// No Hilt imports
import kotlinx.coroutines.tasks.await

class TransactionSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    private val database: FinvestaDatabase by lazy {
        FinvestaDatabase.getInstance(appContext)
    }
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    override suspend fun doWork(): Result {
        return try {
            syncTransactionsToFirebase()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun syncTransactionsToFirebase() {
        val transactionDao = database.transactionDao()
        val unsyncedTransactions = transactionDao.getUnsyncedTransactions()

        unsyncedTransactions.forEach { transaction ->
            try {
                val transactionData = mapOf(
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

                // Upload to Firestore
                firestore.collection("users")
                    .document(transaction.userId)
                    .collection("transactions")
                    .document(transaction.id)
                    .set(transactionData)
                    .await()

                // Mark as synced in local database
                val syncedTransaction = transaction.copy(
                    isSynced = true,
                    updatedAt = System.currentTimeMillis()
                )
                transactionDao.updateTransaction(syncedTransaction)
                Log.d("TransactionSyncWorker", "Synced transaction ${transaction.id} to Firebase")
            } catch (e: Exception) {
                Log.d("TransactionSyncWorker", "Failed to sync transaction ${transaction.id} to Firebase")
            }
        }
    }
}
