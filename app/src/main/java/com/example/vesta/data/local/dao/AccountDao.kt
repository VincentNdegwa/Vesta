package com.example.vesta.data.local.dao

import androidx.room.*
import com.example.vesta.data.local.entities.AccountEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface AccountDao {
    
    @Query("SELECT * FROM accounts WHERE userId = :userId AND isDeleted = 0 ORDER BY name ASC")
    fun getAccountsFlow(userId: String): Flow<List<AccountEntity>>
    
    @Query("SELECT * FROM accounts WHERE userId = :userId AND isDeleted = 0 ORDER BY name ASC")
    suspend fun getAccounts(userId: String): List<AccountEntity>
    
    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccount(id: String): AccountEntity?
    
    @Query("SELECT * FROM accounts WHERE userId = :userId AND isActive = 1 AND isDeleted = 0")
    suspend fun getActiveAccounts(userId: String): List<AccountEntity>
    
    @Query("SELECT SUM(balance) FROM accounts WHERE userId = :userId AND isIncludeInTotal = 1 AND isDeleted = 0")
    suspend fun getTotalBalance(userId: String): Double?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)
    
    @Update
    suspend fun updateAccount(account: AccountEntity)
    
    @Query("UPDATE accounts SET balance = :balance, updatedAt = :updatedAt, needsSync = 1 WHERE id = :id")
    suspend fun updateBalance(id: String, balance: Double, updatedAt: Instant)
    
    @Query("UPDATE accounts SET isDeleted = 1, updatedAt = :deletedAt, needsSync = 1 WHERE id = :id")
    suspend fun softDeleteAccount(id: String, deletedAt: Instant)
    
    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun hardDeleteAccount(id: String)
    
    @Query("SELECT * FROM accounts WHERE needsSync = 1")
    suspend fun getUnsyncedAccounts(): List<AccountEntity>
    
    @Query("UPDATE accounts SET needsSync = 0, lastSyncedAt = :syncTime WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>, syncTime: Instant)
}
