package com.example.handspeak.ui.screen.voicetosign

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.handspeak.data.database.AppDatabase
import com.example.handspeak.data.database.HistoryEntity
import com.example.handspeak.data.model.SignInfo
import com.example.handspeak.data.repository.HistoryRepository
import com.example.handspeak.util.JsonHelper
import com.example.handspeak.util.ImageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VoiceToSignUiState(
    val recognizedText: String = "",
    val isListening: Boolean = false,
    val signInfo: SignInfo? = null,
    val signSequence: List<SignInfo>? = null,
    val errorMessage: String? = null,
    val isPlaying: Boolean = false
)

class VoiceToSignViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(VoiceToSignUiState())
    val uiState: StateFlow<VoiceToSignUiState> = _uiState.asStateFlow()
    
    private val signMap: Map<String, SignInfo> = JsonHelper.loadSignMap(application)
    private val repository: HistoryRepository
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = HistoryRepository(database.historyDao())
    }
    
    fun setListening(listening: Boolean) {
        _uiState.value = _uiState.value.copy(isListening = listening)
    }
    
    fun onSpeechResult(text: String) {
        Log.d("VoiceToSignViewModel", "Speech result received: $text")
        _uiState.value = _uiState.value.copy(
            recognizedText = text,
            isListening = false,
            errorMessage = null
        )
        translateToSign(text)
    }
    
    fun onSpeechError(error: String) {
        _uiState.value = _uiState.value.copy(
            isListening = false,
            errorMessage = error
        )
    }
    
    private fun translateToSign(text: String) {
        val trimmedText = text.trim()
        
        Log.d("VoiceToSignViewModel", "Translating text: '$trimmedText'")
        Log.d("VoiceToSignViewModel", "SignMap size: ${signMap.size}")
        Log.d("VoiceToSignViewModel", "SignMap keys: ${signMap.keys.take(5)}")
        
        if (trimmedText.isEmpty()) {
            Log.w("VoiceToSignViewModel", "Text is empty")
            _uiState.value = _uiState.value.copy(
                errorMessage = "لم يتم التعرف على أي كلام"
            )
            return
        }
        
        // Search in sign map - try exact match first
        var signInfo: SignInfo? = signMap[trimmedText]
        
        if (signInfo == null) {
            // Try case-insensitive exact match
            signInfo = signMap.entries.firstOrNull { 
                it.key.equals(trimmedText, ignoreCase = true) 
            }?.value
        }
        
        if (signInfo == null) {
            // Try partial match (contains)
            signInfo = signMap.entries.firstOrNull { 
                it.key.contains(trimmedText, ignoreCase = true) || 
                trimmedText.contains(it.key, ignoreCase = true)
            }?.value
        }
        
        if (signInfo == null) {
            // Try word-by-word match
            val words = trimmedText.split("\\s+".toRegex())
            signInfo = words.firstOrNull()?.let { firstWord ->
                signMap.entries.firstOrNull { 
                    it.key.contains(firstWord, ignoreCase = true) || 
                    firstWord.contains(it.key, ignoreCase = true)
                }?.value
            }
        }
        
        // Decide whether to use word-level sign or fall back to characters
        // 1) If a direct sign exists BUT has no images, treat as not found
        val app = getApplication<Application>()
        run {
            val folder: String? = signInfo?.folder
            if (folder != null) {
                val count = ImageHelper.getImageCount(app, folder)
                if (count == 0) {
                    signInfo = null
                }
            }
        }

        // 2) Build per-character sequence regardless; we'll prefer it if non-empty
        val sequence = if (trimmedText.length > 1) {
            val list = mutableListOf<SignInfo>()
            trimmedText.forEach { ch ->
                val si = signMap[ch.toString()]
                if (si != null) list.add(si)
            }
            if (list.isNotEmpty()) list else null
        } else null
        
        if (sequence != null) {
            _uiState.value = _uiState.value.copy(
                signInfo = null,
                signSequence = sequence,
                errorMessage = null
            )
            saveToHistory(trimmedText)
        } else {
            val resolved: SignInfo? = signInfo
            if (resolved != null) {
                Log.d("VoiceToSignViewModel", "Found sign: ${resolved.label}")
                _uiState.value = _uiState.value.copy(
                    signInfo = resolved,
                    signSequence = null,
                    errorMessage = null
                )
                saveToHistory(trimmedText)
            } else {
            Log.w("VoiceToSignViewModel", "No sign found for: '$trimmedText'")
            _uiState.value = _uiState.value.copy(
                signInfo = null,
                signSequence = null,
                errorMessage = "لم يتم العثور على إشارة مطابقة للنص: '$trimmedText'. جرب كلمات مثل: مرحبا، شكرا، نعم، لا"
            )
            }
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
                    translationType = "voice_to_sign"
                )
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun clear() {
        _uiState.value = VoiceToSignUiState()
    }
}


