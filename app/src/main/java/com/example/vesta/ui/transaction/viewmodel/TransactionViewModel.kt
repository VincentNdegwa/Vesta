package com.example.vesta.ui.transaction.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vesta.data.repository.TransactionRepository
import com.example.vesta.data.local.entities.TransactionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import javax.inject.Inject

data class TransactionUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isTransactionSaved: Boolean = false,
    val categories: List<String> = emptyList()
)

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()
    
    fun addTransaction(
        amount: Double,
        type: String,
        category: String,
        date: String,
        note: String,
        userId: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val transaction = TransactionEntity(
                    userId = userId,
                    amount = amount,
                    type = type.lowercase(),
                    category = category,
                    description = note.ifBlank { null },
                    date = parseDate(date)
                )
                
                val result = transactionRepository.addTransaction(transaction)
                
                if (result.isSuccess) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isTransactionSaved = true
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to save transaction"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to save transaction: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun parseDate(dateString: String): Long {
        return try {
            val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            formatter.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    fun getStats(userId: String){
        val start_current_month = parseDate(Date.from(Instant.now()).toString())
        val now = parseDate(Date.from(Instant.now()).toString())

        val start_prev_month = parseDate(Date.from(Instant.now().minusMonths(1)).toString())
        val end_prev_month = parseDate(Date.from(Instant.now().minusMonths(1)).toString())

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            var current_month_income = transactionRepository.getTotalIncomeForPeriod(userId,start_current_month, now)
            var current_month_expense = transactionRepository.getTotalExpenseForPeriod(userId,start_current_month, now)

            var prev_month_income = transactionRepository.getTotalIncomeForPeriod(userId,start_prev_month, end_prev_month)
            var prev_month_expense = transactionRepository.getTotalExpenseForPeriod(userId,start_prev_month, end_prev_month)


        }

    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun resetTransactionSaved() {
        _uiState.update { it.copy(isTransactionSaved = false) }
    }
}
