package com.example.vesta.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.vesta.data.local.FinvestaDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.vesta.data.local.extensions.toMap

class BudgetSyncWorker(
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
        Log.d("BudgetSyncWorker", "Received process: $process")
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
        Log.d("BudgetSyncWorker", "Syncing budgets to Firestore")
        val budgetDao = database.budgetDao()
        val unsyncedBudgets = budgetDao.getUnsyncedBudgets()
        unsyncedBudgets.forEach { budget ->
            try {
                val updatedBudget = budget.copy(
                    isSynced = true,
                    updatedAt = System.currentTimeMillis()
                )
                val synAccount = updatedBudget.toMap()
                firestore.collection("users")
                    .document(budget.userId)
                    .collection("budgets")
                    .document(budget.id)
                    .set(synAccount)
                    .await()
                budgetDao.updateBudget(updatedBudget)
                Log.d("BudgetSyncWorker", "Synced budget ${budget.id} to Firebase")
            } catch (e: Exception) {
                Log.d("BudgetSyncWorker", "Failed to sync budget ${budget.id} to Firebase")
            }
        }
    }
    suspend fun syncAccountsFromFirebaseToRoom(userId: String) {
        val budgetDao = database.budgetDao()
        try {
            val count = budgetDao.getCount(userId)
            if (count > 0){
                return
            }
            Log.d("BudgetSyncWorker", "Syncing budgets from Firestore to Room for user $userId")

            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("budgets")
                .get()
                .await()
            val budgets = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                try {
                    com.example.vesta.data.local.entities.BudgetEntity(
                        id = data["id"] as? String ?: doc.id,
                        userId = data["userId"] as? String ?: "",
                        name = data["name"] as? String ?: "",
                        categoryId = data["categoryId"] as? String ?: "",
                        targetAmount = (data["targetAmount"] as? Number)?.toDouble() ?: 0.0,
                        spentAmount = (data["spentAmount"] as? Number)?.toDouble() ?: 0.0,
                        period = try {
                            val periodStr = data["period"] as? String ?: "MONTHLY"
                            com.example.vesta.data.local.entities.BudgetPeriod.valueOf(periodStr)
                        } catch (e: Exception) {
                            com.example.vesta.data.local.entities.BudgetPeriod.MONTHLY
                        },
                        startDate = (data["startDate"] as? Number)?.toLong() ?: 0L,
                        endDate = (data["endDate"] as? Number)?.toLong() ?: 0L,
                        resetOn = if (data["resetOn"] == null) null else (data["resetOn"] as? Number)?.toLong(),
                        lastCalculated = (data["lastCalculated"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        isActive = data["isActive"] as? Boolean ?: true,
                        isSynced = data["isSynced"] as? Boolean ?: false
                    )
                } catch (e: Exception) {
                    Log.d("BudgetSyncWorker", "Error mapping Firestore budget: ${doc.id}")
                    null
                }
            }
            if (budgets.isNotEmpty()) {
                budgetDao.insertBudgets(budgets)
                Log.d("BudgetSyncWorker", "Synced ${budgets.size} budgets from Firestore to Room")
            }

        } catch (e: Exception) {
            Log.d("BudgetSyncWorker", "Failed to sync budgets from Firestore: ${e.message}")
        }
    }
}
