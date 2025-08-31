package com.example.vesta.ui.savings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vesta.data.local.entities.*
import com.example.vesta.ui.auth.viewmodel.AuthViewModel
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

    val userId = authUiState.userId;
    LaunchedEffect(userId) {
        userId?.let { viewModel.loadGoals(it) }
    }

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
            if (uiState.activeGoals.isNotEmpty()) {
                item {
                    Text(
                        text = "Active Goals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(uiState.activeGoals) { goal ->
                    SavingsGoalCard(
                        goal = goal,
                        onContribute = { amount ->
                            if (userId != null) {
                                viewModel.addContribution(goal.id, userId, amount)
                            }
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
                    SavingsGoalCard(
                        goal = goal,
                        onContribute = { amount ->
                            if (userId != null) {
                                viewModel.addContribution(goal.id, userId, amount)
                            }
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
fun SavingsGoalCard(
    goal: SavingsGoalEntity,
    onContribute: (Double) -> Unit
) {
    var showContributeDialog by remember { mutableStateOf(false) }

    if (showContributeDialog) {
        ContributeDialog(
            goalName = goal.name,
            onDismiss = { showContributeDialog = false },
            onContribute = { amount ->
                onContribute(amount)
                showContributeDialog = false
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showContributeDialog = true },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (goal.status == GoalStatus.COMPLETED) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = (goal.currentAmount / goal.targetAmount).toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatAmount(goal.currentAmount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = formatAmount(goal.targetAmount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (goal.autoContribute) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoMode,
                        contentDescription = "Auto-contribute",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = when {
                            goal.autoContributeAmount != null -> 
                                "Auto: ${formatAmount(goal.autoContributeAmount)}/month"
                            goal.autoContributePercentage != null -> 
                                "Auto: ${goal.autoContributePercentage}% of income"
                            else -> "Auto-contribute enabled"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
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
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
