package com.example.vesta.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.vesta.data.local.FinvestaDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.vesta.data.local.extensions.toMap

class CategorySyncWorker(
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
        Log.d("CategorySyncWorker", "Received process: $process")
        return try {
            if (process == "DOWNLOAD" && userId != null) {
                syncCategoriesFromFirebaseToRoom(userId)
            } else {
                syncCategoriesToFirebase()
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
    private suspend fun syncCategoriesToFirebase() {
        Log.d("CategorySyncWorker", "Syncing categories to Firestore")
        val categoryDao = database.categoryDao()
        val unsyncedCategories = categoryDao.getUnsyncedCategories()
        unsyncedCategories.forEach { category ->
            try {
                val updatedCategory = category.copy(
                    isSynced = true,
                    updatedAt = System.currentTimeMillis()
                )
                val synCategory = updatedCategory.toMap()
                firestore.collection("users")
                    .document(category.userId)
                    .collection("categories")
                    .document(category.id)
                    .set(synCategory)
                    .await()
                categoryDao.updateCategory(updatedCategory)
                Log.d("CategorySyncWorker", "Synced category ${category.id} to Firebase")
            } catch (e: Exception) {
                Log.d("CategorySyncWorker", "Failed to sync category ${category.id} to Firebase")
            }
        }
    }
    suspend fun syncCategoriesFromFirebaseToRoom(userId: String) {
        Log.d("CategorySyncWorker", "Syncing categories from Firestore to Room for user $userId")
        val categoryDao = database.categoryDao()
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("categories")
                .get()
                .await()
            val categories = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                try {
                    val entityClass = com.example.vesta.data.local.entities.CategoryEntity::class
                    val constructor = entityClass.constructors.first()
                    val args = constructor.parameters.associateWith { param ->
                        data[param.name]
                    }
                    constructor.callBy(args)
                } catch (e: Exception) {
                    Log.d("CategorySyncWorker", "Error mapping Firestore category: ${doc.id}")
                    null
                }
            }
            if (categories.isNotEmpty()) {
                categoryDao.insertCategories(categories)
                Log.d("CategorySyncWorker", "Synced ${categories.size} categories from Firestore to Room")
            }
        } catch (e: Exception) {
            Log.d("CategorySyncWorker", "Failed to sync categories from Firestore: ${e.message}")
        }
    }
}
