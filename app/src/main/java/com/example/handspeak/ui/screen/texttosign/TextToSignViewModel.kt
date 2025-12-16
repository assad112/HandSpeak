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
        
        // Always split text into unique characters to show individual sign images
        // This allows users to see how each letter is signed without duplicates
        val sequence = if (text.isNotEmpty()) {
            val list = mutableListOf<SignInfo>()
            val seenChars = mutableSetOf<String>() // لتجنب تكرار الأحرف
            text.forEach { ch ->
                // Skip spaces and whitespace
                if (!ch.isWhitespace()) {
                    val key = ch.toString()
                    // التحقق من أن الحرف لم يُضاف من قبل
                    if (!seenChars.contains(key)) {
                        seenChars.add(key)
                    val item = signMap[key]
                        if (item != null && item.folder != null) {
                            // التحقق من عدم إضافة نفس SignInfo مرتين
                            if (!list.any { it.folder == item.folder && it.label == item.label }) {
                        list.add(item)
                            }
                    } else {
                        // Log missing character for debugging
                        Log.w("TextToSignViewModel", "Character not found in sign map: '$key' (Unicode: ${ch.code})")
                    }
                }
            }
            }
            // تأكيد إضافي لإزالة التكرار
            if (list.isNotEmpty()) list.distinctBy { "${it.folder}_${it.label}" } else null
        } else null

        if (sequence != null && sequence.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                signInfo = null,
                signSequence = sequence,
                errorMessage = null
            )
            saveToHistory(text)
            
            // Speak the text if sound is enabled
            val enableSound = prefs.getBoolean("enable_sound", true)
            if (enableSound) {
                speakText(text)
            }
        } else {
            _uiState.value = _uiState.value.copy(
                signInfo = null,
                signSequence = null,
                errorMessage = "لم يتم العثور على إشارة مطابقة للنص المدخل. تأكد من إدخال حروف عربية صحيحة."
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


