package com.example.vesta.data.repository

import android.content.Context
import android.util.Log
import com.example.vesta.data.auth.AuthService
import com.example.vesta.data.local.FinvestaDatabase
import com.example.vesta.data.local.entities.AccountEntity
import com.example.vesta.data.local.entities.CategoryEntity
import com.example.vesta.data.local.entities.DefaultExpenseCategories
import com.example.vesta.data.local.entities.DefaultIncomeCategories
import com.example.vesta.data.local.entities.UserEntity
import com.example.vesta.data.preferences.PreferencesManager
import com.example.vesta.data.sync.AccountSyncWorker
import com.example.vesta.data.sync.CategorySyncWorker
import com.example.vesta.ui.sync.SyncViewModel
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authService: AuthService,
    private val database: FinvestaDatabase,
    private val preferencesManager: PreferencesManager,
    private val firestore: FirebaseFirestore,
    private val authStateManager: com.example.vesta.utils.AuthStateManager,
    @ApplicationContext private val context: Context
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

                setUserDefaultData(user, database)

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

    private suspend fun setUserDefaultData(user: FirebaseUser, database: FinvestaDatabase) {
        val now = System.currentTimeMillis()
        val defaultAccount = AccountEntity(
            userId = user.uid,
            name = "Main Account",
            type = "CHECKING",
            balance = 0.0
        )
        val defaultExpenseCategories =  DefaultExpenseCategories
        val defaultIncomeCategories = DefaultIncomeCategories

        for (category in defaultIncomeCategories) {
            val categoryEntity = CategoryEntity(
                userId = user.uid,
                name = category,
                type = "INCOME",
                createdAt = now,
                updatedAt = now,
                isSystem = true
            )
            database.categoryDao().insertCategory(categoryEntity)
        }
        for (category in defaultExpenseCategories) {
            val categoryEntity = CategoryEntity(
                userId = user.uid,
                name = category,
                type = "EXPENSE",
                createdAt = now,
                updatedAt = now,
                isSystem = true
            )
            database.categoryDao().insertCategory(categoryEntity)
        }
        database.accountDao().insertAccount(defaultAccount)
        scheduleCategorySync()
        scheduleAccountSync()
    }

    private fun scheduleCategorySync() {
        val syncViewmodel = SyncViewModel(context)
        syncViewmodel.sync<CategorySyncWorker>("UPLOAD", null)
    }
    private fun scheduleAccountSync() {
        val syncViewmodel = SyncViewModel(context)
        syncViewmodel.sync<AccountSyncWorker>("UPLOAD", null)
    }
    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            val result = authService.signIn(email, password)
            Log.d("AuthRepository", "signIn result: $result")
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
            Log.d("AuthRepository", "getCurrentUser called")
            val currentUser = authService.currentUser
            if (currentUser != null) {
                val localUser = database.userDao().getUser(currentUser.uid)
                if (localUser != null) {
                    Log.d("AuthRepository", "Returning local user")
                    Result.success(localUser)
                } else {
                    Log.d("AuthRepository", "Local user not found, creating new one")
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
                    setUserDefaultData(currentUser, database)
                    Result.success(userEntity)
                }
            } else {
                Log.d("AuthRepository", "No current user found")
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Log.d("AuthRepository", "Error getting current user: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            authService.signOut()
            preferencesManager.clearUserSession()
            
            // Make sure to set session inactive in AuthStateManager
            authStateManager.setSessionActive(false)
            Log.d("AuthRepository", "Session marked as inactive in AuthStateManager")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error in signOut: ${e.message}", e)
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
