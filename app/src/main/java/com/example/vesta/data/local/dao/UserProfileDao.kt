package com.example.vesta.data.local.dao

import androidx.room.*
import com.example.vesta.data.local.entities.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    
    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    suspend fun getUserProfile(userId: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    fun getUserProfileFlow(userId: String): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)

    @Update
    suspend fun updateUserProfile(profile: UserProfileEntity)

    @Query("DELETE FROM user_profiles WHERE userId = :userId")
    suspend fun deleteUserProfile(userId: String)
    @Query("SELECT * FROM user_profiles WHERE isSynced = 0")
    suspend fun getUnsyncedProfiles(): List<UserProfileEntity>

    @Query("UPDATE user_profiles SET isSynced = 1 WHERE userId IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}
