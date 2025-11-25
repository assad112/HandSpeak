package com.example.handspeak.ui.screen.texttosign

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.handspeak.data.database.AppDatabase
import com.example.handspeak.data.database.HistoryEntity
import com.example.handspeak.data.model.SignInfo
import com.example.handspeak.data.repository.HistoryRepository
import com.example.handspeak.util.JsonHelper
import com.example.handspeak.util.TextToSpeechHelper
import com.example.handspeak.util.ImageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TextToSignUiState(
    val inputText: String = "",
    val signInfo: SignInfo? = null,
    val signSequence: List<SignInfo>? = null,
    val errorMessage: String? = null,
    val isPlaying: Boolean = false
)

class TextToSignViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(TextToSignUiState())
    val uiState: StateFlow<TextToSignUiState> = _uiState.asStateFlow()
    
    private val signMap: Map<String, SignInfo> = JsonHelper.loadSignMap(application)
    private val repository: HistoryRepository
    private val ttsHelper: TextToSpeechHelper
    private val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = HistoryRepository(database.historyDao())
        
        // Initialize Text-to-Speech
        ttsHelper = TextToSpeechHelper(application)
        ttsHelper.initialize { success ->
            if (success) {
                Log.d("TextToSignViewModel", "TTS initialized successfully")
            } else {
                Log.e("TextToSignViewModel", "TTS initialization failed")
            }
        }
    }
    
    fun onTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }
    
    fun translateToSign() {
        val text = _uiState.value.inputText.trim()
        
        if (text.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "الرجاء إدخال نص للترجمة"
            )
            return
        }
        
        // 1) Try exact/contains mapping first (prioritize full words/phrases)
        var direct = signMap[text] ?: signMap.entries.firstOrNull {
            it.key.contains(text, ignoreCase = true) || text.contains(it.key, ignoreCase = true)
        }?.value
        
        // Check if direct mapping has images
        var directHasImages = false
        run {
            val folder: String? = direct?.folder
            if (folder != null) {
                val count = ImageHelper.getImageCount(getApplication(), folder)
                directHasImages = count > 0
                if (!directHasImages) {
                    direct = null
                }
            }
        }

        // 2) If direct mapping found with images, use it (don't split into characters)
        if (direct != null && directHasImages) {
            _uiState.value = _uiState.value.copy(
                signInfo = direct,
                signSequence = null,
                errorMessage = null
            )
            saveToHistory(text)
            
            // Speak the text if sound is enabled
            val enableSound = prefs.getBoolean("enable_sound", true)
            if (enableSound) {
                speakText(text)
            }
            return
        }

        // 3) If no direct mapping found, try per-character mapping (e.g., مرحبا → م ر ح ب ا)
        val sequence = if (text.length > 1) {
            val list = mutableListOf<SignInfo>()
            text.forEach { ch ->
                val key = ch.toString()
                val item = signMap[key]
                if (item != null) list.add(item)
            }
            if (list.isNotEmpty()) list else null
        } else null

        if (sequence != null) {
            _uiState.value = _uiState.value.copy(
                signInfo = null,
                signSequence = sequence,
                errorMessage = null
            )
            saveToHistory(text)
        } else {
            _uiState.value = _uiState.value.copy(
                signInfo = null,
                signSequence = null,
                errorMessage = "لم يتم العثور على إشارة مطابقة للنص المدخل. حاول مع نص آخر."
            )
        }
    }
    
    fun setIsPlaying(playing: Boolean) {
        _uiState.value = _uiState.value.copy(isPlaying = playing)
    }
    
    private fun saveToHistory(text: String) {
        viewModelScope.launch {
            repository.insert(
                HistoryEntity(
                    text = text,
                    translationType = "text_to_sign"
                )
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun clear() {
        _uiState.value = TextToSignUiState()
        ttsHelper.stop()
    }
    
    fun speakText(text: String) {
        val enableSound = prefs.getBoolean("enable_sound", true)
        if (enableSound && text.isNotEmpty()) {
            ttsHelper.speak(text, enableSound)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        ttsHelper.shutdown()
    }
}


