package com.example.vesta.ui.transaction.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vesta.data.local.dao.TransactionDao
import com.example.vesta.data.repository.TransactionRepository
import com.example.vesta.data.repositories.SavingsGoalRepository
import com.example.vesta.data.local.entities.TransactionEntity
import com.example.vesta.data.local.entities.CategoryEntity
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
    val categories: List<CategoryEntity> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val incomeChange: Double = 0.0,
    val expenseChange: Double = 0.0,
    val expenseByCategory: List<TransactionDao.CategoryExpenseSum> = emptyList(),
    val incomeByCategory: List<TransactionDao.CategoryExpenseSum> = emptyList()
)

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val savingsGoalRepository: SavingsGoalRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()
    
    fun addTransaction(
        amount: Double,
        type: String,
        categoryId: String,
        date: String,
        note: String,
        userId: String,
        accountId: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val transaction = TransactionEntity(
                    userId = userId,
                    accountId = accountId,
                    amount = amount,
                    type = type.lowercase(),
                    categoryId = categoryId,
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

                    if (type.lowercase() == "income") {
                        savingsGoalRepository.processAutoContributions(
                            userId = userId,
                            transactionId = transaction.id,
                            amount = amount
                        )
                    }

                    loadExpenseByCategoryForCurrentMonth(userId)
                    loadIncomeByCategoryForCurrentMonth(userId)
                    getStats(userId)
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

    fun getStats(userId: String) {
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startCurrentMonth = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startNextMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, -2)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startPrevMonth = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val startThisMonth = calendar.timeInMillis

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val currentMonthIncome = transactionRepository.getTotalIncomeForPeriod(userId, startCurrentMonth, startNextMonth)
            val currentMonthExpense = transactionRepository.getTotalExpenseForPeriod(userId, startCurrentMonth, startNextMonth)

            val prevMonthIncome = transactionRepository.getTotalIncomeForPeriod(userId, startPrevMonth, startThisMonth)
            val prevMonthExpense = transactionRepository.getTotalExpenseForPeriod(userId, startPrevMonth, startThisMonth)

            val incomeChange = if (prevMonthIncome > 0) ((currentMonthIncome - prevMonthIncome) / prevMonthIncome) * 100 else 0.0
            val expenseChange = if (prevMonthExpense > 0) ((currentMonthExpense - prevMonthExpense) / prevMonthExpense) * 100 else 0.0

            _uiState.update {
                it.copy(
                    isLoading = false,
                    totalIncome = currentMonthIncome,
                    totalExpense = currentMonthExpense,
                    incomeChange = incomeChange,
                    expenseChange = expenseChange
                )
            }
        }
    }
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun resetTransactionSaved() {
        _uiState.update { it.copy(isTransactionSaved = false) }
    }

    fun loadExpenseByCategoryForCurrentMonth(userId: String) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val start = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val end = calendar.timeInMillis
        viewModelScope.launch {
            val data = transactionRepository.getExpenseByCategoryForPeriod(userId, start, end)
            _uiState.update { it.copy(expenseByCategory = data) }
        }
    }

    fun loadIncomeByCategoryForCurrentMonth(userId: String) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val start = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val end = calendar.timeInMillis
        viewModelScope.launch {
            val data = transactionRepository.getIncomeByCategoryForPeriod(userId, start, end)
            _uiState.update { it.copy(incomeByCategory = data) }
        }
    }
}
