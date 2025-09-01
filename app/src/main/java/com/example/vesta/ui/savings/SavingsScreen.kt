package com.example.vesta.ui.savings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vesta.data.local.entities.*
import com.example.vesta.ui.auth.viewmodel.AuthViewModel
import com.example.vesta.ui.savings.components.*
import com.example.vesta.ui.savings.dialogs.GoalDetailsDialog
import com.example.vesta.ui.savings.dialogs.ManageRulesDialog
import com.example.vesta.ui.savings.viewmodel.SavingsGoalViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    viewModel: SavingsGoalViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()
    var showAddGoalDialog by remember { mutableStateOf(false) }

    var selectedGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var showContributeDialog by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    val userId = authUiState.userId
    LaunchedEffect(userId) {
        userId?.let { viewModel.loadGoals(it) }
    }

    // Show Dialogs
    if (showAddGoalDialog) {
        AddSavingsGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onCreateGoal = { name, amount, deadline, autoContribute, autoAmount, autoPercentage ->
                if (userId != null) {
                    viewModel.createGoal(
                        userId = userId,
                        name = name,
                        targetAmount = amount,
                        deadline = deadline,
                        autoContribute = autoContribute,
                        autoContributeAmount = autoAmount,
                        autoContributePercentage = autoPercentage
                    )
                }
                showAddGoalDialog = false
            }
        )
    }

    selectedGoal?.let { goal ->
        if (showRulesDialog) {
            ManageRulesDialog(
                goalId = goal.id,
                rules = uiState.selectedGoalRules,
                onCreateRule = { viewModel.createSavingsRule(
                    goalId = it.goalId,
                    type = it.type,
                    frequency = it.frequency,
                    amount = it.amount,
                    percentage = it.percentage,
                    minimumIncomeThreshold = it.minimumIncomeThreshold,
                    maximumContribution = it.maximumContribution,
                    description = it.description
                )},
                onToggleRule = viewModel::toggleSavingsRule,
                onEditRule = { rule -> viewModel.updateSavingsRule(rule) },
                onDeleteRule = { rule -> viewModel.deleteSavingsRule(rule) },
                onDismiss = { 
                    showRulesDialog = false
                    selectedGoal = null
                }
            )
        }

        if (showDetailsDialog) {
            GoalDetailsDialog(
                goal = goal,
                progress = uiState.selectedGoalProgress,
                contributions = uiState.selectedGoalContributions,
                onDismiss = { 
                    showDetailsDialog = false
                    selectedGoal = null
                }
            )
        }

        if (showContributeDialog) {
            ContributeDialog(
                onContribute = { amount ->
                    if (userId != null) {
                        viewModel.addContribution(
                            goalId = goal.id,
                            userId = userId,
                            amount = amount
                        )
                    }
                    showContributeDialog = false
                    selectedGoal = null
                },
                onDismiss = {
                    showContributeDialog = false
                    selectedGoal = null
                },
                goalName = goal.name
            )
        }
    }

    LaunchedEffect(selectedGoal) {
        selectedGoal?.let { goal ->
            viewModel.getGoalProgress(goal.id)
            viewModel.loadContributions(goal.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Savings Goals",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddGoalDialog = true }) {
                        Icon(Icons.Default.Add, "Add Goal")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Behind schedule goals
            if (uiState.behindScheduleGoals.isNotEmpty()) {
                item {
                    Text(
                        text = "Needs Attention",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                items(uiState.behindScheduleGoals) { goal ->
                    SmartGoalCard(
                        goal = goal,
                        onContribute = {
                            selectedGoal = goal
                            showContributeDialog = true
                        },
                        onManageRules = {
                            selectedGoal = goal
                            showRulesDialog = true
                        },
                        onShowDetails = {
                            selectedGoal = goal
                            showDetailsDialog = true
                        }
                    )
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Active goals
            if (uiState.activeGoals.isNotEmpty()) {
                item {
                    Text(
                        text = "Active Goals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(uiState.activeGoals) { goal ->
                    SmartGoalCard(
                        goal = goal,
                        onContribute = {
                            selectedGoal = goal
                            showContributeDialog = true
                        },
                        onManageRules = {
                            selectedGoal = goal
                            showRulesDialog = true
                        },
                        onShowDetails = {
                            selectedGoal = goal
                            showDetailsDialog = true
                        }
                    )
                }
            }

            if (uiState.completedGoals.isNotEmpty()) {
                item {
                    Text(
                        text = "Completed Goals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                items(uiState.completedGoals) { goal ->
                    SmartGoalCard(
                        goal = goal,
                        onContribute = {
                            selectedGoal = goal
                            showContributeDialog = true
                        },
                        onManageRules = {
                            selectedGoal = goal
                            showRulesDialog = true
                        },
                        onShowDetails = {
                            selectedGoal = goal
                            showDetailsDialog = true
                        }
                    )
                }
            }

            if (uiState.activeGoals.isEmpty() && uiState.completedGoals.isEmpty()) {
                item {
                    EmptyGoalsPrompt(
                        onClick = { showAddGoalDialog = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyGoalsPrompt(
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Savings,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No savings goals yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Create your first savings goal to start tracking your progress",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Goal")
        }
    }
}

private fun formatAmount(amount: Double): String {
    return NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance("USD")
    }.format(amount)
}
