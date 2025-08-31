package com.example.vesta.ui.budget
import com.example.vesta.ui.category.CategoryViewModel
import com.example.vesta.data.local.entities.CategoryEntity

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vesta.ui.budget.BudgetViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vesta.ui.auth.viewmodel.AuthViewModel
import com.example.vesta.ui.components.HideableText
import com.example.vesta.ui.security.viewmodel.SecurityViewModel
import com.example.vesta.ui.theme.VestaTheme

data class BudgetCategory(
    val name: String,
    val icon: ImageVector,
    val spent: Double,
    val budgetAmount: Double,
    val color: Color
) {
    val percentage: Int
        get() = if (budgetAmount > 0) ((spent / budgetAmount) * 100).toInt() else 0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onStartBudgeting: () -> Unit = {},
    onViewReports: () -> Unit = {},
    viewModel: BudgetViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    securityViewModel: SecurityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val categoryUiState by categoryViewModel.uiState.collectAsState()
    val securityUiState by securityViewModel.uiState.collectAsState()
    val budgets = uiState.currentPeriodBudgets
    val totalBudget = budgets.sumOf { it.targetAmount }
    val totalSpent = budgets.sumOf { it.spentAmount }
    val remaining = totalBudget - totalSpent
    val overallPercentage = if (totalBudget > 0) ((totalSpent / totalBudget) * 100).toInt() else 0

    LaunchedEffect(authUiState.userId) {
        authUiState.userId?.let {
            viewModel.loadBudgets(it)
            categoryViewModel.loadCategories(it)
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            BudgetTopBar(onBackClick = onBackClick)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            BudgetOverviewSection(
                totalBudget = totalBudget,
                totalSpent = totalSpent,
                remaining = remaining,
                percentage = overallPercentage,
                hideAmounts = securityUiState.hideAmounts
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
            ) {
                items(budgets) { budget ->
                    val category: CategoryEntity? = categoryUiState.categories.find { it.id == budget.categoryId }
                    val categoryName = category?.name ?: "Unknown"
                    val categoryColor = category?.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: MaterialTheme.colorScheme.primary
                    val categoryIcon = when (categoryName) {
                        "Food & Dining" -> Icons.Default.Restaurant
                        "Transportation" -> Icons.Default.DirectionsCar
                        "Bills & Utilities" -> Icons.Default.Lightbulb
                        "Healthcare" -> Icons.Default.LocalHospital
                        else -> Icons.Default.Assessment
                    }
                    BudgetCategoryItem(
                        category = BudgetCategory(
                            name = categoryName,
                            icon = categoryIcon,
                            spent = budget.spentAmount,
                            budgetAmount = budget.targetAmount,
                            color = categoryColor
                        ),
                        onBudgetChange = { }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onStartBudgeting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Budgeting",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onViewReports,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View Budget Reports",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetTopBar(
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Budget Setup",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onPrimary
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun BudgetOverviewSection(
    totalBudget: Double,
    totalSpent: Double,
    remaining: Double,
    percentage: Int,
    hideAmounts: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Budget",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    HideableText(
                        text = String.format("%,.0f", totalBudget),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                        hideAmounts = hideAmounts
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Remaining",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    HideableText(
                        text = String.format("%,.0f", remaining),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                        hideAmounts = hideAmounts
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spent: ${String.format("%,.0f", totalSpent)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { percentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
private fun BudgetCategoryItem(
    category: BudgetCategory,
    onBudgetChange: (Double) -> Unit
) {
    var budgetAmountText by remember { mutableStateOf(category.budgetAmount.toInt().toString()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Category Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                category.color.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = category.name,
                            tint = category.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$${"%.0f".format(category.spent)} of $${"%.0f".format(category.budgetAmount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Text(
                    text = "${category.percentage}%",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (category.percentage > 90) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))


            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { (category.percentage / 100f).coerceAtMost(1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (category.percentage > 90) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            )
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun BudgetScreenPreview() {
//    VestaTheme {
//        BudgetScreen()
//    }
//}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BudgetScreenDarkPreview() {
    VestaTheme {
        BudgetScreen()
    }
}
