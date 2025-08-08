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

                preferencesManager.setUserSession(
                    userId = user.uid,
                    email = user.email ?: email,
                    displayName = user.displayName ?: displayName
                )

                val now = System.currentTimeMillis()
                val userEntity = UserEntity(
                    id = user.uid,
                    email = user.email ?: email,
                    displayName = user.displayName ?: displayName,
                    photoUrl = user.photoUrl?.toString(),
                    createdAt = now,
                    updatedAt = now,
                    isSynced = false
                )

                database.userDao().insertUser(userEntity)
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

                preferencesManager.setUserSession(
                    userId = user.uid,
                    email = user.email ?: email,
                    displayName = user.displayName
                )

                val existingUser = database.userDao().getUser(user.uid)
                if (existingUser == null) {
                    val now = System.currentTimeMillis()
                    val userEntity = UserEntity(
                        id = user.uid,
                        email = user.email ?: email,
                        displayName = user.displayName,
                        photoUrl = user.photoUrl?.toString(),
                        createdAt = now,
                        updatedAt = now,
                        isSynced = false
                    )
                    database.userDao().insertUser(userEntity)
                }

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
    
    suspend fun getCurrentUser(): Result<UserEntity?> {
        return try {
            val currentUser = authService.currentUser
            if (currentUser != null) {
                val localUser = database.userDao().getUser(currentUser.uid)
                if (localUser != null) {
                    Result.success(localUser)
                } else {
                    val now = System.currentTimeMillis()
                    val userEntity = UserEntity(
                        id = currentUser.uid,
                        email = currentUser.email ?: "",
                        displayName = currentUser.displayName,
                        photoUrl = currentUser.photoUrl?.toString(),
                        createdAt = now,
                        updatedAt = now,
                        isSynced = false
                    )
                    database.userDao().insertUser(userEntity)
                    Result.success(userEntity)
                }
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            authService.signOut()
            preferencesManager.clearUserSession()

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
                    val existingUser = database.userDao().getUser(currentUser.uid)
                    existingUser?.let { user ->
                        val updatedUser = user.copy(
                            displayName = displayName ?: user.displayName,
                            photoUrl = photoUrl ?: user.photoUrl,
                            updatedAt = System.currentTimeMillis(),
                            isSynced = false
                        )
                        database.userDao().updateUser(updatedUser)
                        preferencesManager.setUserSession(
                            userId = currentUser.uid,
                            email = currentUser.email ?: user.email,
                            displayName = displayName ?: user.displayName
                        )
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
                "id" to user.id,
                "email" to user.email,
                "displayName" to user.displayName,
                "photoUrl" to user.photoUrl,
                "createdAt" to user.createdAt,
                "updatedAt" to user.updatedAt
            )

            firestore.collection("users")
                .document(user.id)
                .set(userMap)
                .addOnSuccessListener {
                    // Optionally mark as synced
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
