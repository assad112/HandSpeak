package com.example.handspeak.ui.screen.learn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.handspeak.util.JsonHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LearnUiState(
    val searchQuery: String = "",
    val searchResults: List<SignSearchResult> = emptyList(),
    val selectedCategory: String = "all", // "all", "letters", "words"
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class SignSearchResult(
    val label: String,
    val type: String, // "letter" or "word"
    val imagePaths: List<String>
)

class LearnViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(LearnUiState())
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()
    
    private var allSigns: Map<String, Any> = emptyMap()
    
    init {
        loadSignMap()
        loadInitialResults()
    }
    
    private fun loadSignMap() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                allSigns = JsonHelper.loadSignMap(getApplication()) ?: emptyMap()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "خطأ في تحميل البيانات: ${e.message}"
                )
            }
        }
    }
    
    private fun loadInitialResults() {
        viewModelScope.launch {
            // عرض جميع الإشارات المتاحة عند البداية
            performSearch("")
        }
    }
    
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        performSearch(query)
    }
    
    fun clearSearch() {
        _uiState.value = _uiState.value.copy(searchQuery = "")
        performSearch("")
    }
    
    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        performSearch(_uiState.value.searchQuery)
    }
    
    private fun performSearch(query: String) {
        viewModelScope.launch {
            try {
                val results = mutableListOf<SignSearchResult>()
                val searchTerm = query.trim()
                val selectedCategory = _uiState.value.selectedCategory
                
                allSigns.forEach { (key, value) ->
                    if (value is Map<*, *>) {
                        val label = value["label"] as? String ?: key
                        val type = value["type"] as? String ?: "images"
                        val folder = value["folder"] as? String ?: key
                        
                        // تحديد نوع الإشارة (حرف أو كلمة)
                        val signType = if (label.length == 1) "letter" else "word"
                        
                        // فلترة حسب الفئة المختارة
                        val matchesCategory = when (selectedCategory) {
                            "letters" -> signType == "letter"
                            "words" -> signType == "word"
                            else -> true
                        }
                        
                        // فلترة حسب البحث
                        val matchesSearch = searchTerm.isEmpty() || 
                                          label.contains(searchTerm) || 
                                          key.contains(searchTerm, ignoreCase = true)
                        
                        if (matchesCategory && matchesSearch) {
                            // الحصول على مسارات الصور
                            val imagePaths = getImagePathsForSign(folder)
                            
                            if (imagePaths.isNotEmpty()) {
                                results.add(
                                    SignSearchResult(
                                        label = label,
                                        type = signType,
                                        imagePaths = imagePaths
                                    )
                                )
                            }
                        }
                    }
                }
                
                // ترتيب النتائج: الحروف أولاً ثم الكلمات
                val sortedResults = results.sortedWith(
                    compareBy<SignSearchResult> { it.type == "word" }
                        .thenBy { it.label }
                )
                
                _uiState.value = _uiState.value.copy(
                    searchResults = sortedResults,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "خطأ في البحث: ${e.message}"
                )
            }
        }
    }
    
    private fun getImagePathsForSign(folder: String): List<String> {
        val imagePaths = mutableListOf<String>()
        val context = getApplication<Application>()
        
        try {
            // محاولة قراءة الصور من مجلد signs
            val assetManager = context.assets
            val signFolder = "signs/$folder"
            
            try {
                val files = assetManager.list(signFolder)
                if (files != null && files.isNotEmpty()) {
                    files.filter { file ->
                        file.endsWith(".jpg", ignoreCase = true) ||
                        file.endsWith(".jpeg", ignoreCase = true) ||
                        file.endsWith(".png", ignoreCase = true)
                    }.sorted().forEach { file ->
                        imagePaths.add("$signFolder/$file")
                    }
                }
            } catch (e: Exception) {
                // المجلد غير موجود، جرب بدون "signs/"
                try {
                    val files = assetManager.list(folder)
                    if (files != null && files.isNotEmpty()) {
                        files.filter { file ->
                            file.endsWith(".jpg", ignoreCase = true) ||
                            file.endsWith(".jpeg", ignoreCase = true) ||
                            file.endsWith(".png", ignoreCase = true)
                        }.sorted().forEach { file ->
                            imagePaths.add("$folder/$file")
                        }
                    }
                } catch (e2: Exception) {
                    // لا توجد صور
                }
            }
        } catch (e: Exception) {
            // خطأ في الوصول للملفات
        }
        
        return imagePaths
    }
}

