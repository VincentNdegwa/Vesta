package com.example.vesta.ui.budget

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.composed
import androidx.compose.runtime.remember
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vesta.ui.auth.viewmodel.AuthViewModel
import com.example.vesta.data.local.entities.BudgetPeriod
import com.example.vesta.ui.budget.BudgetViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSetupScreen(
    onBackClick: () -> Unit = {},
    viewModel: BudgetViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val periodOptions = listOf(BudgetPeriod.MONTHLY, BudgetPeriod.WEEKLY, BudgetPeriod.YEARLY, BudgetPeriod.DAILY, BudgetPeriod.CUSTOM)
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

    data class TransactionCategory(val name: String)
    val expenseCategories = listOf(
        TransactionCategory("Food & Dining"),
        TransactionCategory("Transportation"),
        TransactionCategory("Shopping"),
        TransactionCategory("Entertainment"),
        TransactionCategory("Bills & Utilities"),
        TransactionCategory("Healthcare"),
        TransactionCategory("Travel"),
        TransactionCategory("Education"),
        TransactionCategory("Groceries"),
        TransactionCategory("Other")
    )

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var periodDropdownExpanded by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(authUiState.userId) {
        authUiState.userId?.let { viewModel.loadBudgets(it) }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Set Budget",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            // Budget Name
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Budget Name") },
                modifier = Modifier.fillMaxWidth()
            )
            // Category Dropdown (required)
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = uiState.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    expenseCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = {
                                viewModel.onCategoryChange(cat.name)
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = if (uiState.targetAmount == 0.0) "" else uiState.targetAmount.toString(),
                onValueChange = {
                    val clean = it.replace(',', '.')
                    val value = clean.toDoubleOrNull() ?: 0.0
                    viewModel.onTargetAmountChange(value)
                },
                label = { Text("Target Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            // Period Dropdown
            ExposedDropdownMenuBox(
                expanded = periodDropdownExpanded,
                onExpandedChange = { periodDropdownExpanded = !periodDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = uiState.period.name.lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Period") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodDropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = periodDropdownExpanded,
                    onDismissRequest = { periodDropdownExpanded = false }
                ) {
                    periodOptions.forEach { period ->
                        DropdownMenuItem(
                            text = { Text(period.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                viewModel.onPeriodChange(period)
                                periodDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            // Show calculated period window or allow custom date pick
            val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

            if (uiState.period == BudgetPeriod.CUSTOM) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showStartDatePicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(dateFormatter.format(Date(uiState.startDate)))
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    }


                    Surface(
                        shape = MaterialTheme.shapes.small,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showEndDatePicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(dateFormatter.format(Date(uiState.endDate)))
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    }

                }
            }

            Text(
                text = "Period: ${dateFormatter.format(Date(uiState.startDate))} – ${dateFormatter.format(Date(uiState.endDate))}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (showStartDatePicker) {
                val startPickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.startDate)
                DatePickerDialog(
                    onDismissRequest = { showStartDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            startPickerState.selectedDateMillis?.let { viewModel.onStartDateChange(it) }
                            showStartDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
                    },
                    content = { DatePicker(state = startPickerState) }
                )
            }

            if (showEndDatePicker) {
                val endPickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.endDate)
                DatePickerDialog(
                    onDismissRequest = { showEndDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            endPickerState.selectedDateMillis?.let { viewModel.onEndDateChange(it) }
                            showEndDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
                    },
                    content = { DatePicker(state = endPickerState) }
                )

            }

            // Spent Amount (read-only, for edit mode/future use)
            if (uiState.spentAmount > 0.0) {
                Text(
                    text = "Spent: $${"%.2f".format(uiState.spentAmount)}",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            // Save Button
            Button(
                onClick = { authUiState.userId?.let { viewModel.saveBudget(it) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text("Save Budget")
            }
            if (uiState.isBudgetSaved) {
                Text(
                    text = "Budget saved!",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
