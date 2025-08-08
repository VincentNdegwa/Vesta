package com.example.vesta.data.local.dao

import androidx.room.*
import com.example.vesta.data.local.entities.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {
    
    @Query("SELECT * FROM user_settings WHERE userId = :userId")
    suspend fun getUserSettings(userId: String): UserSettingsEntity?
    
    @Query("SELECT * FROM user_settings WHERE userId = :userId")
    fun getUserSettingsFlow(userId: String): Flow<UserSettingsEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSettings(settings: UserSettingsEntity)
    
    @Update
    suspend fun updateUserSettings(settings: UserSettingsEntity)
    
    @Query("DELETE FROM user_settings WHERE userId = :userId")
    suspend fun deleteUserSettings(userId: String)

    @Query("SELECT * FROM user_settings WHERE isSynced = 0")
    suspend fun getUnsyncedSettings(): List<UserSettingsEntity>
}
