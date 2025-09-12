package com.example.vesta.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vesta.ui.auth.viewmodel.AuthViewModel
import com.example.vesta.ui.category.CategoryViewModel
import com.example.vesta.ui.transaction.viewmodel.TransactionViewModel
import com.example.vesta.ui.account.viewmodel.AccountViewModel
import com.example.vesta.data.local.entities.CategoryEntity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import com.example.vesta.ui.theme.VestaTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTransactionScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onSaveTransaction: () -> Unit = {},
    transactionViewModel: TransactionViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    accountViewModel: AccountViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    var amount by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }
    var selectedCategoryId by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(getCurrentDate()) }
    var note by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(accounts[0].id) }

    val categoryUiState by categoryViewModel.uiState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(authUiState.userId) {
        authUiState.userId?.let {
            categoryViewModel.loadCategories(it)
        }
    }

    val categories =
        if (isExpense) categoryUiState.expenseCategories else categoryUiState.incomeCategories

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
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
        },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            val isFormValid = amount.isNotBlank() && amount.toDoubleOrNull() != null && amount.toDouble() > 0.0 &&
                selectedAccountId.isNotBlank() && selectedCategoryId.isNotBlank() && selectedDate.isNotBlank()
            FloatingActionButton(
                onClick = ({
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
                }),
                containerColor = if (isFormValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Save Transaction",
                    tint = if (isFormValid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
                .padding(innerPadding)
            ,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                TransactionAmountSection(
                    amount = amount,
                    isExpense = isExpense,
                    onAmountChange = { amount = it },
                    onTypeChange = { isExpense = it }
                )
            }
            item {
                var showAccountSheet by remember { mutableStateOf(false) }
                val selectedAccount = accounts.find { it.id == selectedAccountId }

                Column {
                    OutlinedButton(
                        onClick = { showAccountSheet = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedAccount?.name ?: "Select Account",
                            color = if (selectedAccount != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (selectedAccount != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (showAccountSheet) {
                        ModalBottomSheet(
                            onDismissRequest = { showAccountSheet = false },
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Select Account", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(12.dp))
                                accounts.forEach { account ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                selectedAccountId = account.id
                                                showAccountSheet = false
                                            },
                                        color = if (selectedAccountId == account.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                                        shadowElevation = if (selectedAccountId == account.id) 2.dp else 0.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalanceWallet,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(account.name)
                                            Spacer(modifier = Modifier.weight(1f))
                                            if (selectedAccountId == account.id) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            item {
                var showBottomSheet by remember { mutableStateOf(false) }
                val visibleCategories = categories.take(6)
                val hiddenCount = categories.size - visibleCategories.size

                Column {
                    Box(modifier = Modifier.height(250.dp)) {
                        CategoryGrid(
                            categories = visibleCategories,
                            selectedCategoryId = selectedCategoryId,
                            onCategorySelected = { selectedCategoryId = it }
                        )
                    }
                    if (hiddenCount > 0) {
                        OutlinedButton(
                            onClick = { showBottomSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = ButtonDefaults.outlinedButtonBorder
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("More Categories (${hiddenCount}+)" )
                        }
                    }
                    if (showBottomSheet) {
                        ModalBottomSheet(
                            onDismissRequest = { showBottomSheet = false },
                            containerColor = MaterialTheme.colorScheme.surface,
                            sheetState = rememberModalBottomSheetState(
                                skipPartiallyExpanded = true
                            ),
                            windowInsets = WindowInsets(0),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxHeight(0.7f).padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ){
                                Text(
                                    text= "Categories",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(categories) { category ->
                                        CategoryItem(
                                            category = category,
                                            isSelected = category.id == selectedCategoryId,
                                            onClick = {
                                                selectedCategoryId = category.id
                                                showBottomSheet = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Column (
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                    DateSelector(
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it }
                    )
                    NoteInput(
                        note = note,
                        onNoteChange = { note = it }
                    )
                }
            }
//            item {
//                ReceiptUploadButton()
//            }
        }
    }
}

@Composable
fun TransactionAmountSection(
    amount: String,
    isExpense: Boolean,
    onAmountChange: (String) -> Unit,
    onTypeChange: (Boolean) -> Unit
) {

    Card(
        modifier = Modifier.padding(top = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
        ) {
            // Transaction Type Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, top = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50)),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TransactionTypeButton(
                        text = "Expense",
                        selected = isExpense,
                        onClick = { onTypeChange(true) },
                        isExpense = true
                    )
                    TransactionTypeButton(
                        text = "Income",
                        selected = !isExpense,
                        onClick = { onTypeChange(false) },
                        isExpense = false
                    )
                }
            }

            // Amount Input
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth(0.7f)) {
                    BasicTextField(
                        value = amount,
                        onValueChange = { newAmount ->
                            if (newAmount.isEmpty() || newAmount.matches(Regex("^\\d*\\.?\\d*$"))) {
                                onAmountChange(newAmount)
                            }
                        },
                        textStyle = MaterialTheme.typography.displayLarge.copy(
                            color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        cursorBrush = SolidColor(if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    Divider(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(2.dp),
                        color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isExpense) "Expense Amount" else "Income Amount",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Tap to edit amount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun TransactionTypeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    isExpense: Boolean
) {
    val selectedColor = when {
        selected && isExpense -> MaterialTheme.colorScheme.error
        selected && !isExpense -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    val textColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier.padding(5.dp).clickable(onClick = onClick),
        color = selectedColor,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = textColor
        )
    }
}

@Composable
fun CategoryGrid(
    categories: List<CategoryEntity>,
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categories) { category ->
            CategoryItem(
                category = category,
                isSelected = category.id == selectedCategoryId,
                onClick = { onCategorySelected(category.id) }
            )
        }
    }
}

@Composable
fun CategoryItem(
    category: CategoryEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            ),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            if (isSelected) {
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelector(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()) }
    
    val selectedDateMillis = remember(selectedDate) {
        try {
            dateFormatter.parse(selectedDate)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .fillMaxWidth()
            .clickable { showDatePicker = true },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = selectedDate)
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
                        onDateSelected(dateFormatter.format(calendar.time))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun NoteInput(
    note: String,
    onNoteChange: (String) -> Unit
) {
    var showNoteDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .fillMaxWidth()
            .clickable { showNoteDialog = true },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Notes,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (note.isEmpty()) "Add Note" else note,
                color = if (note.isEmpty()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                       else MaterialTheme.colorScheme.onSurface
            )
        }
    }

    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Add Note") },
            text = {
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note") },
                    maxLines = 5
                )
            },
            confirmButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    onNoteChange("")
                    showNoteDialog = false 
                }) {
                    Text("Clear")
                }
            }
        )
    }
}

@Composable
fun ReceiptUploadButton() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { /* Handle receipt upload */ },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Add Receipt")
        }
    }
}

fun getCurrentDate(): String {
    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    return formatter.format(Date())
}
