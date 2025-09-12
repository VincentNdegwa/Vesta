package com.example.vesta.ui.dashboard

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.sharp.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vesta.data.local.dao.TransactionDao
import com.example.vesta.ui.account.viewmodel.AccountViewModel
import com.example.vesta.ui.auth.viewmodel.AuthViewModel
import com.example.vesta.ui.category.CategoryViewModel
import com.example.vesta.ui.category.CategoryUiState
import com.example.vesta.ui.components.Logo
import com.example.vesta.ui.theme.VestaTheme
import com.example.vesta.ui.transaction.viewmodel.TransactionUiState
import com.example.vesta.ui.transaction.viewmodel.TransactionViewModel
import kotlin.math.roundToInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle. compose. LocalLifecycleOwner
import com.example.vesta.ui.security.viewmodel.SecurityViewModel
import com.example.vesta.ui.components.HideableText
import java.util.*
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onAddTransactionClick: () -> Unit = {},
    onSetBudgetClick: () -> Unit = {},
    onTransactionsClick: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
    transactionViewModel: TransactionViewModel = hiltViewModel(),
    accountViewModel: AccountViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    securityViewModel: SecurityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userId = uiState.userId
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val transactionUiState by transactionViewModel.uiState.collectAsStateWithLifecycle()
    val categoryUiState by categoryViewModel.uiState.collectAsState()
    val securityUiState by securityViewModel.uiState.collectAsState()

        fun getData(userId: String){
            transactionViewModel.getStats(userId)
            transactionViewModel.loadExpenseByCategoryForCurrentMonth(userId)
            transactionViewModel.loadIncomeByCategoryForCurrentMonth(userId)
            accountViewModel.loadAccounts(userId)
            categoryViewModel.loadCategories(userId)
        }

    LaunchedEffect(userId) {
        userId?.let {
            getData(it)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, userId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && userId != null) {
                getData(userId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    Scaffold(
        modifier = modifier,
        topBar = {
            DashboardTopBar(
                userDisplayName = uiState.userDisplayName ?: "User",
                userEmail = uiState.userEmail ?: ""
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                // Financial Overview Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FinancialOverviewCard(
                        title = "Total Income",
                        amount = "${transactionUiState.totalIncome}",
                        change = "${transactionUiState.incomeChange}% this month",
                        isPositive = transactionUiState.incomeChange > 0,
                        modifier = Modifier.weight(1f),
                        hideAmounts = securityUiState.hideAmounts,
                        currency = securityUiState.currency
                    )
                    FinancialOverviewCard(
                        title = "Total Expenses",
                        amount = "${transactionUiState.totalExpense}",
                        change = "${transactionUiState.expenseChange}% this month",
                        isPositive = transactionUiState.expenseChange > 0,
                        modifier = Modifier.weight(1f),
                        hideAmounts = securityUiState.hideAmounts,
                        currency = securityUiState.currency
                    )
                }
            }

            item {
                val configuration = LocalConfiguration.current
                val screenWidth = configuration.screenWidthDp.dp
                val horizontalPadding = 16.dp * 2 
                val cardSpacing = 12.dp
                val cardWidth = screenWidth - horizontalPadding
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(cardSpacing)
                ) {
                    items(accounts) { account ->
                        AvailableBalanceCard(
                            name = account.name,
                            type = account.type,
                            balance = account.balance,
                            currency = account.currency,
                            description = account.description,
                            lastUpdated = account.updatedAt,
                            modifier = Modifier.width(cardWidth),
                            hideAmounts = securityUiState.hideAmounts,
                            currencySymbol= securityUiState.currency
                        )
                    }
                }
            }

            item {
                // Quick Actions
                QuickActionsSection(
                    onAddTransactionClick = onAddTransactionClick,
                    onSetBudgetClick = onSetBudgetClick,
                    onTransactionsClick= onTransactionsClick
                )
            }


            item {
                // Income by Category
                IncomeCategoriesSection(
                    transactionUiState = transactionUiState,
                    categoryUiState = categoryUiState,
                    currency = securityUiState.currency
                )
            }

            item {
                // Spending Categories
                SpendingCategoriesSection(
                    transactionUiState = transactionUiState,
                    categoryUiState = categoryUiState,
                    currency = securityUiState.currency
                )
            }

            item {
                // Notifications
//                NotificationsSection()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    userDisplayName: String,
    userEmail: String
) {
    // Extract first name from display name or use "User" as fallback
    val firstName = remember(userDisplayName) {
        when {
            userDisplayName.isNotBlank() -> {
                userDisplayName.split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: userDisplayName
            }
            userEmail.isNotBlank() -> {
                userEmail.substringBefore("@").replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase() else it.toString() 
                }
            }
            else -> "User"
        }
    }
    
    // Get time-based greeting
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    
    TopAppBar(
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Logo(size = 32)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Finvesta",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = "$greeting, $firstName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        actions = {
            IconButton(onClick = { /* Handle notifications */ }) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            IconButton(onClick = { /* Handle profile */ }) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
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
private fun FinancialOverviewCard(
    title: String,
    amount: String,
    change: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier,
    hideAmounts: Boolean = false,
    currency: String = "$"
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isPositive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPositive) "↗" else "↘",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isPositive) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "$title ($currency)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            val numericBalance = amount.toDouble().let {
                val bd = BigDecimal.valueOf(it).setScale(2, RoundingMode.DOWN)
                "%,.2f".format(bd.toDouble())
            } ?: "-"

            HideableText(
                text = numericBalance,
                hideAmounts = hideAmounts,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = change,
                style = MaterialTheme.typography.bodySmall,
                color = if (isPositive) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun AvailableBalanceCard(
    name: String?,
    type: String?,
    balance: Double?,
    currency: String?,
    description: String?,
    lastUpdated: Long?,
    modifier: Modifier = Modifier,
    hideAmounts: Boolean = false,
    currencySymbol: String = "$"
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiary
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "${name?.takeIf { it.isNotBlank() } ?: "Account"} (${currencySymbol})",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onTertiary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = type?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: "-",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            val numericBalance = balance?.let {
                val bd = BigDecimal.valueOf(it).setScale(2, RoundingMode.DOWN)
                "%,.2f".format(bd.toDouble())
            } ?: "-"

            HideableText(
                text = numericBalance,
                hideAmounts = hideAmounts,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp
                ),
                color = MaterialTheme.colorScheme.onTertiary
            )

            if (!description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = lastUpdated?.let {
                    val date = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it))
                    "Last updated $date"
                } ?: "Last updated -",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun QuickActionsSection(
    onAddTransactionClick: () -> Unit,
    onSetBudgetClick: () -> Unit,
    onTransactionsClick: ()-> Unit
) {
    Column {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onAddTransactionClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Transaction",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            OutlinedButton(
                onClick = onSetBudgetClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Text(
                    text = "$",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Set Budget",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onTransactionsClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

    }
}

@Composable
private fun SpendingCategoriesSection(
    transactionUiState: TransactionUiState,
    categoryUiState: CategoryUiState,
    currency: String = "$"
) {
    val expenseByCategory = transactionUiState.expenseByCategory
    val categories = categoryUiState.categories

    val themeColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.outline,
        MaterialTheme.colorScheme.surfaceVariant
    )
    val colorMap = remember(expenseByCategory) {
        val shuffled = themeColors.shuffled(java.util.Random(0xBADA55))
        expenseByCategory.mapIndexed { idx, catSum ->
            catSum.categoryId to shuffled[idx % shuffled.size]
        }.toMap()
    }
    val chartData = expenseByCategory.mapNotNull { catSum: TransactionDao.CategoryExpenseSum ->
        val cat = categories.find { it.id == catSum.categoryId }
        if (cat != null) Triple(cat.name, catSum.total, colorMap[catSum.categoryId] ?: MaterialTheme.colorScheme.primary) else null
    }
    Column {
        Text(
            text = "Spending Categories",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "This month's breakdown",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Pie chart
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    if (chartData.isNotEmpty()) {
                        PieChart(data = chartData.map { it.second }, colors = chartData.map { it.third })
                    } else {
                        Text(
                            text = "No data",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Legend
                chartData.forEach { (category, amount, color) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color = color, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = currency,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(1.dp))
                            Text(
                                text = "${amount.roundToInt()}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomeCategoriesSection(
    transactionUiState: TransactionUiState,
    categoryUiState: CategoryUiState,
    currency: String = "$"
) {
    val incomeByCategory = transactionUiState.incomeByCategory
    val categories = categoryUiState.categories
    val themeColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.outline,
        MaterialTheme.colorScheme.surfaceVariant
    )
    val colorMap = remember(incomeByCategory) {
        val shuffled = themeColors.shuffled(java.util.Random(0xBADA55))
        incomeByCategory.mapIndexed { idx, catSum ->
            catSum.categoryId to shuffled[idx % shuffled.size]
        }.toMap()
    }
    val chartData = incomeByCategory.mapNotNull { catSum: TransactionDao.CategoryExpenseSum ->
        val cat = categories.find { it.id == catSum.categoryId }
        if (cat != null) Triple(cat.name, catSum.total, colorMap[catSum.categoryId] ?: MaterialTheme.colorScheme.primary) else null
    }
    Column {
        Text(
            text = "Income Categories",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "This month's income breakdown",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Pie chart
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    if (chartData.isNotEmpty()) {
                        PieChart(data = chartData.map { it.second }, colors = chartData.map { it.third })
                    } else {
                        Text(
                            text = "No data",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Legend
                chartData.forEach { (category, amount, color) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color = color, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = currency,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(1.dp))
                            Text(
                                text = "${amount.roundToInt()}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PieChart(data: List<Double>, colors: List<Color>, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(200.dp)) {
        val total = data.sum()
        var startAngle = -90f
        data.forEachIndexed { i, value ->
            val sweep = if (total > 0) (value / total * 360f).toFloat() else 0f
            drawArc(
                color = colors.getOrElse(i) { Color.Gray },
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun NotificationsSection() {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "2 new",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Credit Card Payment Due
        NotificationCard(
            title = "Credit Card Payment Due",
            description = "Chase Sapphire - $1,240.50",
            timeInfo = "Due Tomorrow",
            dueDate = "Due Feb 15, 2025",
            backgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
            iconColor = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Budget Alert
        NotificationCard(
            title = "Budget Alert",
            description = "Shopping budget: $1,400 of $1,650",
            timeInfo = "85% Used",
            backgroundColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
            iconColor = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun NotificationCard(
    title: String,
    description: String,
    timeInfo: String,
    dueDate: String? = null,
    backgroundColor: Color,
    iconColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color = iconColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (dueDate != null) Icons.Sharp.List
                                 else Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                dueDate?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Text(
                text = timeInfo,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = iconColor
            )
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun DashboardScreenPreview() {
//    VestaTheme {
//        DashboardScreen()
//    }
//}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DashboardScreenDarkPreview() {
    VestaTheme {
        DashboardScreen()
    }
}
