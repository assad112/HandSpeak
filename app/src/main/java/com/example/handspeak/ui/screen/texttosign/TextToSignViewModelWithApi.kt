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
import com.example.handspeak.data.repository.SignLanguageRepository
import com.example.handspeak.util.JsonHelper
import com.example.handspeak.util.TextToSpeechHelper
import com.example.handspeak.util.ImageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * مثال على استخدام API في ViewModel
 * 
 * هذا الملف يوضح كيفية دمج API مع البيانات المحلية
 */
class TextToSignViewModelWithApi(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(TextToSignUiState())
    val uiState: StateFlow<TextToSignUiState> = _uiState.asStateFlow()
    
    // البيانات المحلية
    private val signMap: Map<String, SignInfo> = JsonHelper.loadSignMap(application)
    private val repository: HistoryRepository
    private val ttsHelper: TextToSpeechHelper
    
    // API Repository
    private val apiRepository = SignLanguageRepository(application)
    
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
    
    /**
     * ترجمة النص إلى إشارة مع محاولة جلبها من API أولاً
     */
    fun translateToSign() {
        val text = _uiState.value.inputText.trim()
        
        if (text.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "الرجاء إدخال نص للترجمة"
            )
            return
        }
        
        // تحديث الحالة: جاري التحميل
        _uiState.value = _uiState.value.copy(
            errorMessage = "جاري البحث..."
        )
        
        viewModelScope.launch {
            try {
                // 1) محاولة جلب الإشارة من API أولاً
                val apiSignInfo = apiRepository.getSignInfoFromApi(text)
                
                if (apiSignInfo != null && apiSignInfo.folder != null) {
                    // تم العثور على الإشارة في API
                    Log.d("TextToSignViewModel", "Found sign in API: ${apiSignInfo.label}")
                    
                    // تحميل الصور من الإنترنت إذا كانت متاحة
                    val imageCount = apiRepository.downloadSignImages(apiSignInfo.folder)
                    
                    if (imageCount > 0) {
                        // تحديث الحالة مع الإشارة من API
                        _uiState.value = _uiState.value.copy(
                            signInfo = apiSignInfo,
                            signSequence = null,
                            errorMessage = null
                        )
                        saveToHistory(text)
                        
                        // تشغيل الصوت
                        val enableSound = prefs.getBoolean("enable_sound", true)
                        if (enableSound) {
                            speakText(text)
                        }
                        return@launch
                    }
                }
                
                // 2) إذا لم تُوجد في API، استخدم البيانات المحلية
                translateFromLocalData(text)
                
            } catch (e: Exception) {
                Log.e("TextToSignViewModel", "Error fetching from API, using local data", e)
                // في حالة فشل API، استخدم البيانات المحلية
                translateFromLocalData(text)
            }
        }
    }
    
    /**
     * ترجمة من البيانات المحلية (الطريقة الأصلية)
     */
    private fun translateFromLocalData(text: String) {
        // Always split text into unique characters to show individual sign images without duplicates
        val sequence = if (text.isNotEmpty()) {
            val list = mutableListOf<SignInfo>()
            val seenChars = mutableSetOf<String>() // لتجنب تكرار الأحرف
            text.forEach { ch ->
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
                        Log.w("TextToSignViewModel", "Character not found: '$key'")
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
            
            val enableSound = prefs.getBoolean("enable_sound", true)
            if (enableSound) {
                speakText(text)
            }
        } else {
            _uiState.value = _uiState.value.copy(
                signInfo = null,
                signSequence = null,
                errorMessage = "لم يتم العثور على إشارة مطابقة للنص المدخل."
            )
        }
    }
    
    /**
     * تحميل صور إشارة من API
     */
    fun downloadSignImages(folder: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "جاري تحميل الصور..."
                )
                
                val count = apiRepository.downloadSignImages(folder)
                
                if (count > 0) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "تم تحميل $count صورة بنجاح"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "لم يتم العثور على صور للتحميل"
                    )
                }
            } catch (e: Exception) {
                Log.e("TextToSignViewModel", "Error downloading images", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "فشل تحميل الصور: ${e.message}"
                )
            }
        }
    }
    
    /**
     * البحث عن إشارات في API
     */
    fun searchSigns(query: String) {
        viewModelScope.launch {
            try {
                val results = apiRepository.searchSigns(query)
                
                if (results.isNotEmpty()) {
                    // عرض النتائج (يمكن إضافة UI خاص بالنتائج)
                    Log.d("TextToSignViewModel", "Found ${results.size} signs")
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "تم العثور على ${results.size} إشارة"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "لم يتم العثور على نتائج"
                    )
                }
            } catch (e: Exception) {
                Log.e("TextToSignViewModel", "Error searching signs", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "فشل البحث: ${e.message}"
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




