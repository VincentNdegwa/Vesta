package com.example.vesta.data.local.dao

import androidx.room.*
import com.example.vesta.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    
    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getUser(uid: String): UserEntity?
    
    @Query("SELECT * FROM users WHERE uid = :uid")
    fun getUserFlow(uid: String): Flow<UserEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
    
    @Update
    suspend fun updateUser(user: UserEntity)
    
    @Query("DELETE FROM users WHERE uid = :uid")
    suspend fun deleteUser(uid: String)
    
    @Query("SELECT * FROM users WHERE lastSyncedAt IS NULL OR lastSyncedAt < updatedAt")
    suspend fun getUnsyncedUsers(): List<UserEntity>
}
