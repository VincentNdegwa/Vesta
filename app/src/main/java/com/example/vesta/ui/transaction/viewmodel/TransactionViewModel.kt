package com.example.vesta.ui.transaction.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class TransactionUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isTransactionSaved: Boolean = false,
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val incomeChange: Double = 0.0,
    val expenseChange: Double = 0.0,
    val expenseByCategory: List<TransactionDao.CategoryExpenseSum> = emptyList(),
    val incomeByCategory: List<TransactionDao.CategoryExpenseSum> = emptyList(),
    val searchQuery: String = "",
    val selectedTypeFilter: String = "All Types",
    val selectedCategoryFilter: String = "All Categories",
    val selectedDateFilter: String = "All Time"
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
        // Reset time to start of day
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // Get start of current month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startCurrentMonth = calendar.timeInMillis
        
        // Get start of next month
        calendar.add(Calendar.MONTH, 1)
        val startNextMonth = calendar.timeInMillis

        // Get start of previous month
        calendar.add(Calendar.MONTH, -2)
        val startPrevMonth = calendar.timeInMillis
        
        // Get start of this month (same as startCurrentMonth)
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
                    incomeChange = BigDecimal.valueOf(incomeChange).setScale(2, RoundingMode.DOWN).toDouble(),
                    expenseChange = BigDecimal.valueOf(expenseChange).setScale(2, RoundingMode.DOWN).toDouble()
                )
            }
            Log.d("TransactionViewModel", "getStats: $currentMonthIncome, $currentMonthExpense, $incomeChange, $expenseChange")
        }
    }
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun resetTransactionSaved() {
        _uiState.update { it.copy(isTransactionSaved = false) }
    }

    fun loadTransactions(userId: String) {
        Log.d("TransactionViewModel", "Loading transactions for user: $userId")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                transactionRepository.getTransactions(userId).collect { transactions ->
                    Log.d("TransactionViewModel", "Received ${transactions.size} transactions")
                    _uiState.update { state ->
                        state.copy(
                            transactions = transactions,
                            filteredTransactions = filterTransactions(
                                transactions,
                                state.searchQuery,
                                state.selectedTypeFilter,
                                state.selectedCategoryFilter,
                                state.selectedDateFilter
                            ),
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "Error loading transactions", e)
                _uiState.update { it.copy(
                    error = "Failed to load transactions: ${e.message}",
                    isLoading = false
                ) }
            }
        }
    }

    private fun filterTransactions(
        transactions: List<TransactionEntity>,
        searchQuery: String,
        typeFilter: String,
        categoryFilter: String,
        dateFilter: String
    ): List<TransactionEntity> {
        Log.d("TransactionViewmodel", "$transactions")
        return transactions.filter { transaction ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                transaction.description?.contains(searchQuery, ignoreCase = true) ?: false
            }
            
            val matchesType = when (typeFilter) {
                "All Types" -> true
                "Income" -> transaction.type.equals("income", ignoreCase = true)
                "Expense" -> transaction.type.equals("expense", ignoreCase = true)
                else -> true
            }
            
            val matchesCategory = categoryFilter == "All Categories" || 
                                transaction.categoryId == categoryFilter

            val matchesDate = when (dateFilter) {
                "All Time" -> true
                "Today" -> isToday(transaction.date)
                "This Week" -> isThisWeek(transaction.date)
                "This Month" -> isThisMonth(transaction.date)
                else -> true
            }
            
            matchesSearch && matchesType && matchesCategory && matchesDate
        }
    }

    private fun isToday(timestamp: Long): Boolean {
        val today = Calendar.getInstance()
        val transactionDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        return today.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == transactionDate.get(Calendar.DAY_OF_YEAR)
    }

    private fun isThisWeek(timestamp: Long): Boolean {
        val today = Calendar.getInstance()
        val transactionDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        return today.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR) &&
               today.get(Calendar.WEEK_OF_YEAR) == transactionDate.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isThisMonth(timestamp: Long): Boolean {
        val today = Calendar.getInstance()
        val transactionDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        return today.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR) &&
               today.get(Calendar.MONTH) == transactionDate.get(Calendar.MONTH)
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredTransactions = filterTransactions(
                    state.transactions,
                    query,
                    state.selectedTypeFilter,
                    state.selectedCategoryFilter,
                    state.selectedDateFilter
                )
            )
        }
    }

    fun updateTypeFilter(type: String) {
        _uiState.update { state ->
            state.copy(
                selectedTypeFilter = type,
                filteredTransactions = filterTransactions(
                    state.transactions,
                    state.searchQuery,
                    type,
                    state.selectedCategoryFilter,
                    state.selectedDateFilter
                )
            )
        }
    }

    fun updateCategoryFilter(category: String) {
        _uiState.update { state ->
            state.copy(
                selectedCategoryFilter = category,
                filteredTransactions = filterTransactions(
                    state.transactions,
                    state.searchQuery,
                    state.selectedTypeFilter,
                    category,
                    state.selectedDateFilter
                )
            )
        }
    }

    fun updateDateFilter(date: String) {
        _uiState.update { state ->
            state.copy(
                selectedDateFilter = date,
                filteredTransactions = filterTransactions(
                    state.transactions,
                    state.searchQuery,
                    state.selectedTypeFilter,
                    state.selectedCategoryFilter,
                    date
                )
            )
        }
    }

    fun loadExpenseByCategoryForCurrentMonth(userId: String) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val end = calendar.timeInMillis

        viewModelScope.launch {
            val data = transactionRepository.getExpenseByCategoryForPeriod(userId, start, end)
            _uiState.update { it.copy(expenseByCategory = data) }
        }
    }

    fun loadIncomeByCategoryForCurrentMonth(userId: String) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val end = calendar.timeInMillis

        viewModelScope.launch {
            val data = transactionRepository.getIncomeByCategoryForPeriod(userId, start, end)
            _uiState.update { it.copy(incomeByCategory = data) }
        }
    }
}
