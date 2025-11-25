package com.example.handspeak.ui.screen.account

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.handspeak.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AccountUiState(
    val currentUser: FirebaseUser? = null,
    val displayName: String = "",
    val email: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AccountViewModel(application: Application) : AndroidViewModel(application) {
    
    private val authRepository = AuthRepository()
    
    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()
    
    init {
        loadUserInfo()
    }
    
    private fun loadUserInfo() {
        val user = authRepository.getCurrentUser()
        _uiState.value = _uiState.value.copy(
            currentUser = user,
            displayName = user?.displayName ?: "",
            email = user?.email ?: ""
        )
    }
    
    fun updateDisplayName(newName: String) {
        if (newName.trim().isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "الاسم لا يمكن أن يكون فارغاً"
            )
            return
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            val result = authRepository.updateDisplayName(newName.trim())
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    displayName = newName.trim(),
                    successMessage = "تم تحديث الاسم بنجاح"
                )
                loadUserInfo() // Refresh user info
            } else {
                val exception = result.exceptionOrNull() ?: Exception("Unknown error")
                Log.e("AccountViewModel", "Error updating display name", exception)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "فشل تحديث الاسم: ${exception.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
    
    fun refreshUserInfo() {
        loadUserInfo()
    }
}

