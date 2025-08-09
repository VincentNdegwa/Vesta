package com.example.vesta.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vesta.data.local.entities.BudgetEntity
import com.example.vesta.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

sealed class BudgetPeriod(val label: String) {
    object Monthly : BudgetPeriod("Monthly")
    object Weekly : BudgetPeriod("Weekly")
    object Yearly : BudgetPeriod("Yearly")
    companion object {
        fun fromString(period: String): BudgetPeriod = when (period.lowercase()) {
            "weekly" -> Weekly
            "yearly" -> Yearly
            else -> Monthly
        }
    }
}

data class BudgetUiState(
    val budgets: List<BudgetEntity> = emptyList(),
    val name: String = "",
    val category: String = "",
    val targetAmount: String = "",
    val period: BudgetPeriod = BudgetPeriod.Monthly,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val isBudgetSaved: Boolean = false,
    val error: String? = null
)

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
            _uiState.update { it.copy(budgets = budgets, isLoading = false) }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onCategoryChange(category: String) {
        _uiState.update { it.copy(category = category) }
    }

    fun onTargetAmountChange(amount: String) {
        _uiState.update { it.copy(targetAmount = amount) }
    }

    fun onPeriodChange(period: BudgetPeriod) {
        _uiState.update { it.copy(period = period) }
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
            val amount = state.targetAmount.toDoubleOrNull()
            if (state.name.isBlank() || amount == null || amount <= 0) {
                _uiState.update { it.copy(error = "Please enter a valid name and amount.") }
                return@launch
            }
            val budget = BudgetEntity(
                userId = userId,
                name = state.name,
                category = state.category,
                targetAmount = amount,
                period = state.period.label,
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
}
