package com.example.vesta.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.vesta.data.local.FinvestaDatabase
import com.example.vesta.data.local.entities.AccountEntity
import com.example.vesta.data.sync.AccountSyncWorker
import com.example.vesta.data.sync.TransactionSyncWorker
import com.example.vesta.ui.sync.SyncViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val database: FinvestaDatabase,
    @ApplicationContext private val context: Context,
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
    suspend fun getDefaultAccountForUser(userId: String): AccountEntity = accountDao.getDefaultAccountForUser(userId)

    private fun scheduleAccountSync() {
        val syncViewmodel = SyncViewModel(context)
        syncViewmodel.sync<AccountSyncWorker>("UPLOAD", null)
    }
}
