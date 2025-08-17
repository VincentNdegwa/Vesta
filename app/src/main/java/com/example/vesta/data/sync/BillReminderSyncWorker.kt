package com.example.vesta.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.vesta.data.local.FinvestaDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.vesta.data.local.extensions.toMap

class BillReminderSyncWorker(
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
        Log.d("BillReminderSyncWorker", "Received process: $process")
        return try {
            if (process == "DOWNLOAD" && userId != null) {
                syncBillRemindersFromFirebaseToRoom(userId)
            } else {
                syncBillRemindersToFirebase()
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
    private suspend fun syncBillRemindersToFirebase() {
        Log.d("BillReminderSyncWorker", "Syncing bill reminders to Firestore")
        val billReminderDao = database.billReminderDao()
        val unsyncedReminders = billReminderDao.getUnsyncedBillReminders()
        unsyncedReminders.forEach { reminder ->
            try {
                val updatedReminder = reminder.copy(
                    isSynced = true,
                    updatedAt = System.currentTimeMillis()
                )
                val synReminder = updatedReminder.toMap()
                firestore.collection("users")
                    .document(reminder.userId)
                    .collection("bill_reminders")
                    .document(reminder.id)
                    .set(synReminder)
                    .await()
                billReminderDao.updateBillReminder(updatedReminder)
                Log.d("BillReminderSyncWorker", "Synced bill reminder ${reminder.id} to Firebase")
            } catch (e: Exception) {
                Log.d("BillReminderSyncWorker", "Failed to sync bill reminder ${reminder.id} to Firebase")
            }
        }
    }
    suspend fun syncBillRemindersFromFirebaseToRoom(userId: String) {
        Log.d("BillReminderSyncWorker", "Syncing bill reminders from Firestore to Room for user $userId")
        val billReminderDao = database.billReminderDao()
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("bill_reminders")
                .get()
                .await()
            val reminders = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                try {
                    val entityClass = com.example.vesta.data.local.entities.BillReminderEntity::class
                    val constructor = entityClass.constructors.first()
                    val args = constructor.parameters.associateWith { param ->
                        data[param.name]
                    }
                    constructor.callBy(args)
                } catch (e: Exception) {
                    Log.d("BillReminderSyncWorker", "Error mapping Firestore bill reminder: ${doc.id}")
                    null
                }
            }
            if (reminders.isNotEmpty()) {
                billReminderDao.insertBillReminders(reminders)
                Log.d("BillReminderSyncWorker", "Synced ${reminders.size} bill reminders from Firestore to Room")
            }
        } catch (e: Exception) {
            Log.d("BillReminderSyncWorker", "Failed to sync bill reminders from Firestore: ${e.message}")
        }
    }
}
