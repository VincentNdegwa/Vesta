package com.example.vesta.ui.bills

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vesta.data.local.entities.RecurrenceType
import com.example.vesta.ui.auth.viewmodel.AuthViewModel
import com.example.vesta.ui.bills.viewmodel.BillViewModel
import com.example.vesta.ui.category.CategoryViewModel
import com.example.vesta.ui.theme.VestaTheme
import java.text.SimpleDateFormat
import java.util.*

data class Bill(
    val id: String,
    val name: String,
    val categoryId: String,
    val categoryName: String,
    val amount: Double,
    val dueDate: Date,
    val isRecurring: Boolean,
    val status: BillStatus,
    val icon: ImageVector,
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val intervalCount: Int = 1,
    val timesPerPeriod: Int? = null
)

enum class BillStatus(val displayName: String, val color: Color) {
    UPCOMING("Upcoming", Color(0xFFFFA726)),
    OVERDUE("Overdue", Color(0xFFE57373)),
    PAID("Paid", Color(0xFF66BB6A))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onAddBillClick: () -> Unit = {},
    onBillClick: (Bill) -> Unit = {},
    onEditBillClick: (String) -> Unit = {}, // New parameter to handle edit navigation
    viewModel: BillViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    var authUiState = authViewModel.uiState.collectAsStateWithLifecycle()
    var billsUiState = viewModel.uiState.collectAsStateWithLifecycle()
    val categoryUiState = categoryViewModel.uiState.collectAsStateWithLifecycle()
    var userId = authUiState.value.userId
    LaunchedEffect(userId) {
        if (userId != null) {
            viewModel.loadBillReminders(userId)
            categoryViewModel.loadCategories(userId)
        }
    }

