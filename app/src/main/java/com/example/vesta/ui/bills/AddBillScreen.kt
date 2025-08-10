package com.example.vesta.ui.bills

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vesta.data.local.entities.RecurrenceType
import com.example.vesta.ui.bills.viewmodel.BillViewModel
import com.example.vesta.ui.theme.VestaTheme
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vesta.ui.auth.viewmodel.AuthViewModel
import com.example.vesta.ui.components.DateInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBillScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    viewModel: BillViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var billName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    // Use current date as default value for due date
    var dueDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var dueDateString by remember { mutableStateOf("") } // For storing formatted date string if needed
    var category by remember { mutableStateOf("Bills") } // Default category
    var selectedRecurrenceType by remember { mutableStateOf(RecurrenceType.NONE) }
    var intervalCount by remember { mutableStateOf("1") }
    var timesPerPeriod by remember { mutableStateOf("") }
    
    var expanded by remember { mutableStateOf(false) }
    var showRecurrenceOptions by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Format the due date for display
    val dateFormatter = remember { SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()) }
    
    val uiState by viewModel.uiState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

    val userId = authUiState.userId
    // Categories
    val categories = listOf("Bills", "Utilities", "Rent", "Credit Card", "Insurance", "Subscription", "Other")
    
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Form validation
    val isFormValid = userId != null && 
                     billName.isNotBlank() && 
                     amount.isNotBlank() && 
                     amount.toDoubleOrNull() != null && 
                     dueDate > 0 &&
                     (selectedRecurrenceType == RecurrenceType.NONE || 
                      intervalCount.isNotBlank() && intervalCount.toIntOrNull() != null && intervalCount.toIntOrNull()!! > 0)
    
    // Handle success state
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            viewModel.resetSuccess()
            onBackClick()
        }
    }
    
    // Handle error state
    if (uiState.error != null) {
        Toast.makeText(LocalContext.current, uiState.error, Toast.LENGTH_SHORT).show()
        viewModel.clearError()
    }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            AddBillTopBar(onBackClick = onBackClick)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        // Scrollable form content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
                // Bill Name Field
                BillInputSection(
                    title = "Bill Name"
                ) {
                    OutlinedTextField(
                        value = billName,
                        onValueChange = { billName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "e.g., Credit Card Payment",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                }
                
                // Category dropdown
                BillInputSection(title = "Category") {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(text = option) },
                                    onClick = {
                                        category = option
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Amount Field
                BillInputSection(
                    title = "Amount"
                ) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { newAmount ->
                            // Only allow numbers and decimal point
                            if (newAmount.isEmpty() || newAmount.matches(Regex("^\\d*\\.?\\d*$"))) {
                                amount = newAmount
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "0.00",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                }
                
                // Due Date Field
                BillInputSection(
                    title = "Due Date"
                ) {
                    DateInput(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { showDatePicker = true }),
                        value = dueDate
                    )
                    
                    // Date Picker Dialog
                    if (showDatePicker) {
                        val datePickerState = rememberDatePickerState(
                            initialSelectedDateMillis = dueDate
                        )
                        
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        datePickerState.selectedDateMillis?.let { selectedDate ->
                                            dueDate = selectedDate
                                            dueDateString = dateFormatter.format(Date(selectedDate))
                                        }
                                        showDatePicker = false
                                    }
                                ) {
                                    Text("OK")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showDatePicker = false }
                                ) {
                                    Text("Cancel")
                                }
                            }
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }
                }
                
                // Recurrence Type Selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRecurrenceOptions = !showRecurrenceOptions }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Recurrence",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when (selectedRecurrenceType) {
                                        RecurrenceType.NONE -> "One-time bill"
                                        RecurrenceType.DAILY -> {
                                            val count = intervalCount.toIntOrNull() ?: 1
                                            if (count == 1) "Repeats daily" else "Repeats every $count days"
                                        }
                                        RecurrenceType.WEEKLY -> {
                                            val count = intervalCount.toIntOrNull() ?: 1
                                            if (count == 1) "Repeats weekly" else "Repeats every $count weeks"
                                        }
                                        RecurrenceType.MONTHLY -> {
                                            val count = intervalCount.toIntOrNull() ?: 1
                                            if (count == 1) "Repeats monthly" else "Repeats every $count months"
                                        }
                                        RecurrenceType.YEARLY -> {
                                            val count = intervalCount.toIntOrNull() ?: 1
                                            if (count == 1) "Repeats yearly" else "Repeats every $count years"
                                        }
                                        RecurrenceType.CUSTOM -> "Custom recurrence"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = "Expand recurrence options",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(24.dp)
                            )
                    }
                    
                    // Recurrence Options
                    AnimatedVisibility(visible = showRecurrenceOptions) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Recurrence Type",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            Text(
                                text = "Choose how often this bill repeats",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Column(modifier = Modifier.selectableGroup()) {
                                RecurrenceType.values().forEach { type ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .selectable(
                                                selected = (type == selectedRecurrenceType),
                                                onClick = { 
                                                    selectedRecurrenceType = type 
                                                    // Reset these values when changing recurrence type
                                                    if (type == RecurrenceType.NONE) {
                                                        intervalCount = "1"
                                                        timesPerPeriod = ""
                                                    }
                                                },
                                                role = Role.RadioButton
                                            )
                                            .padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = (type == selectedRecurrenceType),
                                            onClick = null // null because we're handling selection in the Row
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val displayText = when (type) {
                                            RecurrenceType.NONE -> "One-time (No recurrence)"
                                            RecurrenceType.DAILY -> "Daily"
                                            RecurrenceType.WEEKLY -> "Weekly"
                                            RecurrenceType.MONTHLY -> "Monthly"
                                            RecurrenceType.YEARLY -> "Yearly"
                                            RecurrenceType.CUSTOM -> "Custom"
                                        }
                                        Text(
                                            text = displayText,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                            
                            // Interval Count (only show if not NONE)
                            AnimatedVisibility(visible = selectedRecurrenceType != RecurrenceType.NONE) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                ) {
                                    Text(
                                        text = "Repeat every",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    
                                    Text(
                                        text = when (selectedRecurrenceType) {
                                            RecurrenceType.DAILY -> "How many days between occurrences"
                                            RecurrenceType.WEEKLY -> "How many weeks between occurrences"
                                            RecurrenceType.MONTHLY -> "How many months between occurrences"
                                            RecurrenceType.YEARLY -> "How many years between occurrences"
                                            else -> "Frequency of recurrence"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        val isIntervalError = selectedRecurrenceType != RecurrenceType.NONE &&
                                            (intervalCount.isBlank() || intervalCount.toIntOrNull() == null || intervalCount.toIntOrNull()!! <= 0)
                                        
                                        OutlinedTextField(
                                            value = intervalCount,
                                            onValueChange = { 
                                                if (it.isEmpty() || it.toIntOrNull() != null) {
                                                    intervalCount = it 
                                                }
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.width(80.dp),
                                            singleLine = true,
                                            isError = isIntervalError,
                                            supportingText = if (isIntervalError) {
                                                { Text("Required") }
                                            } else null,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                errorBorderColor = MaterialTheme.colorScheme.error
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        Text(
                                            text = when (selectedRecurrenceType) {
                                                RecurrenceType.DAILY -> if (intervalCount == "1") "day" else "days"
                                                RecurrenceType.WEEKLY -> if (intervalCount == "1") "week" else "weeks"
                                                RecurrenceType.MONTHLY -> if (intervalCount == "1") "month" else "months"
                                                RecurrenceType.YEARLY -> if (intervalCount == "1") "year" else "years"
                                                else -> ""
                                            }
                                        )
                                    }
                                }
                            }
                            
                            // Times per period (only show for appropriate recurrence types)
                            AnimatedVisibility(
                                visible = selectedRecurrenceType != RecurrenceType.NONE && 
                                        selectedRecurrenceType != RecurrenceType.YEARLY &&
                                        selectedRecurrenceType != RecurrenceType.CUSTOM
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                ) {
                                    val periodText = when (selectedRecurrenceType) {
                                        RecurrenceType.DAILY -> "day"
                                        RecurrenceType.WEEKLY -> "week"
                                        RecurrenceType.MONTHLY -> "month"
                                        else -> "period"
                                    }
                                    
                                    Text(
                                        text = "How many times per $periodText?",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    
                                    Text(
                                        text = "Example: For a bill due 3 times in a week, enter 3",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    
                                    OutlinedTextField(
                                        value = timesPerPeriod,
                                        onValueChange = { 
                                            if (it.isEmpty() || (it.toIntOrNull() != null && it.toIntOrNull()!! > 0)) {
                                                timesPerPeriod = it 
                                            }
                                        },
                                        placeholder = { Text("Leave blank for single occurrence") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                            focusedContainerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                            
                            // Custom recurrence options (only show if CUSTOM is selected)
                            AnimatedVisibility(visible = selectedRecurrenceType == RecurrenceType.CUSTOM) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                ) {
                                    Text(
                                        text = "Custom Recurrence Settings",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    
                                    Text(
                                        text = "For custom recurrence patterns, please specify the details below.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                    
                                    // Additional custom recurrence settings can be added here
                                    // This is a placeholder for future custom recurrence implementation
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Action Buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Add Bill Button
                    Button(
                        onClick = {
                            if (isFormValid && userId != null) {
                                // Validate times per period for recurring bills
                                val finalTimesPerPeriod = when {
                                    selectedRecurrenceType == RecurrenceType.NONE -> null
                                    timesPerPeriod.isBlank() -> null
                                    else -> timesPerPeriod.toIntOrNull()?.let { 
                                        if (it > 0) it else null 
                                    }
                                }
                                
                                // Use default interval of 1 if not specified or invalid
                                val finalIntervalCount = if (intervalCount.isBlank()) {
                                    1
                                } else {
                                    intervalCount.toIntOrNull() ?: 1
                                }
                                
                                // Add the bill reminder with all recurrence details
                                viewModel.addBillReminder(
                                    userId = userId,
                                    title = billName,
                                    amount = amount.toDoubleOrNull() ?: 0.0,
                                    category = category,
                                    dueDate = dueDate, // Now passing the timestamp directly
                                    recurrenceType = selectedRecurrenceType,
                                    intervalCount = finalIntervalCount,
                                    timesPerPeriod = finalTimesPerPeriod
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = isFormValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Add Bill",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (isFormValid) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Cancel Button
                    OutlinedButton(
                        onClick = onCancelClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        ),
                        border = null,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
                
            // Bottom spacing
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBillTopBar(
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Add New Bill",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
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

@Composable
private fun BillInputSection(
    title: String,
    placeholder: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        content()
    }
}

//@Preview(showBackground = true)
//@Composable
//fun AddBillScreenPreview() {
//    VestaTheme {
//        AddBillScreen()
//    }
//}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddBillScreenDarkPreview() {
    VestaTheme {
        AddBillScreen()
    }
}
