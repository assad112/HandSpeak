package com.example.handspeak.ui.screen.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.handspeak.data.database.AppDatabase
import com.example.handspeak.data.database.HistoryEntity
import com.example.handspeak.data.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryUiState(
    val historyItems: List<HistoryEntity> = emptyList(),
    val selectedFilter: String = "all", // "all", "sign_to_text", "text_to_sign", "voice_to_sign"
    val showDeleteDialog: Boolean = false,
    val itemToDelete: HistoryEntity? = null
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    private val repository: HistoryRepository
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = HistoryRepository(database.historyDao())
        
        loadHistory()
    }
    
    private fun loadHistory() {
        viewModelScope.launch {
            repository.allHistory.collect { items ->
                updateFilteredItems(items)
            }
        }
    }
    
    fun setFilter(filter: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        loadHistory()
    }
    
    private fun updateFilteredItems(allItems: List<HistoryEntity>) {
        val filtered = when (_uiState.value.selectedFilter) {
            "all" -> allItems
            else -> allItems.filter { it.translationType == _uiState.value.selectedFilter }
        }
        _uiState.value = _uiState.value.copy(historyItems = filtered)
    }
    
    fun showDeleteDialog(item: HistoryEntity) {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = true,
            itemToDelete = item
        )
    }
    
    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = false,
            itemToDelete = null
        )
    }
    
    fun deleteItem() {
        viewModelScope.launch {
            _uiState.value.itemToDelete?.let {
                repository.delete(it)
            }
            hideDeleteDialog()
        }
    }
    
    fun deleteAll() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }
}











