package com.example.vesta.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.vesta.data.local.FinvestaDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.vesta.data.local.extensions.toMap

class AccountSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    private val database: FinvestaDatabase by lazy {
        FinvestaDatabase.getInstance(appContext)
    }
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    override suspend fun doWork(): Result {
        val process = inputData.getString("process") ?: "UPLOAD"
        val userId = inputData.getString("userId")
        Log.d("AccountSyncWorker", "Received process: $process")
        return try {
            if (process == "DOWNLOAD" && userId != null && userId != "null") {
                syncAccountsFromFirebaseToRoom(userId)
            } else {
                syncAccountsToFirebase()
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    private suspend fun syncAccountsToFirebase() {
        Log.d("AccountSyncWorker", "Syncing accounts to Firestore")
        val accountDao = database.accountDao()
        val unsyncedAccounts = accountDao.getUnsyncedAccounts()
        unsyncedAccounts.forEach { account ->
            try {
                val updatedAccount = account.copy(
                    isSynced = true,
                    updatedAt = System.currentTimeMillis()
                )
                val synAccount = updatedAccount.toMap()
                firestore.collection("users")
                    .document(account.userId)
                    .collection("accounts")
                    .document(account.id)
                    .set(synAccount)
                    .await()
                accountDao.updateAccount(updatedAccount)
                Log.d("AccountSyncWorker", "Synced account ${account.id} to Firebase")
            } catch (e: Exception) {
                Log.d("AccountSyncWorker", "Failed to sync account ${account.id} to Firebase")
            }
        }
    }
    suspend fun syncAccountsFromFirebaseToRoom(userId: String) {
        val accountDao = database.accountDao()
        try {
            val count = accountDao.getCount(userId)
            if (count > 0){
                return
            }
            Log.d("AccountSyncWorker", "Syncing accounts from Firestore to Room for user $userId")

            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("accounts")
                .get()
                .await()
            val accounts = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    try {
                        val entityClass = com.example.vesta.data.local.entities.AccountEntity::class
                        val constructor = entityClass.constructors.first()
                        val args = constructor.parameters.associateWith { param ->
                            data[param.name]
                        }
                        constructor.callBy(args)
                    } catch (e: Exception) {
                        Log.d("AccountSyncWorker", "Error mapping Firestore account: ${doc.id}")
                        null
                    }
                }
            if (accounts.isNotEmpty()) {
                accountDao.insertAccounts(accounts)
                Log.d("AccountSyncWorker", "Synced ${accounts.size} accounts from Firestore to Room")
            }

        } catch (e: Exception) {
            Log.d("AccountSyncWorker", "Failed to sync accounts from Firestore: ${e.message}")
        }
    }
}
