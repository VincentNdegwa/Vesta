package com.example.vesta.data.repository

import com.example.vesta.data.auth.AuthService
import com.example.vesta.data.local.FinvestaDatabase
import com.example.vesta.data.local.entities.UserEntity
import com.example.vesta.data.preferences.PreferencesManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authService: AuthService,
    private val database: FinvestaDatabase,
    private val preferencesManager: PreferencesManager,
    private val firestore: FirebaseFirestore
) {
    
    val authState = authService.getAuthStateFlow()
    val isLoggedIn = preferencesManager.isLoggedIn
    val currentUserId = preferencesManager.userId
    
    suspend fun signUp(email: String, password: String, displayName: String): Result<Unit> {
        return try {
            val result = authService.signUp(email, password, displayName)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                
                // Save user session locally
                preferencesManager.setUserSession(
                    userId = user.uid,
                    email = user.email ?: email,
                    displayName = user.displayName ?: displayName
                )
                
                // Create user entity in Room
                val userEntity = UserEntity(
                    uid = user.uid,
                    email = user.email ?: email,
                    displayName = user.displayName ?: displayName,
                    photoUrl = user.photoUrl?.toString(),
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                    lastSyncedAt = null
                )
                
                database.userDao().insertUser(userEntity)
                
                // Sync user to Firebase (if online)
                syncUserToFirebase(userEntity)
                
                Result.success(Unit)
            } else {
                result.exceptionOrNull()?.let { throw it }
                throw Exception("Sign up failed")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            val result = authService.signIn(email, password)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                
                // Save user session locally
                preferencesManager.setUserSession(
                    userId = user.uid,
                    email = user.email ?: email,
                    displayName = user.displayName
                )
                
                // Check if user exists locally, if not create
                val existingUser = database.userDao().getUser(user.uid)
                if (existingUser == null) {
                    val userEntity = UserEntity(
                        uid = user.uid,
                        email = user.email ?: email,
                        displayName = user.displayName,
                        photoUrl = user.photoUrl?.toString(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                        lastSyncedAt = null
                    )
                    database.userDao().insertUser(userEntity)
                }
                
                // Trigger full data sync from Firebase (if online)
                // This will be handled by SyncRepository
                
                Result.success(Unit)
            } else {
                result.exceptionOrNull()?.let { throw it }
                throw Exception("Sign in failed")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return authService.sendPasswordResetEmail(email)
    }
    
    suspend fun signOut(): Result<Unit> {
        return try {
            authService.signOut()
            preferencesManager.clearUserSession()
            
            // Clear all local data (optional - you might want to keep it for offline access)
            // clearLocalData()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateProfile(displayName: String?, photoUrl: String?): Result<Unit> {
        return try {
            val updateResult = authService.updateProfile(displayName, photoUrl)
            if (updateResult.isSuccess) {
                val currentUser = authService.currentUser
                if (currentUser != null) {
                    // Update local user data
                    val existingUser = database.userDao().getUser(currentUser.uid)
                    existingUser?.let { user ->
                        val updatedUser = user.copy(
                            displayName = displayName ?: user.displayName,
                            photoUrl = photoUrl ?: user.photoUrl,
                            updatedAt = Clock.System.now()
                        )
                        database.userDao().updateUser(updatedUser)
                        
                        // Update preferences
                        preferencesManager.setUserSession(
                            userId = currentUser.uid,
                            email = currentUser.email ?: user.email,
                            displayName = displayName ?: user.displayName
                        )
                        
                        // Sync to Firebase
                        syncUserToFirebase(updatedUser)
                    }
                }
            }
            updateResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun syncUserToFirebase(user: UserEntity) {
        try {
            val userMap = mapOf(
                "uid" to user.uid,
                "email" to user.email,
                "displayName" to user.displayName,
                "photoUrl" to user.photoUrl,
                "createdAt" to user.createdAt.toEpochMilliseconds(),
                "updatedAt" to user.updatedAt.toEpochMilliseconds()
            )
            
            firestore.collection("users")
                .document(user.uid)
                .set(userMap)
                .addOnSuccessListener {
                    // Update sync time in local database
                    // This should be done in a coroutine but Firebase callbacks don't support suspend
                }
                .addOnFailureListener {
                    // Handle sync failure - maybe mark for retry
                }
        } catch (e: Exception) {
            // Handle offline case - data will be synced when online
        }
    }
    
    private suspend fun clearLocalData() {
        // Clear all user-related data from Room database
        val currentUser = authService.currentUser
        currentUser?.let { user ->
            database.userDao().deleteUser(user.uid)
            database.userProfileDao().deleteUserProfile(user.uid)
            database.userSettingsDao().deleteUserSettings(user.uid)
            // Clear other related data...
        }
    }
}
