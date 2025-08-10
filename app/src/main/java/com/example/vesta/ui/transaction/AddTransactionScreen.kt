package com.example.vesta.ui.transaction
import com.example.vesta.ui.category.CategoryViewModel
import com.example.vesta.data.local.entities.CategoryEntity

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vesta.ui.auth.viewmodel.AuthViewModel
import com.example.vesta.ui.transaction.viewmodel.TransactionViewModel
import com.example.vesta.ui.account.viewmodel.AccountViewModel
import com.example.vesta.ui.theme.VestaTheme
import java.text.SimpleDateFormat
import java.util.*

private fun getCurrentDate(): String {
    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    return formatter.format(Date())
}

data class TransactionCategory(
    val name: String,
    val color: Color = Color.Transparent
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onSaveTransaction: () -> Unit = { -> },
    transactionViewModel: TransactionViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    accountViewModel: AccountViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    var amount by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }
    var selectedCategoryId by remember { mutableStateOf("") }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(getCurrentDate()) }
    var note by remember { mutableStateOf("") }
    var showBottomSheet by remember { mutableStateOf(false) }

    val transactionUiState by transactionViewModel.uiState.collectAsStateWithLifecycle()
    val categoryUiState by categoryViewModel.uiState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()

    // Load accounts when userId is available
    LaunchedEffect(authUiState.userId) {
        authUiState.userId?.let {
            accountViewModel.loadAccounts(it)
            categoryViewModel.loadCategories(it)
        }
    }
    var selectedAccountId by remember { mutableStateOf("") }
    var showAccountDropdown by remember { mutableStateOf(false) }
    
    // Handle successful transaction save
    LaunchedEffect(transactionUiState.isTransactionSaved) {
        if (transactionUiState.isTransactionSaved) {
            onBackClick() // Navigate back to previous screen
            transactionViewModel.resetTransactionSaved()
        }
    }

    LaunchedEffect(accounts) {
        when(accounts.size){
            1-> selectedAccountId = accounts[0].id
            else-> selectedAccountId = ""
        }
    }

    val categories = if (isExpense) categoryUiState.expenseCategories else categoryUiState.incomeCategories

    Scaffold(
        modifier = modifier,
        topBar = {
            AddTransactionTopBar(onBackClick = onBackClick)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Error Display
            if (transactionUiState.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = transactionUiState.error!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }


            // Amount Section
            AmountSection(
                amount = amount,
                onAmountChange = { 
                    amount = it
                    if (transactionUiState.error != null) {
                        transactionViewModel.clearError()
                    }
                }
            )
            // Expense/Income Toggle
            TransactionTypeToggle(
                isExpense = isExpense,
                onTypeChange = {
                    isExpense = it
                    selectedCategoryId = ""
                }
            )

            // Account Selection
            AccountDropdownSection(
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                onAccountSelected = { selectedAccountId = it },
                showDropdown = showAccountDropdown,
                onDropdownClick = { showAccountDropdown = true },
                onDismiss = { showAccountDropdown = false }
            )


            // Category Selection
            CategorySection(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategoryClick = { showBottomSheet = true }
            )

            // Date Selection
            DateSection(
                selectedDate = selectedDate,
                onDateChange = { selectedDate = it }
            )

            // Note Section
            NoteSection(
                note = note,
                onNoteChange = { note = it }
            )

            // Receipt Upload Section
            ReceiptUploadSection()

            // Save Button
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    val type = if (isExpense) "expense" else "income"
                    val userId = authUiState.userId ?: ""
                    if (userId.isNotEmpty() && selectedAccountId.isNotEmpty()) {
                        transactionViewModel.addTransaction(
                            amount = amountValue,
                            type = type,
                            categoryId = selectedCategoryId,
                            date = selectedDate,
                            note = note,
                            userId = userId,
                            accountId = selectedAccountId
                        )
                        onSaveTransaction()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !transactionUiState.isLoading && 
                         amount.isNotBlank() &&
                        selectedCategoryId.isNotBlank() &&
                         authUiState.userId != null &&
                         selectedAccountId.isNotBlank()
            ) {
                if (transactionUiState.isLoading) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Saving...",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    Text(
                        text = "Save Transaction",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Category Selection Bottom Sheet
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            CategoryBottomSheet(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { categoryId ->
                    selectedCategoryId = categoryId
                    showBottomSheet = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionTopBar(
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Add Transaction",
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
private fun AmountSection(
    amount: String,
    onAmountChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Amount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                BasicTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.widthIn(min = 100.dp)
                ) { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (amount.isEmpty()) {
                            Text(
                                text = "0.00",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                        innerTextField()
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionTypeToggle(
    isExpense: Boolean,
    onTypeChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Expense Button
        Button(
            onClick = { onTypeChange(true) },
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isExpense) MaterialTheme.colorScheme.error 
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                contentColor = if (isExpense) Color.White 
                              else MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = "Expense",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }

        // Income Button
        Button(
            onClick = { onTypeChange(false) },
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (!isExpense) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                contentColor = if (!isExpense) Color.White 
                              else MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = "Income",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun CategorySection(
    categories: List<CategoryEntity>,
    selectedCategoryId: String,
    onCategoryClick: () -> Unit
) {
    val selectedCategory = categories.find { it.id == selectedCategoryId }
    Column {
        Text(
            text = "Category",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = selectedCategory?.name ?: "Select a category",
            onValueChange = { },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCategoryClick() },
            placeholder = {
                Text(
                    text = "Select a category",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select category",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            },
            readOnly = true,
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = MaterialTheme.colorScheme.primary,
                disabledTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdownSection(
    accounts: List<com.example.vesta.data.local.entities.AccountEntity>,
    selectedAccountId: String,
    onAccountSelected: (String) -> Unit,
    showDropdown: Boolean,
    onDropdownClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val selectedAccount = accounts.find { it.id == selectedAccountId }
    Column {
        Text(
            text = "Account",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        ExposedDropdownMenuBox(
            expanded = showDropdown,
            onExpandedChange = { if (it) onDropdownClick() else onDismiss() },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedAccount?.name ?: "Select an account",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdown) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                placeholder = {
                    Text(
                        text = "Select an account",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                },
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
                expanded = showDropdown,
                onDismissRequest = onDismiss,
                modifier = Modifier
                    .exposedDropdownSize(true)
                    .heightIn(max = 300.dp)
            ) {
                if (accounts.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text(text = "No accounts found") },
                        onClick = onDismiss
                    )
                } else {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                onAccountSelected(account.id)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateSection(
    selectedDate: String,
    onDateChange: (String) -> Unit
) {
    Column {
        Text(
            text = "Date",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = selectedDate,
            onValueChange = onDateChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Calendar",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            },
            readOnly = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
private fun NoteSection(
    note: String,
    onNoteChange: (String) -> Unit
) {
    Column {
        Text(
            text = "Note (Optional)",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = {
                Text(
                    text = "Add a note...",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            maxLines = 4
        )
    }
}

@Composable
private fun ReceiptUploadSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable { /* Handle file upload */ },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = "Upload receipt",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Upload Receipt",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Choose File",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun CategoryBottomSheet(
    categories: List<CategoryEntity>,
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Text(
                text = "Select Category",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        items(categories) { category ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onCategorySelected(category.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (category.id == selectedCategoryId)
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun AddTransactionScreenPreview() {
//    VestaTheme {
//        AddTransactionScreen()
//    }
//}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddTransactionScreenDarkPreview() {
    VestaTheme {
        AddTransactionScreen()
    }
}
