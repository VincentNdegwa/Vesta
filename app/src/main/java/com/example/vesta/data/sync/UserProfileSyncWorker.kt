package com.example.vesta.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.vesta.data.local.FinvestaDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.vesta.data.local.extensions.toMap

class UserProfileSyncWorker(
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
        Log.d("UserProfileSyncWorker", "Received process: $process")
        return try {
            if (process == "DOWNLOAD" && userId != null) {
                syncUserProfileFromFirebaseToRoom(userId)
            } else {
                syncUserProfileToFirebase()
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
    private suspend fun syncUserProfileToFirebase() {
        Log.d("UserProfileSyncWorker", "Syncing user profile to Firestore")
        val userProfileDao = database.userProfileDao()
        val userProfile = userProfileDao.getUnsyncedUserProfile()
        userProfile.forEach { profile ->
            try {
                val updatedProfile = profile.copy(
                    isSynced = true,
                    updatedAt = System.currentTimeMillis()
                )
                val synProfile = updatedProfile.toMap()
                firestore.collection("users")
                    .document(profile.userId)
                    .collection("profile")
                    .document(profile.userId)
                    .set(synProfile)
                    .await()
                userProfileDao.updateUserProfile(updatedProfile)
                Log.d("UserProfileSyncWorker", "Synced user profile ${profile.userId} to Firebase")
            } catch (e: Exception) {
                Log.d("UserProfileSyncWorker", "Failed to sync user profile ${profile.userId} to Firebase")
            }
        }
    }
    suspend fun syncUserProfileFromFirebaseToRoom(userId: String) {
        Log.d("UserProfileSyncWorker", "Syncing user profile from Firestore to Room for user $userId")
        val userProfileDao = database.userProfileDao()
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("profile")
                .document(userId)
                .get()
                .await()
            val data = snapshot.data
            if (data != null) {
                try {
                    val entityClass = com.example.vesta.data.local.entities.UserProfileEntity::class
                    val constructor = entityClass.constructors.first()
                    val args = constructor.parameters.associateWith { param ->
                        data[param.name]
                    }
                    val profile = constructor.callBy(args)
                    userProfileDao.insertUserProfile(profile)
                    Log.d("UserProfileSyncWorker", "Synced user profile for user $userId from Firestore to Room")
                } catch (e: Exception) {
                    Log.d("UserProfileSyncWorker", "Error mapping Firestore user profile: $userId")
                }
            }
        } catch (e: Exception) {
            Log.d("UserProfileSyncWorker", "Failed to sync user profile from Firestore: ${e.message}")
        }
    }
}
