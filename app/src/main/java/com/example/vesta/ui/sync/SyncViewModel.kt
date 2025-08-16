package com.example.vesta.ui.sync

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.vesta.data.sync.TransactionSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}

@HiltViewModel
class SyncViewModel @Inject constructor(
    @ApplicationContext val context: Context
) : ViewModel() {
    val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    inline fun <reified T : CoroutineWorker> sync(
        process: String,
        userId: String,
        uniqueName: String = "sync_work"
    ) {
        _syncState.value = SyncState.Loading
        viewModelScope.launch {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val inputData = androidx.work.workDataOf(
                    "process" to process,
                    "userId" to userId
                )

                val syncWorkRequest = OneTimeWorkRequestBuilder<T>()
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .build()

                WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        uniqueName,
                        ExistingWorkPolicy.REPLACE,
                        syncWorkRequest
                    )
                _syncState.value = SyncState.Success
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
