package com.example.vesta.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vesta.data.local.entities.BudgetEntity
import com.example.vesta.data.local.entities.BudgetPeriod
import com.example.vesta.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject



data class BudgetUiState(
    val budgets: List<BudgetEntity> = emptyList(),
    val currentPeriodBudgets: List<BudgetEntity> = emptyList(),
    val name: String = "",
    val categoryId: String = "",
    val targetAmount: Double = 0.0,
    val spentAmount: Double = 0.0,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val startDate: Long = getDefaultPeriodStart(BudgetPeriod.MONTHLY),
    val endDate: Long = getDefaultPeriodEnd(BudgetPeriod.MONTHLY),
    val isLoading: Boolean = false,
    val isBudgetSaved: Boolean = false,
    val error: String? = null
)

fun getDefaultPeriodStart(period: BudgetPeriod): Long {
    val cal = Calendar.getInstance()
    when (period) {
        BudgetPeriod.DAILY -> {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        BudgetPeriod.WEEKLY -> {
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        BudgetPeriod.MONTHLY -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        BudgetPeriod.YEARLY -> {
            cal.set(Calendar.MONTH, 0)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        BudgetPeriod.CUSTOM -> {
            // Use today as default
        }
    }
    return cal.timeInMillis
}

fun getDefaultPeriodEnd(period: BudgetPeriod): Long {
    val cal = Calendar.getInstance()
    when (period) {
        BudgetPeriod.DAILY -> {
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
        }
        BudgetPeriod.WEEKLY -> {
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            cal.add(Calendar.DAY_OF_WEEK, 6)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
        }
        BudgetPeriod.MONTHLY -> {
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
        }
        BudgetPeriod.YEARLY -> {
            cal.set(Calendar.MONTH, 11)
            cal.set(Calendar.DAY_OF_MONTH, 31)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
        }
        BudgetPeriod.CUSTOM -> {
            // Use today as default
        }
    }
    return cal.timeInMillis
}

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    fun loadBudgets(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val budgets = budgetRepository.getBudgets(userId)
            val now = System.currentTimeMillis()
            val currentPeriodBudgets = budgetRepository.getCurrentPeriodBudgets(userId, now)
            _uiState.update { it.copy(budgets = budgets, currentPeriodBudgets = currentPeriodBudgets, isLoading = false) }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onCategoryIdChange(categoryId: String) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun onTargetAmountChange(amount: Double) {
        _uiState.update { it.copy(targetAmount = amount) }
    }

    fun onPeriodChange(period: BudgetPeriod) {
        _uiState.update {
            it.copy(
                period = period,
                startDate = getDefaultPeriodStart(period),
                endDate = getDefaultPeriodEnd(period)
            )
        }
    }

    fun onStartDateChange(date: Long) {
        _uiState.update { it.copy(startDate = date) }
    }

    fun onEndDateChange(date: Long) {
        _uiState.update { it.copy(endDate = date) }
    }

    fun saveBudget(userId: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val amount = state.targetAmount
            if (state.name.isBlank() || state.categoryId.isBlank() || amount <= 0) {
                _uiState.update { it.copy(error = "Please enter a valid name, category, and amount.") }
                return@launch
            }
            val budget = BudgetEntity(
                userId = userId,
                name = state.name,
                categoryId = state.categoryId,
                targetAmount = amount,
                period = state.period,
                startDate = state.startDate,
                endDate = state.endDate
            )
            budgetRepository.insertBudget(budget)
            _uiState.update { it.copy(isBudgetSaved = true, error = null) }
            loadBudgets(userId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetBudgetSaved() {
        _uiState.update { it.copy(isBudgetSaved = false) }
    }
    
    fun onExpenseAdded(userId: String, category: String, amount: Double, date: Long) {
        viewModelScope.launch {
            budgetRepository.addExpenseToBudgetByCategoryId(userId, category, amount, date)
            loadBudgets(userId)
        }
    }

}
