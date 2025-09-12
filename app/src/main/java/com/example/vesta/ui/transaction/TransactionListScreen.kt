package com.example.vesta.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vesta.data.local.entities.TransactionEntity
import com.example.vesta.ui.auth.viewmodel.AuthViewModel
import com.example.vesta.ui.transaction.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    ) {
    val transactionUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(authUiState.userId) {
        authUiState.userId.let {it->
            if (!it.isNullOrBlank()){
                viewModel.loadTransactions(it)
            }
        }
    }

    val transactions = transactionUiState.filteredTransactions
    var showTypeMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showDateMenu by remember { mutableStateOf(false) }

    val typeOptions = listOf("All Types", "Income", "Expense")
    val dateOptions = listOf("All Time", "Today", "This Week", "This Month")
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Transaction",
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                // Search Bar
//                OutlinedTextField(
//                    value = transactionUiState.searchQuery,
//                    onValueChange = { viewModel.updateSearchQuery(it) },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(16.dp),
//                    placeholder = { Text("Search transactions...") }
//                )

                // Filters
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    // Type Filter
//                    Card(
//                        modifier = Modifier.fillMaxWidth(),
//                        shape = RoundedCornerShape(12.dp),
//                        colors = CardDefaults.cardColors(
//                            containerColor = MaterialTheme.colorScheme.surface
//                        ),
//                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
//                    ) {
//                        Row(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .clickable { showTypeMenu = true }
//                                .padding(16.dp),
//                            horizontalArrangement = Arrangement.SpaceBetween,
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Column {
//                                Text(
//                                    text = "Type",
//                                    style = MaterialTheme.typography.bodySmall,
//                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
//                                )
//                                Text(
//                                    text = transactionUiState.selectedTypeFilter,
//                                    style = MaterialTheme.typography.bodyLarge
//                                )
//                            }
//                            Icon(
//                                Icons.Default.KeyboardArrowDown,
//                                contentDescription = "Select type",
//                                tint = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                        }
//                        DropdownMenu(
//                            expanded = showTypeMenu,
//                            onDismissRequest = { showTypeMenu = false }
//                        ) {
//                            typeOptions.forEach { option ->
//                                DropdownMenuItem(
//                                    text = { Text(option) },
//                                    onClick = {
//                                        viewModel.updateTypeFilter(option)
//                                        showTypeMenu = false
//                                    }
//                                )
//                            }
//                        }
//                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Category Filter
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCategoryMenu = true }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Category",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = transactionUiState.selectedCategoryFilter,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select category",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showCategoryMenu,
                                onDismissRequest = { showCategoryMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Categories") },
                                    onClick = {
                                        viewModel.updateCategoryFilter("All Categories")
                                        showCategoryMenu = false
                                    }
                                )
                                transactionUiState.categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.name) },
                                        onClick = {
                                            viewModel.updateCategoryFilter(category.id)
                                            showCategoryMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Date Filter
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDateMenu = true }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Date",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = transactionUiState.selectedDateFilter,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select date",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showDateMenu,
                                onDismissRequest = { showDateMenu = false }
                            ) {
                                dateOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            viewModel.updateDateFilter(option)
                                            showDateMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Summary Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Income Card
                    Card(
                        modifier = Modifier
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                "Total Income",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val totalIncome = transactions
                                .filter { it.type.equals("income", ignoreCase = true) }
                                .sumOf { it.amount }
                            Text(
                                text="$totalIncome",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Total Expenses Card
                    Card(
                        modifier = Modifier
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                "Total Expenses",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val totalExpenses = transactions
                                .filter { it.type.equals("expense", ignoreCase = true) }
                                .sumOf { it.amount }
                            Text(
                                text="$totalExpenses",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

            }

            // List section with background color
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = MaterialTheme.colorScheme.background
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions) { transaction ->
                        TransactionCard(transaction = transaction)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionCard(transaction: TransactionEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = {  }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                    Text(
                        text = transaction.description ?: "Transaction",
                    style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                Spacer(modifier = Modifier.height(4.dp))
                        Text(
                    text = transaction.categoryId,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDate(transaction.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
            Text(
                text = "${transaction.amount}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (transaction.type.equals("income", ignoreCase = true))
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return format.format(date)
}

private fun formatAmount(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    return if (amount >= 0) format.format(amount) else format.format(-amount)
}