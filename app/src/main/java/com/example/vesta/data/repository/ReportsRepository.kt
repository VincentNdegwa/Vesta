package com.example.vesta.data.repository

import com.example.vesta.data.local.entities.CategoryEntity
import com.example.vesta.data.local.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository class for reports related data operations
 */
@Singleton
class ReportsRepository @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {

    /**
     * Get monthly income vs expense data for reports
     */
    suspend fun getMonthlyIncomeVsExpense(
        userId: String,
        monthsToShow: Int = 6
    ): List<MonthlyFinanceData> {
        val currentTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime

        val months = mutableListOf<MonthlyFinanceData>()

        // Go back months-to-show and then loop forward
        calendar.add(Calendar.MONTH, -(monthsToShow - 1))
        
        // Set to beginning of that month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // For each month
        for (i in 0 until monthsToShow) {
            val startDate = calendar.timeInMillis
            
            // Move to end of month
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            
            val endDate = calendar.timeInMillis
            
            // Get income and expenses for this month
            val income = transactionRepository.getTotalIncomeForPeriod(userId, startDate, endDate)
            val expense = transactionRepository.getTotalExpenseForPeriod(userId, startDate, endDate)
            
            // Get month name
            val monthName = getMonthName(calendar.get(Calendar.MONTH))
            val year = calendar.get(Calendar.YEAR)
            
            months.add(MonthlyFinanceData(monthName, year, income, expense))
            
            // Move to first of next month
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }
        
        return months
    }
    
    /**
     * Get weekly income vs expense data for reports
     */
    suspend fun getWeeklyIncomeVsExpense(
        userId: String,
        weeksToShow: Int = 4
    ): List<WeeklyFinanceData> {
        val currentTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime
        
        val weeks = mutableListOf<WeeklyFinanceData>()
        
        // Go back to beginning of current week (Sunday)
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            calendar.add(Calendar.DAY_OF_WEEK, -1)
        }
        
        // Then go back by (weeksToShow - 1) weeks
        calendar.add(Calendar.WEEK_OF_YEAR, -(weeksToShow - 1))
        
        // Start from beginning of day
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // For each week
        for (i in 0 until weeksToShow) {
            val startDate = calendar.timeInMillis
            
            // Move to end of week (Saturday)
            calendar.add(Calendar.DAY_OF_WEEK, 6)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            
            val endDate = calendar.timeInMillis
            
            // Get income and expenses for this week
            val income = transactionRepository.getTotalIncomeForPeriod(userId, startDate, endDate)
            val expense = transactionRepository.getTotalExpenseForPeriod(userId, startDate, endDate)
            
            val weekNumber = i + 1 // Week 1, Week 2, etc.
            
            weeks.add(WeeklyFinanceData(weekNumber, income, expense))
            
            // Move to first of next week
            calendar.add(Calendar.DAY_OF_WEEK, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }
        
        return weeks
    }
    
    /**
     * Get category breakdown data for reports
     */
    suspend fun getCategoryBreakdown(
        userId: String,
        startDate: Long,
        endDate: Long,
        type: String = "expense" // Default to expense categories
    ): List<CategorySpending> {
        // Get all transactions for the period
        val transactions = transactionRepository.getTransactionsByDateRange(userId, startDate, endDate)
            .filter { it.type.equals(type, ignoreCase = true) }
        
        // Get all categories for the user
        val categories = when (type.lowercase()) {
            "expense" -> categoryRepository.getCategories(userId).filter { it.type.equals("expense", ignoreCase = true) }
            "income" -> categoryRepository.getCategories(userId).filter { it.type.equals("income", ignoreCase = true) }
            else -> categoryRepository.getCategories(userId)
        }
        
        // Map of categoryId to CategoryEntity
        val categoryMap = categories.associateBy { it.id }
        
        // Calculate spending by category
        val categorySpending = mutableMapOf<String, Double>()
        transactions.forEach { transaction ->
            val categoryId = transaction.categoryId
            categorySpending[categoryId] = (categorySpending[categoryId] ?: 0.0) + transaction.amount
        }
        
        // Create CategorySpending objects
        return categorySpending.map { (categoryId, amount) ->
            val category = categoryMap[categoryId]
            CategorySpending(
                categoryId = categoryId,
                categoryName = category?.name ?: "Unknown",
                amount = amount,
                color = category?.color ?: "#808080", // Default gray if no color
                icon = category?.icon
            )
        }.sortedByDescending { it.amount }
    }
    
