package com.example.vesta.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.vesta.data.repository.ReportExportData
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.BufferedWriter
import java.io.FileWriter

/**
 * Utility class for exporting reports
 */
object ExportUtils {

    /**
     * Export report data to a file
     */
    fun exportReportData(
        context: Context, 
        reportData: ReportExportData,
        format: String
    ): Uri? {
        return when (format.lowercase()) {
            "csv" -> exportToCSV(context, reportData)
            "pdf" -> exportToPDF(context, reportData)
            else -> exportToCSV(context, reportData) // Default to CSV
        }
    }
    
    /**
     * Export data to CSV format using proper CSV formatting
     */
    private fun exportToCSV(context: Context, reportData: ReportExportData): Uri? {
        val dateFormat = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault())
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "vesta_report_${timeStamp}.csv"
        val moneyFormat = DecimalFormat("0.00")

        try {
            val reportFile = File(context.cacheDir, fileName)
            fun escapeCSV(value: String): String {
                val needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n")
                val escaped = value.replace("\"", "\"\"") // double quotes inside field
                return if (needsQuotes) "\"$escaped\"" else escaped
            }

            // Build the entire CSV as a list of rows
            val csvData = mutableListOf<List<String>>()

            fun metaRow(key: String, value: String): List<String> {
                return listOf(key, "", "", value, "")
            }

            // Add report metadata header for all report types
            csvData.add(metaRow("ReportType:", reportData.reportType))
            csvData.add(metaRow("Period:", "${dateFormat.format(Date(reportData.startDate))} to ${dateFormat.format(Date(reportData.endDate))}"))
            
            // Conditionally add different sections based on report type
            when (reportData.reportType) {
                "transactions" -> {
                    // Only include transaction data
                    csvData.add(listOf("")) // empty row
                    csvData.add(listOf("Date", "Category", "Description", "Amount", "Type"))
                    reportData.transactions.forEach { transaction ->
                        val category = reportData.categories[transaction.categoryId]?.name ?: "Unknown"
                        csvData.add(listOf(
                            dateFormat.format(Date(transaction.date)),
                            category,
                            transaction.description ?: "",
                            moneyFormat.format(transaction.amount),
                            transaction.type
                        ))
                    }
                }
                
                "summary" -> {
                    // Only include summary data
                    csvData.add(metaRow("TotalIncome:", moneyFormat.format(reportData.incomeTotal)))
                    csvData.add(metaRow("TotalExpenses:", moneyFormat.format(reportData.expenseTotal)))
                    csvData.add(metaRow("Net:", moneyFormat.format(reportData.incomeTotal - reportData.expenseTotal)))
                    
                    // Calculate additional summary metrics
                    val savingsRate = if (reportData.incomeTotal > 0) 
                        (reportData.incomeTotal - reportData.expenseTotal) / reportData.incomeTotal * 100 
                    else 0.0
                    csvData.add(metaRow("SavingsRate:", String.format("%.1f%%", savingsRate)))
                    
                    // Days in period
                    val daysInPeriod = ((reportData.endDate - reportData.startDate) / (1000 * 60 * 60 * 24)).toInt() + 1
                    csvData.add(metaRow("DaysInPeriod:", daysInPeriod.toString()))
                    
                    // Average daily spending
                    val dailyAvg = reportData.expenseTotal / daysInPeriod
                    csvData.add(metaRow("AvgDailySpending:", moneyFormat.format(dailyAvg)))
                    
                    // Transaction counts
                    val incomeCount = reportData.transactions.count { it.type.equals("income", ignoreCase = true) }
                    val expenseCount = reportData.transactions.count { it.type.equals("expense", ignoreCase = true) }
                    csvData.add(metaRow("IncomeTransactions:", incomeCount.toString()))
                    csvData.add(metaRow("ExpenseTransactions:", expenseCount.toString()))
                }
                
                "category" -> {
                    // Only include category breakdown
                    csvData.add(listOf("")) // empty row
                    csvData.add(listOf("Category Breakdown"))
                    csvData.add(listOf("Category", "Amount", "Percentage of Total"))
                    
                    // Calculate total expense for percentage
                    val totalExpense = reportData.expenseTotal
                    
                    // Add expense categories
                    csvData.add(listOf("Expense Categories:"))
                    reportData.categoryBreakdown.forEach { category ->
                        val percentage = if (totalExpense > 0) category.amount / totalExpense * 100 else 0.0
                        csvData.add(listOf(
                            category.categoryName, 
                            moneyFormat.format(category.amount),
                            String.format("%.1f%%", percentage)
                        ))
                    }
                    
                    // Add income categories
                    val incomeCategories = mutableMapOf<String, Double>()
                    reportData.transactions
                        .filter { it.type.equals("income", ignoreCase = true) }
                        .forEach { transaction ->
                            val categoryId = transaction.categoryId
                            incomeCategories[categoryId] = (incomeCategories[categoryId] ?: 0.0) + transaction.amount
                        }
                    
                    if (incomeCategories.isNotEmpty()) {
                        csvData.add(listOf("")) // empty row
                        csvData.add(listOf("Income Categories:"))
                        csvData.add(listOf("Category", "Amount", "Percentage of Total"))
                        
                        incomeCategories.forEach { (categoryId, amount) ->
                            val categoryName = reportData.categories[categoryId]?.name ?: "Unknown"
                            val percentage = if (reportData.incomeTotal > 0) amount / reportData.incomeTotal * 100 else 0.0
                            csvData.add(listOf(
                                categoryName,
                                moneyFormat.format(amount),
                                String.format("%.1f%%", percentage)
                            ))
                        }
                    }
                }
                
                else -> {
                    // "complete" or any other type - include everything
                    csvData.add(metaRow("TotalIncome:", moneyFormat.format(reportData.incomeTotal)))
                    csvData.add(metaRow("TotalExpenses:", moneyFormat.format(reportData.expenseTotal)))
                    csvData.add(metaRow("Net:", moneyFormat.format(reportData.incomeTotal - reportData.expenseTotal)))

                    csvData.add(listOf("")) // empty row

                    // Category breakdown section
                    csvData.add(listOf("Category Breakdown"))
                    csvData.add(listOf("Category", "Amount"))
                    reportData.categoryBreakdown.forEach { category ->
                        csvData.add(listOf(category.categoryName, moneyFormat.format(category.amount)))
                    }

                    csvData.add(listOf("")) // empty row

                    // Transaction data
                    csvData.add(listOf("Date", "Category", "Description", "Amount", "Type"))
                    reportData.transactions.forEach { transaction ->
                        val category = reportData.categories[transaction.categoryId]?.name ?: "Unknown"
                        csvData.add(listOf(
                            dateFormat.format(Date(transaction.date)),
                            category,
                            transaction.description ?: "",
                            moneyFormat.format(transaction.amount),
                            transaction.type
                        ))
                    }
                }
            }


            // Compute max column width per column
            val colWidths = mutableMapOf<Int, Int>()
            csvData.forEach { row ->
                row.forEachIndexed { index, cell ->
                    val len = cell.length
                    colWidths[index] = maxOf(colWidths[index] ?: 0, len)
                }
            }

            // When writing rows, pad to max width
            FileWriter(reportFile).use { writer ->
                csvData.forEachIndexed { index, row ->
                    val paddedRow = row.mapIndexed { i, cell -> cell.padEnd(colWidths[i] ?: cell.length, ' ') }
                    val line = paddedRow.joinToString(",") { escapeCSV(it) }
                    writer.append(line)
                    if (index < csvData.size - 1) writer.append("\n")
                }
            }

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                reportFile
            )

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    
    /**
     * Export data to PDF format using iText 7 library
     */
    private fun exportToPDF(context: Context, reportData: ReportExportData): Uri? {
        val dateFormat = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault())
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "vesta_report_${timeStamp}.pdf"
        val moneyFormat = DecimalFormat("0.00")
        
