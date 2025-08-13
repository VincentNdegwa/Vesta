package com.example.vesta.ui.reports.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vesta.data.repository.CategorySpending
import com.example.vesta.data.repository.MonthlyFinanceData
import com.example.vesta.data.repository.ReportExportData
import com.example.vesta.data.repository.ReportOverview
import com.example.vesta.data.repository.ReportsRepository
import com.example.vesta.data.repository.WeeklyFinanceData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportsRepository: ReportsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()
    
    fun loadReportData(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Load overview data
                val overview = reportsRepository.getReportOverview(userId)
                
                // Load monthly income/expense trend
                val monthlyData = reportsRepository.getMonthlyIncomeVsExpense(userId)
                
                // Load weekly data
                val weeklyData = reportsRepository.getWeeklyIncomeVsExpense(userId)
                
                // Load category breakdown (last 30 days)
                val calendar = Calendar.getInstance()
                val endDate = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, -30)
                val startDate = calendar.timeInMillis
                
                val categoryBreakdown = reportsRepository.getCategoryBreakdown(userId, startDate, endDate)
                
                // Monthly category spending
                val monthlyCategoryData = reportsRepository.getMonthlyCategorySpending(userId)
                
                // Update UI state with all data
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        overview = overview,
                        monthlyFinanceData = monthlyData,
                        weeklyFinanceData = weeklyData,
                        categoryBreakdown = categoryBreakdown,
                        monthlyCategoryData = monthlyCategoryData
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to load report data: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    fun prepareExportData(userId: String, startDate: Long, endDate: Long, reportType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isExporting = true) }
            
            try {
                val exportData = reportsRepository.getExportData(userId, startDate, endDate, reportType)
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isExporting = false,
                        exportData = exportData
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isExporting = false,
                        error = "Failed to prepare export data: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun exportComplete() {
        _uiState.update { it.copy(isExporting = false) }
    }
}

/**
 * UI state for reports screens
 */
data class ReportsUiState(
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val error: String? = null,
    val overview: ReportOverview = ReportOverview(0.0, 0.0, 0.0),
    val monthlyFinanceData: List<MonthlyFinanceData> = emptyList(),
    val weeklyFinanceData: List<WeeklyFinanceData> = emptyList(),
    val categoryBreakdown: List<CategorySpending> = emptyList(),
    val monthlyCategoryData: List<com.example.vesta.data.repository.MonthlyCategoryData> = emptyList(),
    val exportData: ReportExportData? = null
)
