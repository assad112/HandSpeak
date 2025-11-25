package com.example.handspeak.ui.screen.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.handspeak.ml.AdaptiveLearningHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val darkMode: Boolean = false,
    val animationSpeed: Float = 1f, // 0.5 to 2.0
    val confidenceThreshold: Float = 0.6f, // 0.5 to 0.9
    val showConfidence: Boolean = true,
    val enableSound: Boolean = true,
    val enableAdaptiveLearning: Boolean = true,
    val learningSampleCount: Int = 0,
    val useLSTM: Boolean = true // استخدام LSTM أو Dense
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    private val _uiState = MutableStateFlow(loadSettings())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        // تحديث عدد العينات عند التحميل
        updateLearningSampleCount()
    }
    
    private fun loadSettings(): SettingsUiState {
        val app = getApplication<Application>()
        val sampleCount = AdaptiveLearningHelper.getTotalSampleCount(app)
        return SettingsUiState(
            darkMode = prefs.getBoolean("dark_mode", false),
            animationSpeed = prefs.getFloat("animation_speed", 1f),
            confidenceThreshold = prefs.getFloat("confidence_threshold", 0.6f),
            showConfidence = prefs.getBoolean("show_confidence", true),
            enableSound = prefs.getBoolean("enable_sound", true),
            enableAdaptiveLearning = prefs.getBoolean("enable_adaptive_learning", true),
            learningSampleCount = sampleCount,
            useLSTM = prefs.getBoolean("use_lstm", true)
        )
    }
    
    private fun updateLearningSampleCount() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val count = AdaptiveLearningHelper.getTotalSampleCount(app)
            _uiState.value = _uiState.value.copy(learningSampleCount = count)
        }
    }
    
    fun setDarkMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(darkMode = enabled)
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }
    
    fun setAnimationSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(animationSpeed = speed)
        prefs.edit().putFloat("animation_speed", speed).apply()
    }
    
    fun setConfidenceThreshold(threshold: Float) {
        _uiState.value = _uiState.value.copy(confidenceThreshold = threshold)
        prefs.edit().putFloat("confidence_threshold", threshold).apply()
    }
    
    fun setShowConfidence(show: Boolean) {
        _uiState.value = _uiState.value.copy(showConfidence = show)
        prefs.edit().putBoolean("show_confidence", show).apply()
    }
    
    fun setEnableSound(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(enableSound = enabled)
        prefs.edit().putBoolean("enable_sound", enabled).apply()
    }
    
    fun setEnableAdaptiveLearning(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(enableAdaptiveLearning = enabled)
        prefs.edit().putBoolean("enable_adaptive_learning", enabled).apply()
    }
    
    fun refreshLearningStats() {
        updateLearningSampleCount()
    }
    
    fun setUseLSTM(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(useLSTM = enabled)
        prefs.edit().putBoolean("use_lstm", enabled).apply()
    }
    
    fun resetToDefaults() {
        val defaults = SettingsUiState()
        _uiState.value = defaults
        with(prefs.edit()) {
            putBoolean("dark_mode", defaults.darkMode)
            putFloat("animation_speed", defaults.animationSpeed)
            putFloat("confidence_threshold", defaults.confidenceThreshold)
            putBoolean("show_confidence", defaults.showConfidence)
            putBoolean("enable_sound", defaults.enableSound)
            putBoolean("enable_adaptive_learning", defaults.enableAdaptiveLearning)
            putBoolean("use_lstm", defaults.useLSTM)
            apply()
        }
        updateLearningSampleCount()
    }
}