        try {
            // Create file in app's cache directory
            val reportFile = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(reportFile)
            
            // Create PDF document with iText
            val pdfWriter = PdfWriter(outputStream)
            val pdf = PdfDocument(pdfWriter)
            val document = Document(pdf)
            
            // Set up styles
            val primaryColor = DeviceRgb(33, 150, 243) // Material Blue
            val headerBorder = SolidBorder(primaryColor, 1f)
            
            // Add title based on report type
            val reportTitle = when (reportData.reportType) {
                "transactions" -> "Vesta Transaction Report"
                "summary" -> "Vesta Financial Summary"
                "category" -> "Vesta Category Analysis"
                else -> "Vesta Financial Report"
            }
            
            val title = Paragraph(reportTitle)
                .setFontSize(24f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20f)
            document.add(title)
            
            // Add metadata
            document.add(Paragraph("Report Type: ${getReportTypeTitle(reportData.reportType)}")
                .setFontSize(12f)
                .setMarginBottom(5f))
                
            document.add(Paragraph("Period: ${dateFormat.format(Date(reportData.startDate))} to ${dateFormat.format(Date(reportData.endDate))}")
                .setFontSize(12f)
                .setMarginBottom(10f))
            
            // Based on report type, show different content
            when (reportData.reportType) {
                "transactions" -> {
                    // Only show transactions table
                    addTransactionsSection(document, reportData, dateFormat, moneyFormat, primaryColor, headerBorder)
                }
                
                "summary" -> {
                    // Show expanded summary information
                    addExpandedSummarySection(document, reportData, moneyFormat, primaryColor)
                }
                
                "category" -> {
                    // Show detailed category breakdown
                    addCategoryBreakdownSection(document, reportData, moneyFormat, primaryColor, headerBorder, detailed = true)
                }
                
                else -> {
                    // "complete" - show everything
                    addSummarySection(document, reportData, moneyFormat)
                    addCategoryBreakdownSection(document, reportData, moneyFormat, primaryColor, headerBorder)
                    addTransactionsSection(document, reportData, dateFormat, moneyFormat, primaryColor, headerBorder)
                }
            }
            
            // Add footer
            document.add(Paragraph("Generated on ${dateFormat.format(Date())}")
                .setFontSize(10f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20f))
            
            document.close()
            
            // Return content URI using FileProvider
            return FileProvider.getUriForFile(
                context, 
                "${context.packageName}.provider",
                reportFile
            )
            
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    private fun getReportTypeTitle(reportType: String): String {
        return when (reportType) {
            "transactions" -> "Transactions Only"
            "summary" -> "Summary Report"
            "category" -> "Category Breakdown"
            else -> "Complete Report"
        }
    }
    
    private fun addSummarySection(document: Document, reportData: ReportExportData, moneyFormat: DecimalFormat) {
        // Add summary section
        document.add(Paragraph("Summary")
            .setFontSize(16f)
            .setBold()
            .setMarginBottom(10f))
        
        // Create summary table
        val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(15f)
        
        summaryTable.addCell(Cell().add(Paragraph("Total Income:")).setBorder(Border.NO_BORDER))
        summaryTable.addCell(Cell().add(Paragraph("$${moneyFormat.format(reportData.incomeTotal)}")
            .setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER))
            
        summaryTable.addCell(Cell().add(Paragraph("Total Expenses:")).setBorder(Border.NO_BORDER))
        summaryTable.addCell(Cell().add(Paragraph("$${moneyFormat.format(reportData.expenseTotal)}")
            .setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER))
            
        summaryTable.addCell(Cell().add(Paragraph("Net:").setBold()).setBorder(Border.NO_BORDER))
        val netAmount = reportData.incomeTotal - reportData.expenseTotal
        val netColor = if (netAmount >= 0) ColorConstants.DARK_GRAY else ColorConstants.RED
        summaryTable.addCell(Cell().add(Paragraph("$${moneyFormat.format(netAmount)}")
            .setTextAlignment(TextAlignment.RIGHT)
            .setFontColor(netColor)
            .setBold()).setBorder(Border.NO_BORDER))
            
        document.add(summaryTable)
    }
    
    private fun addExpandedSummarySection(document: Document, reportData: ReportExportData, moneyFormat: DecimalFormat, primaryColor: DeviceRgb) {
        // Add summary section with more detailed metrics
        document.add(Paragraph("Financial Summary")
            .setFontSize(16f)
            .setBold()
            .setMarginBottom(10f))
        
        // Create summary table
        val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(15f)
        
        // Basic metrics
        summaryTable.addCell(Cell().add(Paragraph("Total Income:")).setBorder(Border.NO_BORDER))
        summaryTable.addCell(Cell().add(Paragraph("$${moneyFormat.format(reportData.incomeTotal)}")
            .setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER))
            
        summaryTable.addCell(Cell().add(Paragraph("Total Expenses:")).setBorder(Border.NO_BORDER))
        summaryTable.addCell(Cell().add(Paragraph("$${moneyFormat.format(reportData.expenseTotal)}")
            .setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER))
            
        summaryTable.addCell(Cell().add(Paragraph("Net:").setBold()).setBorder(Border.NO_BORDER))
        val netAmount = reportData.incomeTotal - reportData.expenseTotal
        val netColor = if (netAmount >= 0) ColorConstants.DARK_GRAY else ColorConstants.RED
        summaryTable.addCell(Cell().add(Paragraph("$${moneyFormat.format(netAmount)}")
            .setTextAlignment(TextAlignment.RIGHT)
            .setFontColor(netColor)
            .setBold()).setBorder(Border.NO_BORDER))
        
        // Advanced metrics
        // Savings Rate
        val savingsRate = if (reportData.incomeTotal > 0) 
            (reportData.incomeTotal - reportData.expenseTotal) / reportData.incomeTotal * 100 
        else 0.0
        summaryTable.addCell(Cell().add(Paragraph("Savings Rate:")).setBorder(Border.NO_BORDER))
        summaryTable.addCell(Cell().add(Paragraph(String.format("%.1f%%", savingsRate))
            .setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER))
        
        // Transaction counts
        val incomeCount = reportData.transactions.count { it.type.equals("income", ignoreCase = true) }
        val expenseCount = reportData.transactions.count { it.type.equals("expense", ignoreCase = true) }
        
        summaryTable.addCell(Cell().add(Paragraph("Income Transactions:")).setBorder(Border.NO_BORDER))
        summaryTable.addCell(Cell().add(Paragraph("$incomeCount")
            .setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER))
            
        summaryTable.addCell(Cell().add(Paragraph("Expense Transactions:")).setBorder(Border.NO_BORDER))
        summaryTable.addCell(Cell().add(Paragraph("$expenseCount")
            .setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER))
        
        // Days in period
        val daysInPeriod = ((reportData.endDate - reportData.startDate) / (1000 * 60 * 60 * 24)).toInt() + 1
        summaryTable.addCell(Cell().add(Paragraph("Days in Period:")).setBorder(Border.NO_BORDER))
        summaryTable.addCell(Cell().add(Paragraph("$daysInPeriod")
            .setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER))
        
        // Daily average
        val dailyAvg = reportData.expenseTotal / daysInPeriod
        summaryTable.addCell(Cell().add(Paragraph("Daily Average Spending:")).setBorder(Border.NO_BORDER))
        summaryTable.addCell(Cell().add(Paragraph("$${moneyFormat.format(dailyAvg)}")
            .setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER))
            
        document.add(summaryTable)
        
        // Add top expense categories
        addTopCategoriesSection(document, reportData, moneyFormat, primaryColor)
    }
    
    private fun addTopCategoriesSection(document: Document, reportData: ReportExportData, moneyFormat: DecimalFormat, primaryColor: DeviceRgb) {
        document.add(Paragraph("Top Expense Categories")
            .setFontSize(14f)
            .setBold()
            .setMarginTop(10f)
            .setMarginBottom(10f))
            
        // Get top 5 categories
        val topCategories = reportData.categoryBreakdown.take(5)
        
        // Create pie chart description
        val topCategoriesTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 30f, 20f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(15f)
            
        // Header
        topCategoriesTable.addHeaderCell(Cell().add(Paragraph("Category").setBold())
            .setBackgroundColor(primaryColor, 0.1f))
        topCategoriesTable.addHeaderCell(Cell().add(Paragraph("Amount").setBold())
            .setTextAlignment(TextAlignment.RIGHT)
            .setBackgroundColor(primaryColor, 0.1f))
        topCategoriesTable.addHeaderCell(Cell().add(Paragraph("% of Total").setBold())
            .setTextAlignment(TextAlignment.RIGHT)
            .setBackgroundColor(primaryColor, 0.1f))
            
        // Rows
        topCategories.forEach { category ->
            val percentage = if (reportData.expenseTotal > 0) 
                (category.amount / reportData.expenseTotal) * 100 
            else 0.0
            
            topCategoriesTable.addCell(Cell().add(Paragraph(category.categoryName)))
            topCategoriesTable.addCell(Cell().add(Paragraph("$${moneyFormat.format(category.amount)}")
                .setTextAlignment(TextAlignment.RIGHT)))
            topCategoriesTable.addCell(Cell().add(Paragraph(String.format("%.1f%%", percentage))
                .setTextAlignment(TextAlignment.RIGHT)))
        }
        
        document.add(topCategoriesTable)
    }
    
    private fun addCategoryBreakdownSection(
        document: Document, 
        reportData: ReportExportData, 
        moneyFormat: DecimalFormat, 
        primaryColor: DeviceRgb, 
        headerBorder: SolidBorder,
        detailed: Boolean = false
    ) {
        // Add category breakdown section
        document.add(Paragraph("Category Breakdown")
            .setFontSize(16f)
            .setBold()
            .setMarginBottom(10f))
        
        // Create category table
        val cols = if (detailed) floatArrayOf(50f, 25f, 25f) else floatArrayOf(70f, 30f)
        val categoryTable = Table(UnitValue.createPercentArray(cols))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(15f)
        
        // Add header for expense categories
        if (detailed) {
            document.add(Paragraph("Expense Categories")
                .setFontSize(14f)
                .setBold()
                .setMarginBottom(5f))
                
            categoryTable.addHeaderCell(Cell()
                .add(Paragraph("Category").setBold())
                .setBackgroundColor(primaryColor, 0.2f)
                .setBorderBottom(headerBorder))
                
            categoryTable.addHeaderCell(Cell()
                .add(Paragraph("Amount").setBold().setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(primaryColor, 0.2f)
                .setBorderBottom(headerBorder))
                
            categoryTable.addHeaderCell(Cell()
                .add(Paragraph("% of Total").setBold().setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(primaryColor, 0.2f)
                .setBorderBottom(headerBorder))
        } else {
            categoryTable.addHeaderCell(Cell()
                .add(Paragraph("Category").setBold())
                .setBackgroundColor(primaryColor, 0.2f)
                .setBorderBottom(headerBorder))
                
            categoryTable.addHeaderCell(Cell()
                .add(Paragraph("Amount").setBold().setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(primaryColor, 0.2f)
                .setBorderBottom(headerBorder))
        }
        
        // Add expense category rows
        reportData.categoryBreakdown.forEach { category ->
            categoryTable.addCell(Cell().add(Paragraph(category.categoryName)))
            categoryTable.addCell(Cell().add(Paragraph("$${moneyFormat.format(category.amount)}")
                .setTextAlignment(TextAlignment.RIGHT)))
                
            if (detailed) {
                val percentage = if (reportData.expenseTotal > 0) 
                    (category.amount / reportData.expenseTotal) * 100 
                else 0.0
                categoryTable.addCell(Cell().add(Paragraph(String.format("%.1f%%", percentage))
                    .setTextAlignment(TextAlignment.RIGHT)))
            }
        }
        
        document.add(categoryTable)
        
        // If detailed view, also add income categories
        if (detailed) {
            // Calculate income by category
            val incomeCategories = mutableMapOf<String, Double>()
            reportData.transactions
                .filter { it.type.equals("income", ignoreCase = true) }
                .forEach { transaction ->
                    val categoryId = transaction.categoryId
                    incomeCategories[categoryId] = (incomeCategories[categoryId] ?: 0.0) + transaction.amount
                }
                
            if (incomeCategories.isNotEmpty()) {
                document.add(Paragraph("Income Categories")
                    .setFontSize(14f)
                    .setBold()
                    .setMarginTop(10f)
                    .setMarginBottom(5f))
                    
                val incomeCategoryTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 25f, 25f)))
                    .setWidth(UnitValue.createPercentValue(100f))
                    .setMarginBottom(15f)
                    
                // Add header
                incomeCategoryTable.addHeaderCell(Cell()
                    .add(Paragraph("Category").setBold())
                    .setBackgroundColor(primaryColor, 0.2f)
                    .setBorderBottom(headerBorder))
                    
                incomeCategoryTable.addHeaderCell(Cell()
                    .add(Paragraph("Amount").setBold().setTextAlignment(TextAlignment.RIGHT))
                    .setBackgroundColor(primaryColor, 0.2f)
                    .setBorderBottom(headerBorder))
                    
                incomeCategoryTable.addHeaderCell(Cell()
                    .add(Paragraph("% of Total").setBold().setTextAlignment(TextAlignment.RIGHT))
                    .setBackgroundColor(primaryColor, 0.2f)
                    .setBorderBottom(headerBorder))
                
                // Add rows
                incomeCategories.forEach { (categoryId, amount) ->
                    val categoryName = reportData.categories[categoryId]?.name ?: "Unknown"
                    val percentage = if (reportData.incomeTotal > 0) 
                        (amount / reportData.incomeTotal) * 100 
                    else 0.0
                        
                    incomeCategoryTable.addCell(Cell().add(Paragraph(categoryName)))
                    incomeCategoryTable.addCell(Cell().add(Paragraph("$${moneyFormat.format(amount)}")
                        .setTextAlignment(TextAlignment.RIGHT)))
                    incomeCategoryTable.addCell(Cell().add(Paragraph(String.format("%.1f%%", percentage))
                        .setTextAlignment(TextAlignment.RIGHT)))
                }
                
                document.add(incomeCategoryTable)
            }
        }
    }
    
    private fun addTransactionsSection(
        document: Document, 
        reportData: ReportExportData, 
        dateFormat: SimpleDateFormat, 
        moneyFormat: DecimalFormat, 
        primaryColor: DeviceRgb, 
        headerBorder: SolidBorder
    ) {
        // Add transactions section
        document.add(Paragraph("Transactions")
            .setFontSize(16f)
            .setBold()
            .setMarginBottom(10f))
        
        // Create transaction table
        val transactionTable = Table(UnitValue.createPercentArray(floatArrayOf(20f, 20f, 30f, 15f, 15f)))
            .setWidth(UnitValue.createPercentValue(100f))
        
        // Add header
        transactionTable.addHeaderCell(Cell()
            .add(Paragraph("Date").setBold())
            .setBackgroundColor(primaryColor, 0.2f)
            .setBorderBottom(headerBorder))
            
        transactionTable.addHeaderCell(Cell()
            .add(Paragraph("Category").setBold())
            .setBackgroundColor(primaryColor, 0.2f)
            .setBorderBottom(headerBorder))
            
        transactionTable.addHeaderCell(Cell()
            .add(Paragraph("Description").setBold())
            .setBackgroundColor(primaryColor, 0.2f)
            .setBorderBottom(headerBorder))
            
        transactionTable.addHeaderCell(Cell()
            .add(Paragraph("Amount").setBold().setTextAlignment(TextAlignment.RIGHT))
            .setBackgroundColor(primaryColor, 0.2f)
            .setBorderBottom(headerBorder))
            
        transactionTable.addHeaderCell(Cell()
            .add(Paragraph("Type").setBold())
            .setBackgroundColor(primaryColor, 0.2f)
            .setBorderBottom(headerBorder))
        
        // Add rows
        reportData.transactions.forEach { transaction ->
            val category = reportData.categories[transaction.categoryId]?.name ?: "Unknown"
            
            transactionTable.addCell(Cell().add(Paragraph(dateFormat.format(Date(transaction.date)))))
            transactionTable.addCell(Cell().add(Paragraph(category)))
            transactionTable.addCell(Cell().add(Paragraph(transaction.description ?: "")))
            
            val amountCell = Cell().add(Paragraph("$${moneyFormat.format(transaction.amount)}")
                .setTextAlignment(TextAlignment.RIGHT))
            transactionTable.addCell(amountCell)
            
            transactionTable.addCell(Cell().add(Paragraph(transaction.type)))
        }
        
        document.add(transactionTable)
    }
}