    /**
     * Get monthly spending by category for reports
     */
    suspend fun getMonthlyCategorySpending(
        userId: String,
        monthsToShow: Int = 6
    ): List<MonthlyCategoryData> {
        val currentTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime

        val monthlyData = mutableListOf<MonthlyCategoryData>()

        // Go back months-to-show and then loop forward
        calendar.add(Calendar.MONTH, -(monthsToShow - 1))
        
        // Set to beginning of that month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Get all categories for pre-loading
        val categories = categoryRepository.getCategories(userId)
        val categoryMap = categories.associateBy { it.id }

        // For each month
        for (i in 0 until monthsToShow) {
            val startDate = calendar.timeInMillis
            
            // Move to end of month
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            
            val endDate = calendar.timeInMillis
            
            // Get transactions for this month
            val transactions = transactionRepository.getTransactionsByDateRange(userId, startDate, endDate)
                .filter { it.type.equals("expense", ignoreCase = true) }
            
            // Calculate total spending
            val totalSpending = transactions.sumOf { it.amount }
            
            // Get month name
            val monthName = getMonthName(calendar.get(Calendar.MONTH))
            val year = calendar.get(Calendar.YEAR)
            
            // Calculate category breakdown
            val categoryBreakdown = mutableMapOf<String, Double>()
            transactions.forEach { transaction ->
                val categoryId = transaction.categoryId
                categoryBreakdown[categoryId] = (categoryBreakdown[categoryId] ?: 0.0) + transaction.amount
            }
            
            val categoryPercentages = categoryBreakdown.map { (categoryId, amount) ->
                val category = categoryMap[categoryId]
                CategoryPercentage(
                    categoryId = categoryId,
                    categoryName = category?.name ?: "Unknown",
                    percentage = if (totalSpending > 0) (amount / totalSpending) * 100 else 0.0,
                    color = category?.color ?: "#808080"
                )
            }.sortedByDescending { it.percentage }
            
            monthlyData.add(MonthlyCategoryData(
                month = monthName,
                year = year,
                totalSpending = totalSpending,
                categories = categoryPercentages
            ))
            
            // Move to first of next month
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }
        
        return monthlyData
    }
    
    /**
     * Get report data for export
     */
    suspend fun getExportData(
        userId: String,
        startDate: Long,
        endDate: Long,
        reportType: String
    ): ReportExportData {
        val transactions = transactionRepository.getTransactionsByDateRange(userId, startDate, endDate)
        val categories = categoryRepository.getCategories(userId)
        
        val incomeTotal = transactions
            .filter { it.type.equals("income", ignoreCase = true) }
            .sumOf { it.amount }
            
        val expenseTotal = transactions
            .filter { it.type.equals("expense", ignoreCase = true) }
            .sumOf { it.amount }
        
        val categoryMap = categories.associateBy { it.id }
        
        val categoryBreakdown = getCategoryBreakdown(userId, startDate, endDate, "expense")
        
        return ReportExportData(
            transactions = transactions,
            categories = categoryMap,
            startDate = startDate,
            endDate = endDate,
            incomeTotal = incomeTotal,
            expenseTotal = expenseTotal,
            categoryBreakdown = categoryBreakdown,
            reportType = reportType
        )
    }
    
    /**
     * Get overview data for reports dashboard
     */
    suspend fun getReportOverview(userId: String): ReportOverview {
        val calendar = Calendar.getInstance()
        val currentTime = calendar.timeInMillis

        // Calculate this month's range
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStartDate = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val monthEndDate = calendar.timeInMillis

        // Get month totals
        val monthlyIncome = transactionRepository.getTotalIncomeForPeriod(userId, monthStartDate, monthEndDate)
        val monthlyExpense = transactionRepository.getTotalExpenseForPeriod(userId, monthStartDate, monthEndDate)
        val monthlyNet = monthlyIncome - monthlyExpense
        
        // Calculate daily average - use total days in the month for more accurate average
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dailyAverage = monthlyExpense / daysInMonth
        
        // Calculate savings percentage - ensure proper precision
        val savingsPercentage = if (monthlyIncome > 0) ((monthlyIncome - monthlyExpense) / monthlyIncome) * 100 else 0.0
        
        return ReportOverview(
            monthlyNet = monthlyNet,
            dailyAverage = dailyAverage,
            savingsPercentage = savingsPercentage
        )
    }
    
    private fun getMonthName(month: Int): String {
        return when (month) {
            Calendar.JANUARY -> "Jan"
            Calendar.FEBRUARY -> "Feb"
            Calendar.MARCH -> "Mar"
            Calendar.APRIL -> "Apr"
            Calendar.MAY -> "May"
            Calendar.JUNE -> "Jun"
            Calendar.JULY -> "Jul"
            Calendar.AUGUST -> "Aug"
            Calendar.SEPTEMBER -> "Sep"
            Calendar.OCTOBER -> "Oct"
            Calendar.NOVEMBER -> "Nov"
            Calendar.DECEMBER -> "Dec"
            else -> "Unknown"
        }
    }
}

// Data classes for reports
data class MonthlyFinanceData(
    val month: String,
    val year: Int,
    val income: Double,
    val expense: Double,
    val net: Double = income - expense
)

data class WeeklyFinanceData(
    val week: Int,
    val income: Double,
    val expense: Double,
    val net: Double = income - expense
)

data class CategorySpending(
    val categoryId: String,
    val categoryName: String,
    val amount: Double,
    val color: String,
    val icon: String?
)

data class CategoryPercentage(
    val categoryId: String,
    val categoryName: String,
    val percentage: Double,
    val color: String
)

data class MonthlyCategoryData(
    val month: String,
    val year: Int,
    val totalSpending: Double,
    val categories: List<CategoryPercentage>
)

data class ReportOverview(
    val monthlyNet: Double,
    val dailyAverage: Double,
    val savingsPercentage: Double
)

data class ReportExportData(
    val transactions: List<TransactionEntity>,
    val categories: Map<String, CategoryEntity>,
    val startDate: Long,
    val endDate: Long,
    val incomeTotal: Double,
    val expenseTotal: Double,
    val categoryBreakdown: List<CategorySpending>,
    val reportType: String
)
