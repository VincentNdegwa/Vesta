package com.example.vesta.utils

import android.content.Context
import android.net.Uri
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
            // Create file in app's cache directory
            val reportFile = File(context.cacheDir, fileName)
            val writer = BufferedWriter(FileWriter(reportFile))
            
            // Helper function to properly escape CSV values
            fun escapeCSV(value: String): String {
                // If the value contains comma, newline, or double quote, wrap it in quotes and escape internal quotes
                if (value.contains(",") || value.contains("\n") || value.contains("\"")) {
                    return "\"" + value.replace("\"", "\"\"") + "\""
                }
                return value
            }
            
            // Helper function to write a CSV line
            fun writeLine(vararg values: String) {
                writer.write(values.joinToString(",") { escapeCSV(it) })
                writer.newLine()
            }
            
            // Metadata headers
            writeLine("Report Type:", reportData.reportType)
            writeLine("Period:", 
                "${dateFormat.format(Date(reportData.startDate))} to ${dateFormat.format(Date(reportData.endDate))}")
            writeLine("Total Income:", moneyFormat.format(reportData.incomeTotal))
            writeLine("Total Expenses:", moneyFormat.format(reportData.expenseTotal))
            writeLine("Net:", moneyFormat.format(reportData.incomeTotal - reportData.expenseTotal))
            writer.newLine() // Empty row
            
            // Category breakdown
            writeLine("Category Breakdown")
            writeLine("Category", "Amount")
            reportData.categoryBreakdown.forEach { category ->
                writeLine(category.categoryName, moneyFormat.format(category.amount))
            }
            writer.newLine() // Empty row
            
            // Transaction data with proper column headers
            writeLine("Date", "Category", "Description", "Amount", "Type")
            
            reportData.transactions.forEach { transaction ->
                val category = reportData.categories[transaction.categoryId]?.name ?: "Unknown"
                writeLine(
                    dateFormat.format(Date(transaction.date)),
                    category,
                    transaction.description ?: "",
                    moneyFormat.format(transaction.amount),
                    transaction.type
                )
            }
            
            writer.flush()
            writer.close()
            
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
            
            // Add title
            val title = Paragraph("Vesta Financial Report")
                .setFontSize(24f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20f)
            document.add(title)
            
            // Add metadata
            document.add(Paragraph("Report Type: ${reportData.reportType}")
                .setFontSize(12f)
                .setMarginBottom(5f))
                
            document.add(Paragraph("Period: ${dateFormat.format(Date(reportData.startDate))} to ${dateFormat.format(Date(reportData.endDate))}")
                .setFontSize(12f)
                .setMarginBottom(10f))
            
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
            
            // Add category breakdown section
            document.add(Paragraph("Category Breakdown")
                .setFontSize(16f)
                .setBold()
                .setMarginBottom(10f))
            
            // Create category table
            val categoryTable = Table(UnitValue.createPercentArray(floatArrayOf(70f, 30f)))
                .setWidth(UnitValue.createPercentValue(100f))
                .setMarginBottom(15f)
            
            // Add header
            categoryTable.addHeaderCell(Cell()
                .add(Paragraph("Category").setBold())
                .setBackgroundColor(primaryColor, 0.2f)
                .setBorderBottom(headerBorder))
                
            categoryTable.addHeaderCell(Cell()
                .add(Paragraph("Amount").setBold().setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(primaryColor, 0.2f)
                .setBorderBottom(headerBorder))
            
            // Add rows
            reportData.categoryBreakdown.forEach { category ->
                categoryTable.addCell(Cell().add(Paragraph(category.categoryName)))
                categoryTable.addCell(Cell().add(Paragraph("$${moneyFormat.format(category.amount)}")
                    .setTextAlignment(TextAlignment.RIGHT)))
            }
            
            document.add(categoryTable)
            
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
}
