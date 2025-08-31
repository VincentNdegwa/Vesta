package com.example.vesta.ui.savings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vesta.data.local.entities.*
import com.example.vesta.data.repositories.GoalProgress
import com.example.vesta.data.repositories.SavingsGoalRepository
import com.example.vesta.data.services.SmartSavingsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.*

@HiltViewModel
class SavingsGoalViewModel @Inject constructor(
    private val repository: SavingsGoalRepository,
    private val smartSavingsService: SmartSavingsService
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
        description: String? = null,
        targetAmount: Double,
        deadline: Long,
        savingsRules: List<SavingsRuleEntity> = emptyList(),
        priority: Int = 2,
        autoContribute: Boolean = false,
        autoContributeAmount: Double? = null,
        autoContributePercentage: Double? = null
    ) {
        viewModelScope.launch {
            val goal = SavingsGoalEntity(
                userId = userId,
                name = name,
                description = description,
                targetAmount = targetAmount,
                deadline = deadline,
                priority = priority,
                autoContribute = autoContribute,
                autoContributeAmount = autoContributeAmount,
                autoContributePercentage = autoContributePercentage
            )
            val goalId = repository.createSavingsGoal(goal)
            
            // Create savings rules if provided
            savingsRules.forEach { rule ->
                repository.createSavingsRule(rule.copy(goalId = goalId))
            }
            
            // Initialize smart analysis
            smartSavingsService.processSavingsRules(
                userId = userId,
                currentIncome = 0.0, // Will be updated when income is added
                monthlyIncome = repository.getAverageMonthlyIncome(userId),
                monthlyExpenses = repository.getAverageMonthlyExpenses(userId)
            )
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


    // Smart features
    fun updateSmartAnalysis(userId: String, currentIncome: Double? = null) {
        viewModelScope.launch {
            val monthlyIncome = repository.getAverageMonthlyIncome(userId)
            val monthlyExpenses = repository.getAverageMonthlyExpenses(userId)

            smartSavingsService.processSavingsRules(
                userId = userId,
                currentIncome = currentIncome ?: monthlyIncome,
                monthlyIncome = monthlyIncome,
                monthlyExpenses = monthlyExpenses
            )
        }
    }

    fun getBehindScheduleGoals(userId: String) {
        viewModelScope.launch {
            repository.getBehindScheduleGoals(userId)
                .collect { goals ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            behindScheduleGoals = goals
                        )
                    }
                }
        }
    }

    fun getConsistentGoals(userId: String) {
        viewModelScope.launch {
            repository.getConsistentGoals(userId)
                .collect { goals ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            consistentGoals = goals
                        )
                    }
                }
        }
    }

    fun createSavingsRule(
        goalId: String,
        type: RuleType,
        frequency: RuleFrequency,
        amount: Double? = null,
        percentage: Double? = null,
        minimumIncomeThreshold: Double? = null,
        maximumContribution: Double? = null,
        description: String
    ) {
        viewModelScope.launch {
            val rule = SavingsRuleEntity(
                goalId = goalId,
                type = type,
                frequency = frequency,
                amount = amount,
                percentage = percentage,
                minimumIncomeThreshold = minimumIncomeThreshold,
                maximumContribution = maximumContribution,
                description = description
            )
            repository.createSavingsRule(rule)
        }
    }

    fun toggleSavingsRule(ruleId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleSavingsRule(ruleId, enabled)
        }
    }

    fun updateSavingsRule(rule: SavingsRuleEntity) {
        viewModelScope.launch {
            repository.updateSavingsRule(rule)
        }
    }

    fun deleteSavingsRule(rule: SavingsRuleEntity) {
        viewModelScope.launch {
            repository.deleteSavingsRule(rule)
        }
    }

}


data class SavingsGoalUiState(
    val activeGoals: List<SavingsGoalEntity> = emptyList(),
    val completedGoals: List<SavingsGoalEntity> = emptyList(),
    val behindScheduleGoals: List<SavingsGoalEntity> = emptyList(),
    val consistentGoals: List<SavingsGoalEntity> = emptyList(),
    val selectedGoalProgress: GoalProgress? = null,
    val selectedGoalContributions: List<SavingsContributionEntity> = emptyList(),
    val selectedGoalRules: List<SavingsRuleEntity> = emptyList(),
    val error: String? = null,
    val showMessage: String? = null,
    val isLoading: Boolean = false
)
