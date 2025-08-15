package com.example.vesta.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vesta.data.local.entities.CategoryEntity
import com.example.vesta.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val incomeCategories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    fun loadCategories(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                categoryRepository.insertDefaultCategoriesIfNone(userId)
                
                val all = categoryRepository.getCategories(userId)
                
                val expense = all.filter { it.type.equals("EXPENSE", ignoreCase = true) }
                val income = all.filter { it.type.equals("INCOME", ignoreCase = true) }
                
                _uiState.update { 
                    it.copy(
                        categories = all, 
                        expenseCategories = expense, 
                        incomeCategories = income, 
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = "Failed to load categories: ${e.message}", 
                        isLoading = false
                    )
                }
            }
        }
    }
}
