package com.example.vesta.ui.bills.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vesta.data.local.entities.AccountEntity
import com.example.vesta.data.local.entities.BillReminderEntity
import com.example.vesta.data.local.entities.RecurrenceType
import com.example.vesta.data.repository.BillReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class BillReminderUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val billReminders: List<BillReminderEntity> = emptyList(),
    val activeBillReminders: List<BillReminderEntity> = emptyList(),
    val recurrenceTypes: List<RecurrenceType> = RecurrenceType.values().toList(),
    val selectedRecurrenceType: RecurrenceType = RecurrenceType.NONE,
    val intervalCount: Int = 1,
    val timesPerPeriod: Int? = null
)

@HiltViewModel
class BillViewModel @Inject constructor(
    private val billReminderRepository: BillReminderRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BillReminderUiState())
    val uiState: StateFlow<BillReminderUiState> = _uiState.asStateFlow()


    fun loadBillReminders(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {

                billReminderRepository.getBillRemindersFlow(userId).collect { reminders ->
                    _uiState.update { 
                        it.copy(
                            billReminders = reminders,
                            isLoading = false
                        )
                    }
                }
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to load bill reminders: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    fun loadActiveBillReminders(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val reminders = billReminderRepository.getActiveBillRemindersFlow(userId)
                _uiState.update {
                    it.copy(
                        activeBillReminders = reminders,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to load active bill reminders: ${e.message}"
                    ) 
                }
            }
        }
    }

    fun addBillReminder(
        userId: String,
        title: String,
        amount: Double,
        category: String,
        dueDate: Long,
        recurrenceType: RecurrenceType,
        intervalCount: Int = 1,
        timesPerPeriod: Int? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isSuccess = false) }
            
            try {
                val dueDateTimestamp = dueDate
                
                val billReminder = BillReminderEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    title = title,
                    amount = amount,
                    category = category,
                    dueDate = dueDateTimestamp,
                    recurrenceType = recurrenceType,
                    intervalCount = intervalCount,
                    timesPerPeriod = timesPerPeriod
                )
                
                val result = billReminderRepository.addBillReminder(billReminder)
                
                if (result.isSuccess) {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to add bill reminder"
                    _uiState.update { it.copy(isLoading = false, error = errorMsg) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to add bill reminder: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun markAsPaid(billId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val result = billReminderRepository.markAsPaid(billId)
                
                if (result.isSuccess) {
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to mark bill as paid"
                    _uiState.update { it.copy(isLoading = false, error = errorMsg) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to mark bill as paid: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun markBillAsPaid(billId: String, userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val result = billReminderRepository.markAsPaid(billId)
                
                if (result.isSuccess) {
                    // Refresh the bill reminders list after marking as paid
                    loadBillReminders(userId)
                    loadActiveBillReminders(userId)
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to mark bill as paid"
                    _uiState.update { it.copy(isLoading = false, error = errorMsg) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to mark bill as paid: ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteBillReminder(billId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val result = billReminderRepository.deleteBillReminder(billId)
                
                if (result.isSuccess) {
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to delete bill reminder"
                    _uiState.update { it.copy(isLoading = false, error = errorMsg) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to delete bill reminder: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun setRecurrenceType(type: RecurrenceType) {
        _uiState.update { it.copy(selectedRecurrenceType = type) }
    }
    
    fun setIntervalCount(count: Int) {
        _uiState.update { it.copy(intervalCount = count) }
    }
    
    fun setTimesPerPeriod(times: Int?) {
        _uiState.update { it.copy(timesPerPeriod = times) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun resetSuccess() {
        _uiState.update { it.copy(isSuccess = false) }
    }
    
    // Keeping this method for backward compatibility if needed
    private fun parseDueDate(dateString: String): Long {
        return try {
            val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            formatter.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    
    fun formatDueDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    fun getBillStatus(bill: BillReminderEntity): BillStatus {
        return when {
            bill.isPaid -> BillStatus.PAID
            bill.dueDate < System.currentTimeMillis() -> BillStatus.OVERDUE
            else -> BillStatus.UPCOMING
        }
    }
}

enum class BillStatus(val displayName: String, val color: androidx.compose.ui.graphics.Color) {
    UPCOMING("Upcoming", androidx.compose.ui.graphics.Color(0xFFFFA726)),
    OVERDUE("Overdue", androidx.compose.ui.graphics.Color(0xFFE57373)),
    PAID("Paid", androidx.compose.ui.graphics.Color(0xFF66BB6A))
}
