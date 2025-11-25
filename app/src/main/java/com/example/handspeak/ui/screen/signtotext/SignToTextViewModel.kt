package com.example.handspeak.ui.screen.signtotext

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.handspeak.data.database.AppDatabase
import com.example.handspeak.data.database.HistoryEntity
import com.example.handspeak.data.repository.HistoryRepository
import com.example.handspeak.ml.AdaptiveLearningHelper
import com.example.handspeak.ml.HandDetectionHelper
import com.example.handspeak.ml.SignLanguageClassifier
import com.example.handspeak.util.TextToSpeechHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SignToTextUiState(
    val detectedText: String = "",
    val accumulatedText: String = "",
    val confidence: Float = 0f,
    val isProcessing: Boolean = false,
    val isHandDetected: Boolean = false,
    val errorMessage: String? = null,
    val sequenceBufferSize: Int = 0, // عدد الإطارات في Buffer
    val useLSTM: Boolean = true // استخدام LSTM أو Dense
)

class SignToTextViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(SignToTextUiState())
    val uiState: StateFlow<SignToTextUiState> = _uiState.asStateFlow()
    
    private val classifier: SignLanguageClassifier?
    private val handDetectionHelper: HandDetectionHelper?
    private val ttsHelper: TextToSpeechHelper
    private val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    // LSTM Frame Buffer - لتجميع عدة إطارات قبل التصنيف
    private val frameBuffer = mutableListOf<FloatArray>()
    private val SEQUENCE_LENGTH = 10 // طول التسلسل للـ LSTM
    private val USE_LSTM = true // تفعيل LSTM
    
    init {
        // Initialize classifier (may fail if model not found - that's OK)
        classifier = try {
            SignLanguageClassifier(application)
        } catch (e: Exception) {
            Log.e("SignToTextViewModel", "Failed to initialize classifier", e)
            null
        }
        
        // Initialize hand detection (may fail on x86 emulators - that's OK)
        handDetectionHelper = try {
            HandDetectionHelper(
                context = application,
                onResults = { result ->
                    result?.let { processHandLandmarks(it.landmarks, it.confidence) }
                },
                onError = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error,
                        isProcessing = false
                    )
                }
            )
        } catch (e: Exception) {
            Log.e("SignToTextViewModel", "Failed to initialize hand detection", e)
            null
        }
        
        // Initialize Text-to-Speech
        ttsHelper = TextToSpeechHelper(application)
        ttsHelper.initialize { success ->
            if (success) {
                Log.d("SignToTextViewModel", "TTS initialized successfully")
            } else {
                Log.e("SignToTextViewModel", "TTS initialization failed")
            }
        }
        
        // Check if components are available
        if (classifier == null || handDetectionHelper == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "⚠️ Sign detection requires:\n" +
                        "• TensorFlow model (arabic_sign_lstm.tflite)\n" +
                        "• MediaPipe library (works on ARM devices)\n" +
                        "• Use a real Android device for full functionality"
            )
        }
    }
    
    private val repository: HistoryRepository
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = HistoryRepository(database.historyDao())
    }
    
    fun processFrame(bitmap: Bitmap) {
        // Skip if already processing or components not available
        if (_uiState.value.isProcessing || handDetectionHelper == null || classifier == null) {
            return
        }
        
        _uiState.value = _uiState.value.copy(isProcessing = true)
        
        viewModelScope.launch {
            try {
                // Detect hands in the frame
                val handResult = handDetectionHelper.detectHands(bitmap)
                
                if (handResult != null && handResult.landmarks.isNotEmpty()) {
                    // Hand detected - process landmarks and classify
                    _uiState.value = _uiState.value.copy(isHandDetected = true)
                    processHandLandmarks(handResult.landmarks, handResult.confidence)
                } else {
                    // No hand detected
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        detectedText = "",
                        confidence = 0f,
                        isHandDetected = false
                    )
                }
            } catch (e: Exception) {
                Log.e("SignToTextViewModel", "Error processing frame", e)
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    detectedText = "",
                    confidence = 0f
                )
            }
        }
    }
    
    private fun processHandLandmarks(landmarks: List<com.example.handspeak.data.model.HandLandmark>, @Suppress("UNUSED_PARAMETER") detectionConfidence: Float) {
        viewModelScope.launch {
            try {
                // Normalize landmarks
                val normalizedLandmarks = handDetectionHelper?.normalizeLandmarks(landmarks) 
                    ?: run {
                        _uiState.value = _uiState.value.copy(isProcessing = false)
                        return@launch
                    }
                
                // استخدام LSTM أو Dense حسب الإعداد
                val useLSTM = prefs.getBoolean("use_lstm", USE_LSTM)
                
                val result = if (useLSTM) {
                    // LSTM: جمع الإطارات في Buffer
                    frameBuffer.add(normalizedLandmarks)
                    
                    // تحديث UI بعرض حجم Buffer
                    _uiState.value = _uiState.value.copy(
                        sequenceBufferSize = frameBuffer.size,
                        useLSTM = true
                    )
                    
                    // إذا وصلنا للطول المطلوب، نصنّف التسلسل
                    if (frameBuffer.size >= SEQUENCE_LENGTH) {
                        val sequence = frameBuffer.toList()
                        frameBuffer.clear() // مسح Buffer بعد التصنيف
                        
                        classifier?.classifySequence(sequence, SEQUENCE_LENGTH)
                    } else {
                        // لم نصل بعد للطول المطلوب
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            detectedText = "جمع الإطارات... (${frameBuffer.size}/$SEQUENCE_LENGTH)"
                        )
                        return@launch
                    }
                } else {
                    // Dense: تصنيف إطار واحد مباشرة
                    _uiState.value = _uiState.value.copy(
                        sequenceBufferSize = 1,
                        useLSTM = false
                    )
                    classifier?.classify(normalizedLandmarks)
                }
                
                if (result != null) {
                    val (label, classificationConfidence) = result
                    
                    // Use minimum confidence threshold (0.5 = 50%)
                    val minConfidence = 0.5f
                    
                    if (classificationConfidence >= minConfidence) {
                        _uiState.value = _uiState.value.copy(
                            detectedText = label,
                            confidence = classificationConfidence,
                            isProcessing = false,
                            sequenceBufferSize = if (useLSTM) 0 else 1
                        )
                        Log.d("SignToTextViewModel", "${if (useLSTM) "LSTM" else "Dense"} Detected: $label (${(classificationConfidence * 100).toInt()}%)")
                        
                        // حفظ عينة تدريب للتعلم التكيفي (إذا كان التعلم مفعّل)
                        val enableLearning = prefs.getBoolean("enable_adaptive_learning", true)
                        if (enableLearning && classificationConfidence >= 0.7f) {
                            // حفظ فقط إذا كانت الثقة عالية (≥70%)
                            launch {
                                val app = getApplication<Application>()
                                AdaptiveLearningHelper.saveTrainingSample(
                                    app,
                                    landmarks,
                                    label
                                )
                            }
                        }
                        
                        // Speak the detected text if sound is enabled
                        val enableSound = prefs.getBoolean("enable_sound", true)
                        if (enableSound) {
                            ttsHelper.speak(label, enableSound)
                        }
                    } else {
                        // Confidence too low
                        _uiState.value = _uiState.value.copy(
                            detectedText = "",
                            confidence = classificationConfidence,
                            isProcessing = false,
                            sequenceBufferSize = if (useLSTM) frameBuffer.size else 0
                        )
                    }
                } else {
                    // Classification failed
                    _uiState.value = _uiState.value.copy(
                        detectedText = "",
                        confidence = 0f,
                        isProcessing = false,
                        sequenceBufferSize = if (useLSTM) frameBuffer.size else 0
                    )
                }
            } catch (e: Exception) {
                Log.e("SignToTextViewModel", "Error classifying landmarks", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "خطأ في التصنيف: ${e.message}",
                    isProcessing = false
                )
            }
        }
    }
    
    /**
     * مسح Frame Buffer (مفيد عند تغيير الإشارة)
     */
    fun clearFrameBuffer() {
        frameBuffer.clear()
        _uiState.value = _uiState.value.copy(sequenceBufferSize = 0)
    }
    
    fun addToAccumulated() {
        val currentText = _uiState.value.detectedText
        if (currentText.isNotEmpty()) {
            val newAccumulated = _uiState.value.accumulatedText + currentText + " "
            _uiState.value = _uiState.value.copy(accumulatedText = newAccumulated)
        }
    }
    
    fun clearAccumulated() {
        _uiState.value = _uiState.value.copy(accumulatedText = "")
    }
    
    fun saveToHistory() {
        val text = _uiState.value.accumulatedText.trim()
        if (text.isEmpty()) return
        
        viewModelScope.launch {
            repository.insert(
                HistoryEntity(
                    text = text,
                    translationType = "sign_to_text",
                    confidence = _uiState.value.confidence
                )
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun speakDetectedText() {
        val text = _uiState.value.detectedText
        if (text.isNotEmpty()) {
            val enableSound = prefs.getBoolean("enable_sound", true)
            ttsHelper.speak(text, enableSound)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        classifier?.close()
        handDetectionHelper?.close()
        ttsHelper.shutdown()
    }
}

