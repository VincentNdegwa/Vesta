package com.example.vesta.ui.reports

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vesta.ui.auth.viewmodel.AuthViewModel
import com.example.vesta.ui.components.DateInput
import com.example.vesta.ui.reports.viewmodel.ReportsViewModel
import com.example.vesta.ui.theme.VestaTheme
import com.example.vesta.utils.ExportUtils
import java.text.SimpleDateFormat
import java.util.*

data class DatePreset(
    val label: String,
    val fromDate: String,
    val toDate: String
)

data class ReportType(
    val id: String,
    val title: String,
    val description: String
)

data class ExportFormat(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportReportsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
    viewModel: ReportsViewModel = hiltViewModel()
) {
    // Get user ID and states
    val authUiState = authViewModel.uiState.collectAsStateWithLifecycle()
    val reportsUiState = viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val userId = authUiState.value.userId
    
    // Date formatting
    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    val calendar = Calendar.getInstance()
    
    // Default dates
    val today = calendar.timeInMillis
    
    // Generate date presets dynamically based on current date
    calendar.add(Calendar.DAY_OF_YEAR, -7)
    val sevenDaysAgo = calendar.timeInMillis
    val sevenDaysAgoStr = dateFormat.format(sevenDaysAgo)
    
    calendar.timeInMillis = today
    calendar.add(Calendar.DAY_OF_YEAR, -30)
    val thirtyDaysAgo = calendar.timeInMillis
    val thirtyDaysAgoStr = dateFormat.format(thirtyDaysAgo)
    
    calendar.timeInMillis = today
    calendar.add(Calendar.DAY_OF_YEAR, -90)
    val ninetyDaysAgo = calendar.timeInMillis
    val ninetyDaysAgoStr = dateFormat.format(ninetyDaysAgo)
    
    calendar.timeInMillis = today
    calendar.set(Calendar.DAY_OF_YEAR, 1) // First day of year
    val yearStart = calendar.timeInMillis
    val yearStartStr = dateFormat.format(yearStart)
    
    calendar.timeInMillis = today
    calendar.set(Calendar.MONTH, 11) // December
    calendar.set(Calendar.DAY_OF_MONTH, 31) // Last day of year
    val yearEnd = calendar.timeInMillis
    val yearEndStr = dateFormat.format(yearEnd)
    
    val todayStr = dateFormat.format(today)
    
    // State for the form
    var fromDate by remember { mutableStateOf(thirtyDaysAgoStr) }
    var toDate by remember { mutableStateOf(todayStr) }
    var selectedReportType by remember { mutableStateOf("complete") }
    var selectedFormat by remember { mutableStateOf("pdf") }
    var fromDateMillis by remember { mutableStateOf(thirtyDaysAgo) }
    var toDateMillis by remember { mutableStateOf(today) }
    
    // Parse dates for export
    LaunchedEffect(fromDate, toDate) {
        try {
            fromDateMillis = dateFormat.parse(fromDate)?.time ?: thirtyDaysAgo
            toDateMillis = dateFormat.parse(toDate)?.time ?: today
        } catch (e: Exception) {
            // Keep using the default values if parsing fails
        }
    }
    
    // Show error messages
    LaunchedEffect(reportsUiState.value.error) {
        reportsUiState.value.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    
    // Create a state to track the current export format
    var currentExportFormat by remember { mutableStateOf<String?>(null) }
    
    // Handle export data when it becomes available
    LaunchedEffect(reportsUiState.value.exportData, currentExportFormat) {
        reportsUiState.value.exportData?.let { exportData ->
            if (!reportsUiState.value.isExporting && exportData != null && currentExportFormat != null) {
                val uri = ExportUtils.exportReportData(
                    context = context,
                    reportData = exportData,
                    format = currentExportFormat!!
                )
                
                // If export was successful, share the file
                if (uri != null) {
                    val shareIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        type = when (currentExportFormat) {
                            "pdf" -> "application/pdf"
                            "csv" -> "text/csv"
                            else -> "*/*"
                        }
                        flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Report"))
                    Toast.makeText(context, "Export successful!", Toast.LENGTH_SHORT).show()
                    onExportClick()
                } else {
                    Toast.makeText(context, "Failed to export report", Toast.LENGTH_SHORT).show()
                }
                
                // Reset the current export format and clear the export data
                currentExportFormat = null
                viewModel.exportComplete()
            }
        }
    }
    
    val datePresets = listOf(
        DatePreset("Last 7 days", sevenDaysAgoStr, todayStr),
        DatePreset("Last 30 days", thirtyDaysAgoStr, todayStr),
        DatePreset("Last 90 days", ninetyDaysAgoStr, todayStr),
        DatePreset("This year", yearStartStr, yearEndStr)
    )
    
    val reportTypes = listOf(
        ReportType("complete", "Complete Report", "All transactions and analytics"),
        ReportType("transactions", "Transactions Only", "Transaction history without charts"),
        ReportType("summary", "Summary Report", "Overview with key metrics"),
        ReportType("category", "Category Breakdown", "Spending by category analysis")
    )
    
    val exportFormats = listOf(
        ExportFormat("pdf", "PDF Report", "Formatted report with charts", Icons.Default.Description),
        ExportFormat("csv", "CSV Data", "Raw transaction data", Icons.Default.TableChart)
    )
    
    Scaffold(
        modifier = modifier,
        topBar = {
            ExportReportsTopBar(onBackClick = onBackClick)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (reportsUiState.value.isExporting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Preparing your report...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Export Header Section - Fixed at top
                ExportHeaderSection()
                
                // Scrollable Content
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Date Range Section
                    item {
                        DateRangeSection(
                            fromDate = fromDate,
                            toDate = toDate,
                            onFromDateChange = { fromDate = it },
                            onToDateChange = { toDate = it },
                            datePresets = datePresets,
                            onPresetSelected = { preset ->
                                fromDate = preset.fromDate
                                toDate = preset.toDate
                            }
                        )
                    }
                    
                    // Report Type Section
                    item {
                        ReportTypeSection(
                            reportTypes = reportTypes,
                            selectedType = selectedReportType,
                            onTypeSelected = { selectedReportType = it }
                        )
                    }
                    
                    // Export Format Section
                    item {
                        ExportFormatSection(
                            exportFormats = exportFormats,
                            selectedFormat = selectedFormat,
                            onFormatSelected = { selectedFormat = it }
                        )
                    }
                    
                    // Export Preview Section
                    item {
                        ExportPreviewSection(
                            fromDate = fromDate,
                            toDate = toDate,
                            reportType = reportTypes.find { it.id == selectedReportType }?.title ?: "",
                            format = exportFormats.find { it.id == selectedFormat }?.title ?: ""
                        )
                    }
                    
                    // Export Button and Security Note
                    item {
                        Column {
                            val currentContext = LocalContext.current
                            Button(
                                onClick = {
                                    // Prepare export data when button is clicked
                                    userId?.let {
                                        // Set current export format before triggering the export
                                        currentExportFormat = selectedFormat
                                        Toast.makeText(currentContext, "Preparing report data...", Toast.LENGTH_SHORT).show()
                                        viewModel.prepareExportData(
                                            userId = it,
                                            startDate = fromDateMillis,
                                            endDate = toDateMillis,
                                            reportType = selectedReportType
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = userId != null && !reportsUiState.value.isLoading
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Export Report",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "Your data is encrypted and secure. Reports are generated on-demand and not stored on our servers.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportReportsTopBar(
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Export Reports",
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
private fun ExportHeaderSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Export Your Data",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "Download your financial reports and transaction history",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DateRangeSection(
    fromDate: String,
    toDate: String,
    onFromDateChange: (String) -> Unit,
    onToDateChange: (String) -> Unit,
    datePresets: List<DatePreset>,
    onPresetSelected: (DatePreset) -> Unit
) {
    // Create a date formatter for this composable
    val dateFormat = remember { SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Date Range",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "From Date",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val fromDateMillis = try {
                        dateFormat.parse(fromDate)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }
                    val fromDateCalendar = Calendar.getInstance().apply { timeInMillis = fromDateMillis }
                    val fromDateContext = LocalContext.current
                    
                    DateInput(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                DatePickerDialog(
                                    fromDateContext,
                                    { _, year, month, day ->
                                        val newCalendar = Calendar.getInstance()
                                        newCalendar.set(year, month, day)
                                        onFromDateChange(dateFormat.format(newCalendar.time))
                                    },
                                    fromDateCalendar.get(Calendar.YEAR),
                                    fromDateCalendar.get(Calendar.MONTH),
                                    fromDateCalendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        value = fromDateMillis
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "To Date",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val toDateMillis = try {
                        dateFormat.parse(toDate)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }
                    val toDateCalendar = Calendar.getInstance().apply { timeInMillis = toDateMillis }
                    val toDateContext = LocalContext.current
                    
                    DateInput(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                DatePickerDialog(
                                    toDateContext,
                                    { _, year, month, day ->
                                        val newCalendar = Calendar.getInstance()
                                        newCalendar.set(year, month, day)
                                        onToDateChange(dateFormat.format(newCalendar.time))
                                    },
                                    toDateCalendar.get(Calendar.YEAR),
                                    toDateCalendar.get(Calendar.MONTH),
                                    toDateCalendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        value = toDateMillis
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(datePresets) { preset ->
                    PresetChip(
                        text = preset.label,
                        onClick = { onPresetSelected(preset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun ReportTypeSection(
    reportTypes: List<ReportType>,
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Report Type",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            reportTypes.forEach { reportType ->
                ReportTypeItem(
                    reportType = reportType,
                    isSelected = selectedType == reportType.id,
                    onSelected = { onTypeSelected(reportType.id) }
                )
                if (reportType != reportTypes.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ReportTypeItem(
    reportType: ReportType,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else Color.Transparent
            )
            .selectable(
                selected = isSelected,
                onClick = onSelected
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelected,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reportType.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = reportType.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ExportFormatSection(
    exportFormats: List<ExportFormat>,
    selectedFormat: String,
    onFormatSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Export Format",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            exportFormats.forEach { format ->
                ExportFormatItem(
                    format = format,
                    isSelected = selectedFormat == format.id,
                    onSelected = { onFormatSelected(format.id) }
                )
                if (format != exportFormats.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ExportFormatItem(
    format: ExportFormat,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else Color.Transparent
            )
            .selectable(
                selected = isSelected,
                onClick = onSelected
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelected,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Icon(
            imageVector = format.icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = format.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = format.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ExportPreviewSection(
    fromDate: String,
    toDate: String,
    reportType: String,
    format: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Export Preview",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            PreviewRow("Date Range:", "$fromDate - $toDate")
            PreviewRow("Format:", format)
            PreviewRow("Report Type:", reportType)
            PreviewRow("Estimated Size:", "~2.3 MB")
        }
    }
}

@Composable
private fun PreviewRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

//@Preview(showBackground = true)
//@Composable
//fun ExportReportsScreenPreview() {
//    VestaTheme {
//        ExportReportsScreen()
//    }
//}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ExportReportsScreenDarkPreview() {
    VestaTheme {
        ExportReportsScreen()
    }
}