    val upcomingBills = billsUiState.value.billReminders.map { reminder ->

        val categoryName = categoryUiState.value.expenseCategories.find { it.id == reminder.categoryId }?.name ?: "Other"
        val billIcon = when (categoryName.lowercase()) {
            "credit card", "bills & utilities" -> Icons.Default.CreditCard
            "utilities" -> Icons.Default.ElectricBolt
            "internet", "wifi" -> Icons.Default.Wifi
            "insurance", "healthcare" -> Icons.Default.Security
            "rent", "mortgage", "home" -> Icons.Default.Home
            "subscription", "entertainment" -> Icons.Default.LocalOffer
            else -> Icons.Default.NotificationsActive
        }
        
        // Calculate status based on isPaid and dates
        val status = when {
            reminder.isPaid -> BillStatus.PAID
            reminder.nextDueDate != null && reminder.nextDueDate < System.currentTimeMillis() -> BillStatus.OVERDUE
            else -> BillStatus.UPCOMING
        }
        
        Bill(
            id = reminder.id,
            name = reminder.title,
            categoryId = reminder.categoryId,
            categoryName = categoryName,
            amount = reminder.amount,
            dueDate = Date(reminder.nextDueDate ?: reminder.dueDate),
            isRecurring = reminder.recurrenceType != RecurrenceType.NONE,
            status = status,
            icon = billIcon,
            recurrenceType = reminder.recurrenceType,
            intervalCount = reminder.intervalCount,
            timesPerPeriod = reminder.timesPerPeriod
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            BillsTopBar(
                onBackClick = onBackClick,
                onAddClick = onAddBillClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddBillClick,
                modifier = Modifier.size(56.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Bill",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Upcoming Bills Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upcoming Bills",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${upcomingBills.size} due",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            
            if (upcomingBills.isEmpty()) {
                item {
                    EmptyBillsState(
                        onAddClick = onAddBillClick,
                        modifier = Modifier.fillParentMaxSize()
                    )
                }
            } else {
                items(upcomingBills) { bill ->
                    BillItem(
                        bill = bill,
                        onClick = { onBillClick(bill) },
                        onEdit = {
                            // Navigate to edit screen with the bill ID
                            onEditBillClick(bill.id)
                        },
                        onDelete = {
                            // Delete the bill
                            userId?.let { viewModel.deleteBillReminder(bill.id, it) }
                        },
                        onDisable = {
                            // Disable recurrence for the bill
                            userId?.let { viewModel.disableBillReminder(bill.id, it) }
                        },
                        onMarkPaid = {
                            // Mark the bill as paid
                            userId?.let { viewModel.markBillAsPaid(bill.id, it) }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillsTopBar(
    onBackClick: () -> Unit,
    onAddClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Bill Reminders",
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BillItem(
    bill: Bill,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onDisable: () -> Unit = {},
    onMarkPaid: () -> Unit = {}
) {
    val dateFormatter = remember { SimpleDateFormat("M/d/yyyy", Locale.getDefault()) }
    var showDropdownMenu by remember { mutableStateOf(false) }

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Bill") },
            text = { Text("Are you sure you want to delete this bill? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { showDropdownMenu = true }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bill icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = bill.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Bill details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = bill.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = bill.categoryName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Status badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = bill.status.color.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = bill.status.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = bill.status.color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Amount and due date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$${String.format("%.2f", bill.amount)}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Due ${dateFormatter.format(bill.dueDate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        
                        if (bill.isRecurring) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                
                                // Show the recurrence type if available
                                val recurrenceText = when (bill.recurrenceType) {
                                    RecurrenceType.DAILY -> "Daily"
                                    RecurrenceType.WEEKLY -> "Weekly"
                                    RecurrenceType.MONTHLY -> "Monthly"
                                    RecurrenceType.YEARLY -> "Yearly"
                                    else -> "Recurring"
                                }
                                
                                Text(
                                    text = recurrenceText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            
                            // Show interval count if greater than 1
                            if (bill.intervalCount > 1) {
                                Text(
                                    text = "Every ${bill.intervalCount} ${
                                        when (bill.recurrenceType) {
                                            RecurrenceType.DAILY -> if (bill.intervalCount > 1) "days" else "day"
                                            RecurrenceType.WEEKLY -> if (bill.intervalCount > 1) "weeks" else "week"
                                            RecurrenceType.MONTHLY -> if (bill.intervalCount > 1) "months" else "month"
                                            RecurrenceType.YEARLY -> if (bill.intervalCount > 1) "years" else "year"
                                            else -> ""
                                        }
                                    }",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            
                            // Show times per period if set
                            bill.timesPerPeriod?.let { count ->
                                if (count > 1) {
                                    Text(
                                        text = "$count times per ${
                                            when (bill.recurrenceType) {
                                                RecurrenceType.DAILY -> "day"
                                                RecurrenceType.WEEKLY -> "week"
                                                RecurrenceType.MONTHLY -> "month"
                                                else -> "period"
                                            }
                                        }",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Mark as paid button
            if (bill.status != BillStatus.PAID) {
                Button(
                    onClick = { onMarkPaid() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Mark Paid",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        
        DropdownMenu(
            expanded = showDropdownMenu,
            onDismissRequest = { showDropdownMenu = false },
            modifier = Modifier.width(200.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Edit Bill") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit"
                    )
                },
                onClick = {
                    onEdit()
                    showDropdownMenu = false
                }
            )
            
            if (bill.isRecurring) {
                DropdownMenuItem(
                    text = { Text("Disable Recurrence") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = "Disable Recurrence"
                        )
                    },
                    onClick = {
                        onDisable()
                        showDropdownMenu = false
                    }
                )
            }
            
            if (bill.status != BillStatus.PAID) {
                DropdownMenuItem(
                    text = { Text("Mark as Paid") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Mark as Paid"
                        )
                    },
                    onClick = {
                        onMarkPaid()
                        showDropdownMenu = false
                    }
                )
            }
            
            Divider()
            
            DropdownMenuItem(
                text = { 
                    Text(
                        "Delete Bill", 
                        color = MaterialTheme.colorScheme.error
                    ) 
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    showDeleteConfirmation = true
                    showDropdownMenu = false
                }
            )
        }
    }
}

@Composable
private fun EmptyBillsState(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsNone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(60.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Text
        Text(
            text = "No Bill Reminders",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Add your first bill reminder to keep track of upcoming payments",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Add Button
        Button(
            onClick = onAddClick,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "Add Bill Reminder",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun BillsScreenPreview() {
//    VestaTheme {
//        BillsScreen()
//    }
//}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BillsScreenDarkPreview() {
    VestaTheme {
        BillsScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyBillsStatePreview() {
    VestaTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            EmptyBillsState(onAddClick = {})
        }
    }
}
