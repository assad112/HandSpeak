package com.example.handspeak.ui.screen.learn

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.handspeak.util.ImageHelper
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
    val folder: String,
    val imagePaths: List<String>
)

class LearnViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(LearnUiState())
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()
    
    private var allSigns: Map<String, com.example.handspeak.data.model.SignInfo> = emptyMap()
    
    init {
        loadSignMap()
    }
    
    private fun loadSignMap() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                allSigns = JsonHelper.loadSignMap(getApplication()) ?: emptyMap()
                Log.d("LearnViewModel", "Loaded ${allSigns.size} signs from sign_map.json")
                
                if (allSigns.isEmpty()) {
                    Log.w("LearnViewModel", "sign_map.json is empty or failed to load")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "لا توجد بيانات متاحة. تأكد من وجود sign_map.json في assets"
                    )
                    return@launch
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                
                // عرض جميع الإشارات المتاحة بعد التحميل
                performSearch("")
            } catch (e: Exception) {
                Log.e("LearnViewModel", "Error loading sign map", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "خطأ في تحميل البيانات: ${e.message}"
                )
            }
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
        // إعادة البحث عند تغيير الفئة
        performSearch(_uiState.value.searchQuery)
    }
    
    private fun performSearch(query: String) {
        viewModelScope.launch {
            try {
                if (allSigns.isEmpty()) {
                    Log.w("LearnViewModel", "allSigns is empty, cannot perform search")
                    _uiState.value = _uiState.value.copy(
                        searchResults = emptyList(),
                        errorMessage = null
                    )
                    return@launch
                }
                
                val results = mutableListOf<SignSearchResult>()
                val searchTerm = query.trim().lowercase()
                val selectedCategory = _uiState.value.selectedCategory
                
                Log.d("LearnViewModel", "Performing search: query='$searchTerm', category='$selectedCategory', total signs=${allSigns.size}")
                
                allSigns.forEach { (key, info) ->
                    val label = info.label.ifEmpty { key }
                    val folder = info.folder ?: key
                    val signType = if (label.length == 1) "letter" else "word"
                    
                    // فلترة حسب الفئة المختارة
                    val matchesCategory = when (selectedCategory) {
                        "letters" -> signType == "letter"
                        "words" -> signType == "word"
                        else -> true
                    }
                    
                    // فلترة حسب البحث (case-insensitive)
                    val matchesSearch = searchTerm.isEmpty() ||
                        label.lowercase().contains(searchTerm) ||
                        key.lowercase().contains(searchTerm)
                    
                    if (matchesCategory && matchesSearch) {
                        val imagePaths = getImagePathsForSign(folder)
                        if (imagePaths.isEmpty()) {
                            Log.d("LearnViewModel", "No images found for folder: $folder (label: $label)")
                        }
                        results.add(
                            SignSearchResult(
                                label = label,
                                type = signType,
                                folder = folder,
                                imagePaths = imagePaths
                            )
                        )
                    }
                }
                
                // ترتيب النتائج: الحروف أولاً ثم الكلمات
                val sortedResults = results.sortedWith(
                    compareBy<SignSearchResult> { it.type == "word" }
                        .thenBy { it.label }
                )
                
                Log.d("LearnViewModel", "Search completed: found ${sortedResults.size} results")
                
                _uiState.value = _uiState.value.copy(
                    searchResults = sortedResults,
                    errorMessage = null
                )
            } catch (e: Exception) {
                Log.e("LearnViewModel", "Error performing search", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "خطأ في البحث: ${e.message}"
                )
            }
        }
    }
    
    private fun getImagePathsForSign(folder: String): List<String> {
        val context = getApplication<Application>()
        
        // استخدام ImageHelper للحصول على قائمة الصور بشكل صحيح
        // هذا يحل مشكلة case-insensitive ويستخدم نفس المنطق المستخدم في باقي التطبيق
        return try {
            val imagePaths = ImageHelper.getImagePaths(context, folder)
            Log.d("LearnViewModel", "Found ${imagePaths.size} images for folder: $folder")
            imagePaths
        } catch (e: Exception) {
            Log.e("LearnViewModel", "Error getting image paths for folder: $folder", e)
            emptyList()
        }
    }
}





