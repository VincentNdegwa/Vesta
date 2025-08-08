package com.example.vesta.data.repository

import com.example.vesta.data.local.FinvestaDatabase
import com.example.vesta.data.local.entities.AccountEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val database: FinvestaDatabase
) {
    private val accountDao = database.accountDao()

    fun getAccountsFlow(userId: String): Flow<List<AccountEntity>> =
        accountDao.getAccountsFlow(userId)

    suspend fun getUserAccounts(userId: String): List<AccountEntity?> =
        accountDao.getUserAccounts(userId)

    suspend fun getAccount(id: String): AccountEntity? =
        accountDao.getAccount(id)

    suspend fun insertAccount(account: AccountEntity) =
        accountDao.insertAccount(account)

    suspend fun updateAccount(account: AccountEntity) =
        accountDao.updateAccount(account)

    suspend fun updateBalance(id: String, balance: Double) =
        accountDao.updateBalance(id, balance, System.currentTimeMillis())

    suspend fun getTotalBalance(userId: String): Double =
        accountDao.getTotalBalance(userId) ?: 0.0

    suspend fun getUnsyncedAccounts(): List<AccountEntity> =
        accountDao.getUnsyncedAccounts()

    suspend fun markAsSynced(ids: List<String>) =
        accountDao.markAsSynced(ids, System.currentTimeMillis())
}
