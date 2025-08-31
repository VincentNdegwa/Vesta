package com.example.vesta.ui.savings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vesta.data.local.entities.*
import com.example.vesta.data.repositories.GoalProgress
import com.example.vesta.data.repositories.SavingsGoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.*

@HiltViewModel
class SavingsGoalViewModel @Inject constructor(
    private val repository: SavingsGoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavingsGoalUiState())
    val uiState: StateFlow<SavingsGoalUiState> = _uiState.asStateFlow()

    fun loadGoals(userId: String) {
        viewModelScope.launch {
            repository.getActiveSavingsGoals(userId)
                .combine(repository.getSavingsGoals(userId)) { activeGoals, allGoals ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            activeGoals = activeGoals,
                            completedGoals = allGoals.filter { it.status == GoalStatus.COMPLETED }
                        )
                    }
                }.collect()
        }
    }

    fun createGoal(
        userId: String,
        name: String,
        targetAmount: Double,
        deadline: Long,
        autoContribute: Boolean = false,
        autoContributeAmount: Double? = null,
        autoContributePercentage: Double? = null,
        priority: Int = 2
    ) {
        viewModelScope.launch {
            val goal = SavingsGoalEntity(
                userId = userId,
                name = name,
                targetAmount = targetAmount,
                deadline = deadline,
                autoContribute = autoContribute,
                autoContributeAmount = autoContributeAmount,
                autoContributePercentage = autoContributePercentage,
                priority = priority
            )
            repository.createSavingsGoal(goal)
        }
    }

    fun addContribution(
        goalId: String,
        userId: String,
        amount: Double,
        type: String = "MANUAL"
    ) {
        viewModelScope.launch {
            repository.addContribution(
                goalId = goalId,
                userId = userId,
                amount = amount,
                type = type
            )
        }
    }

    fun updateGoalStatus(goal: SavingsGoalEntity, newStatus: GoalStatus) {
        viewModelScope.launch {
            repository.updateSavingsGoal(goal.copy(status = newStatus))
        }
    }

    fun deleteGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
        }
    }

    fun getGoalProgress(goalId: String) {
        viewModelScope.launch {
            repository.getGoalProgress(goalId)
                .collect { progress ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            selectedGoalProgress = progress
                        )
                    }
                }
        }
    }

    fun loadContributions(goalId: String) {
        viewModelScope.launch {
            repository.getContributionsForGoal(goalId)
                .collect { contributions ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            selectedGoalContributions = contributions
                        )
                    }
                }
        }
    }
}

data class SavingsGoalUiState(
    val activeGoals: List<SavingsGoalEntity> = emptyList(),
    val completedGoals: List<SavingsGoalEntity> = emptyList(),
    val selectedGoalProgress: GoalProgress? = null,
    val selectedGoalContributions: List<SavingsContributionEntity> = emptyList(),
    val error: String? = null
)
