package com.example.vesta.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.vesta.data.local.FinvestaDatabase
import com.google.firebase.firestore.FirebaseFirestore
// No Hilt imports
import kotlinx.coroutines.tasks.await
import com.example.vesta.data.local.extensions.toMap

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
                val transactionData = transaction.toMap()

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

    suspend fun syncTransactionsFromFirebaseToRoom(userId: String) {
        val transactionDao = database.transactionDao()
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("transactions")
                .get()
                .await()
            val transactions = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                try {
                    val entityClass = com.example.vesta.data.local.entities.TransactionEntity::class
                    val constructor = entityClass.constructors.first()
                    val args = constructor.parameters.associateWith { param ->
                        data[param.name]
                    }
                    constructor.callBy(args)
                } catch (e: Exception) {
                    Log.d("TransactionSyncWorker", "Error mapping Firestore transaction: ${doc.id}")
                    null
                }
            }
            if (transactions.isNotEmpty()) {
                transactionDao.insertTransactions(transactions)
                Log.d("TransactionSyncWorker", "Synced ${transactions.size} transactions from Firestore to Room")
            }
        } catch (e: Exception) {
            Log.d("TransactionSyncWorker", "Failed to sync transactions from Firestore: ${e.message}")
        }
    }
}
