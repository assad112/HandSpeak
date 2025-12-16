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
import com.example.handspeak.data.repository.SignLanguageRepository
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
    val useLSTM: Boolean = true, // استخدام LSTM أو Dense
    val currentLandmarks: List<com.example.handspeak.data.model.HandLandmark> = emptyList(), // Landmarks للعرض على الكاميرا
    val useFrontCamera: Boolean = true, // استخدام الكاميرا الأمامية أو الخلفية
    val useApiRecognition: Boolean = false, // استخدام API للتعرف بدلاً من النموذج المحلي
    val isLearningMode: Boolean = false, // وضع التعلم - لحفظ إشارات جديدة
    val learningLabel: String = "", // الحرف/الكلمة المراد تعلمها
    val learningSamplesCollected: Int = 0, // عدد العينات المجمعة
    val showLearningSavedMessage: Boolean = false // رسالة تأكيد الحفظ
)

class SignToTextViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(SignToTextUiState())
    val uiState: StateFlow<SignToTextUiState> = _uiState.asStateFlow()
    
    private var classifier: SignLanguageClassifier?
    private val handDetectionHelper: HandDetectionHelper?
    private val ttsHelper: TextToSpeechHelper
    private val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    // API Repository للتعرف على الحروف عبر الإنترنت
    private val apiRepository = SignLanguageRepository(application)
    
    // LSTM Frame Buffer - لتجميع عدة إطارات قبل التصنيف
    private val frameBuffer = mutableListOf<FloatArray>()
    private val SEQUENCE_LENGTH = 5 // طول التسلسل للـ LSTM - قصير للحروف الثابتة
    private val USE_LSTM_DEFAULT = false // القيمة الافتراضية - يمكن تغييرها من الإعدادات
    
    // تحسينات للتعرف على الحروف الثابتة - محسّن
    private var lastHandPosition: FloatArray? = null // لتتبع الحركة
    private var movementThreshold = 0.12f // حد الحركة (12%) - أكثر حساسية
    private var stableFrameCount = 0 // عدد الإطارات المستقرة
    private val MIN_STABLE_FRAMES = 2 // الحد الأدنى للإطارات المستقرة (أسرع)
    
    // تتبع الحركة والسرعة
    private val movementHistory = mutableListOf<Float>() // تاريخ الحركة
    private val MAX_MOVEMENT_HISTORY = 3 // عدد القياسات للتاريخ
    private var lastDetectionTime = 0L // وقت آخر كشف
    private val MIN_DETECTION_INTERVAL = 800L // 0.8 ثانية بين الكشوفات (أسرع)
    
    // نظام فلترة النتائج - تجنب النتائج المتكررة
    private var lastDetectedLabel: String? = null
    private var consecutiveSameDetections = 0
    private val MIN_SAME_DETECTIONS = 1 // تأكيد بكشف واحد (تقليل للاستجابة الأسرع)
    
    init {
        // Initialize classifier (may fail if model not found - that's OK)
        Log.d("SignToTextViewModel", "🚀 بدء تهيئة SignLanguageClassifier...")
        classifier = try {
            val cls = SignLanguageClassifier(application)
            Log.d("SignToTextViewModel", "✅ تم تهيئة Classifier بنجاح")
            cls
        } catch (e: Exception) {
            Log.e("SignToTextViewModel", "❌ Failed to initialize classifier: ${e.message}", e)
            e.printStackTrace()
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
        if (classifier == null) {
            Log.w("SignToTextViewModel", "Classifier is null - model may not be loaded")
            _uiState.value = _uiState.value.copy(
                errorMessage = "⚠️ النموذج غير متاح:\n" +
                        "• تأكد من وجود arabic_sign_lstm.tflite في assets/\n" +
                        "• تحقق من أن الملف غير مضغوط\n" +
                        "• أعد بناء التطبيق"
            )
        }
        
        if (handDetectionHelper == null) {
            Log.w("SignToTextViewModel", "HandDetectionHelper is null - MediaPipe may not be available")
            _uiState.value = _uiState.value.copy(
                errorMessage = "⚠️ كشف اليد غير متاح:\n" +
                        "• تأكد من وجود hand_landmarker.task في assets/\n" +
                        "• استخدم جهاز Android حقيقي (ARM)\n" +
                        "• MediaPipe لا يعمل على Emulator x86"
            )
        }
        
        if (classifier != null && handDetectionHelper != null) {
            Log.d("SignToTextViewModel", "✅ All components initialized successfully")
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }
    }
    
    private val repository: HistoryRepository
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = HistoryRepository(database.historyDao())
    }

    /**
     * إعادة تحميل النموذج وملف labels من مجلد الأصول
     * استخدمها بعد تحديث `arabic_sign_lstm.tflite` أو `labels.json`
     */
    fun reloadAssets() {
        viewModelScope.launch {
            try {
                Log.d("SignToTextViewModel", "🔄 بدء إعادة تحميل الأصول (model + labels)...")
                // إذا كان لدينا مصنف، أعد تحميله، وإلا أنشئ واحداً جديداً
                if (classifier == null) {
                    classifier = SignLanguageClassifier(getApplication())
                }
                // إعادة تحميل labels أولاً
                classifier?.reloadLabels()
                // إعادة تحميل النموذج
                val ok = classifier?.reloadModel() ?: false
                if (ok) {
                    Log.d("SignToTextViewModel", "✅ تم إعادة تحميل النموذج والملصقات بنجاح")
                    _uiState.value = _uiState.value.copy(errorMessage = null)
                } else {
                    Log.e("SignToTextViewModel", "❌ فشل إعادة تحميل النموذج")
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "❌ فشل إعادة تحميل النموذج. تأكد من وجود الملف في app/src/main/assets/"
                    )
                }
            } catch (e: Exception) {
                Log.e("SignToTextViewModel", "❌ خطأ أثناء إعادة التحميل: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "❌ خطأ أثناء إعادة التحميل: ${e.message}"
                )
            }
        }
    }
    
    // Frame skipping for performance - محسّن للاستجابة الأسرع
    private var frameSkipCounter = 0
    private val FRAME_SKIP_INTERVAL = 1 // معالجة كل إطار (استجابة أسرع)
    
    fun processFrame(bitmap: Bitmap) {
        // التحقق من توفر المكونات
        if (handDetectionHelper == null) {
            Log.w("SignToTextViewModel", "HandDetectionHelper is null - cannot process frame")
            _uiState.value = _uiState.value.copy(
                errorMessage = "⚠️ HandDetectionHelper غير متاح. تأكد من وجود hand_landmarker.task في assets"
            )
            return
        }
        
        if (classifier == null) {
            Log.e("SignToTextViewModel", "❌ Classifier is null - cannot classify")
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                errorMessage = "⚠️ النموذج غير متاح. تأكد من وجود arabic_sign_lstm.tflite في assets",
                detectedText = "⚠️ النموذج غير محمّل - تحقق من الملف"
            )
            return
        }
        
        // Skip if already processing
        if (_uiState.value.isProcessing) {
            return
        }
        
        // Frame skipping for better performance
        frameSkipCounter++
        if (frameSkipCounter % FRAME_SKIP_INTERVAL != 0) {
            return
        }
        
        _uiState.value = _uiState.value.copy(isProcessing = true)
        
        viewModelScope.launch {
            try {
                Log.d("SignToTextViewModel", "Processing frame: ${bitmap.width}x${bitmap.height}")
                
                // Detect hands in the frame
                val handResult = handDetectionHelper?.detectHands(bitmap)
                
                if (handResult != null && handResult.landmarks.isNotEmpty()) {
                    Log.d("SignToTextViewModel", "Hand detected! Landmarks: ${handResult.landmarks.size}, Confidence: ${handResult.confidence}")
                    
                    // Hand detected - process landmarks and classify
                    _uiState.value = _uiState.value.copy(
                        isHandDetected = true,
                        currentLandmarks = handResult.landmarks, // حفظ landmarks للعرض
                        errorMessage = null // مسح أي أخطاء سابقة
                    )
                    processHandLandmarks(handResult.landmarks, handResult.confidence)
                } else {
                    // No hand detected
                    Log.d("SignToTextViewModel", "No hand detected in frame")
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        detectedText = "",
                        confidence = 0f,
                        isHandDetected = false,
                        currentLandmarks = emptyList() // مسح landmarks
                    )
                }
            } catch (e: Exception) {
                Log.e("SignToTextViewModel", "Error processing frame", e)
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    detectedText = "",
                    confidence = 0f,
                    errorMessage = "خطأ في معالجة الإطار: ${e.message}"
                )
            }
        }
    }
    
    private fun processHandLandmarks(landmarks: List<com.example.handspeak.data.model.HandLandmark>, @Suppress("UNUSED_PARAMETER") detectionConfidence: Float) {
        viewModelScope.launch {
            try {
                // التحقق من استخدام API للتعرف
                val useApi = prefs.getBoolean("use_api_recognition", false)
                
                if (useApi) {
                    // استخدام API للتعرف على الحروف
                    recognizeWithApi(landmarks)
                    return@launch
                }
                
                // استخدام النموذج المحلي (الكود الأصلي)
                // Normalize landmarks
                val normalizedLandmarks = handDetectionHelper?.normalizeLandmarks(landmarks) 
                    ?: run {
                        Log.e("SignToTextViewModel", "❌ فشل في normalizeLandmarks")
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            detectedText = "⚠️ خطأ في معالجة landmarks"
                        )
                        return@launch
                    }
                
                Log.d("SignToTextViewModel", "✅ تم normalize landmarks: ${normalizedLandmarks.size} features")
                
                // استخدام LSTM أو Dense حسب الإعداد
                val useLSTM = prefs.getBoolean("use_lstm", USE_LSTM_DEFAULT)
                
                val result: Pair<String, Float>? = if (useLSTM) {
                    // LSTM: جمع الإطارات في Buffer مع تحسينات للحركات الديناميكية
                    
                    // حساب الحركة والسرعة
                    val movement = lastHandPosition?.let { lastPos ->
                        calculateMovement(normalizedLandmarks, lastPos)
                    } ?: 0f
                    
                    // إضافة الحركة إلى التاريخ
                    movementHistory.add(movement)
                    if (movementHistory.size > MAX_MOVEMENT_HISTORY) {
                        movementHistory.removeAt(0)
                    }
                    
                    // حساب متوسط الحركة (للكشف عن يد مستقرة)
                    val avgMovement = if (movementHistory.isNotEmpty()) {
                        movementHistory.average().toFloat()
                    } else 0f
                    
                    // كشف اليد المستقرة (للحروف الثابتة) - خوارزمية محسّنة
                    val isHandStable = movement <= movementThreshold && avgMovement <= movementThreshold
                    
                    // إضافة الإطارات عندما اليد مستقرة فقط
                    if (isHandStable) {
                        frameBuffer.add(normalizedLandmarks)
                        stableFrameCount++
                        lastHandPosition = normalizedLandmarks.copyOf()
                        
                        // عرض تقدم التسجيل للمستخدم
                        val progress = "${frameBuffer.size}/$SEQUENCE_LENGTH"
                        _uiState.value = _uiState.value.copy(
                            detectedText = "📸 جاري التسجيل... $progress",
                            sequenceBufferSize = frameBuffer.size
                        )
                        
                        Log.d("SignToTextViewModel", "✅ يد مستقرة: إطار $progress (حركة: ${(movement*100).toInt()}%)")
                    } else {
                        // اليد تتحرك - مسح Buffer
                        if (frameBuffer.isNotEmpty()) {
                            Log.d("SignToTextViewModel", "⚠️ حركة كثيرة - مسح Buffer (${(movement*100).toInt()}%)")
                            frameBuffer.clear()
                            _uiState.value = _uiState.value.copy(
                                detectedText = "✋ ثبّت يدك على شكل حرف",
                                sequenceBufferSize = 0
                            )
                        }
                        stableFrameCount = 0
                    }
                    
                    // تحديث UI بعرض حجم Buffer
                    _uiState.value = _uiState.value.copy(
                        sequenceBufferSize = frameBuffer.size,
                        useLSTM = true
                    )
                    
                    // تصنيف عند: وصول الطول المطلوب + اليد مستقرة
                    val shouldClassify = frameBuffer.size >= SEQUENCE_LENGTH && stableFrameCount >= MIN_STABLE_FRAMES
                    
                    if (shouldClassify) {
                        // التحقق من الوقت منذ آخر كشف (تجنب الكشف المتكرر)
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastDetectionTime < MIN_DETECTION_INTERVAL) {
                            Log.d("SignToTextViewModel", "تجاهل الكشف - وقت قصير جداً")
                            frameBuffer.clear()
                            _uiState.value = _uiState.value.copy(isProcessing = false)
                            null // إرجاع null لأننا لم نصنف
                        } else {
                            val sequence = frameBuffer.toList()
                            frameBuffer.clear() // مسح Buffer بعد التصنيف
                            lastHandPosition = null // مسح الموضع السابق
                            stableFrameCount = 0
                            movementHistory.clear() // مسح تاريخ الحركة
                            lastDetectionTime = currentTime
                            
                            Log.d("SignToTextViewModel", "تصنيف تسلسل بطول: ${sequence.size}")
                            val classificationResult = classifier?.classifySequence(sequence, SEQUENCE_LENGTH)
                            Log.d("SignToTextViewModel", "نتيجة التصنيف: $classificationResult (classifier=${classifier != null})")
                            classificationResult // إرجاع النتيجة
                        }
                    } else {
                        // لم نصل بعد للطول المطلوب
                        val statusMsg = if (frameBuffer.isNotEmpty()) {
                            "📸 تسجيل الحرف... (${frameBuffer.size}/$SEQUENCE_LENGTH)"
                        } else {
                            "✋ ثبّت يدك على شكل حرف"
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            detectedText = statusMsg
                        )
                        null // إرجاع null لأننا لم نصنف بعد
                    }
                } else {
                    // Dense: تصنيف إطار واحد مباشرة - محسّن للاستجابة السريعة
                    _uiState.value = _uiState.value.copy(
                        sequenceBufferSize = 1,
                        useLSTM = false
                    )
                    
                    // التحقق من الوقت منذ آخر كشف (تجنب الكشف المتكرر)
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastDetectionTime < MIN_DETECTION_INTERVAL) {
                        Log.d("SignToTextViewModel", "⏱️ تجاهل الكشف - وقت قصير جداً (${currentTime - lastDetectionTime}ms)")
                        _uiState.value = _uiState.value.copy(isProcessing = false)
                        null // إرجاع null لأننا لم نصنف
                    } else {
                        lastDetectionTime = currentTime
                        Log.d("SignToTextViewModel", "🔍 بدء التصنيف Dense - landmarks: ${normalizedLandmarks.size}")
                        
                        try {
                            val classificationResult = classifier?.classify(normalizedLandmarks)
                            if (classificationResult == null) {
                                Log.e("SignToTextViewModel", "❌ Classifier أعاد null - النموذج قد لا يعمل بشكل صحيح")
                                _uiState.value = _uiState.value.copy(
                                    isProcessing = false,
                                    detectedText = "⚠️ النموذج لا يعطي نتائج - جرب إشارة أخرى"
                                )
                                null // إرجاع null
                            } else {
                                Log.d("SignToTextViewModel", "✅ نتيجة التصنيف Dense: ${classificationResult.first} (ثقة: ${(classificationResult.second * 100).toInt()}%)")
                                classificationResult // إرجاع النتيجة
                            }
                        } catch (e: Exception) {
                            Log.e("SignToTextViewModel", "❌ خطأ في التصنيف: ${e.message}", e)
                            _uiState.value = _uiState.value.copy(
                                isProcessing = false,
                                detectedText = "⚠️ خطأ في التصنيف: ${e.message}"
                            )
                            null // إرجاع null
                        }
                    }
                }
                
                Log.d("SignToTextViewModel", "النتيجة النهائية: result=$result (useLSTM=$useLSTM)")
                
                if (result != null) {
                    val (label, classificationConfidence) = result
                    
                    // عتبة ثقة منخفضة جداً للنموذج التجريبي (5% فقط)
                    val minConfidence = 0.05f
                    
                    Log.d("SignToTextViewModel", "📊 النتيجة الخام: label='$label', confidence=${(classificationConfidence*100).toInt()}%, minConfidence=${(minConfidence*100).toInt()}%")
                    
                    // نظام فلترة - تأكيد بكشف واحد (مبسط جداً)
                    if (label == lastDetectedLabel) {
                        consecutiveSameDetections++
                    } else {
                        consecutiveSameDetections = 1
                        lastDetectedLabel = label
                    }
                    
                    Log.d("SignToTextViewModel", "🔍 كشف: $label (ثقة: ${(classificationConfidence*100).toInt()}%, تكرار: $consecutiveSameDetections/$MIN_SAME_DETECTIONS)")
                    
                    // التحقق من عتبة الثقة وتأكيد النتيجة
                    val isConfirmed = consecutiveSameDetections >= MIN_SAME_DETECTIONS
                    val meetsConfidence = classificationConfidence >= minConfidence
                    
                    Log.d("SignToTextViewModel", "✅ التحقق: isConfirmed=$isConfirmed, meetsConfidence=$meetsConfidence (${(classificationConfidence*100).toInt()}% >= ${(minConfidence*100).toInt()}%)")
                    
                    // قبول النتيجة حتى لو كانت الثقة منخفضة (للنموذج التجريبي)
                    // للنموذج التجريبي: اعرض النتيجة مباشرة حتى لو كانت الثقة منخفضة
                    val shouldAccept = meetsConfidence && isConfirmed
                    
                    if (shouldAccept || classificationConfidence >= 0.01f) {
                        // إعادة تعيين عداد التأكيد
                        consecutiveSameDetections = 0
                        lastDetectedLabel = null
                        
                        // تنظيف label (إزالة underscores واستبدالها بمسافات)
                        val cleanLabel = label.replace("_", " ")
                        
                        // إضافة تحذير إذا كانت الثقة منخفضة
                        val displayLabel = if (classificationConfidence < 0.1f) {
                            "⚠️ $cleanLabel (${(classificationConfidence * 100).toInt()}%)"
                        } else {
                            cleanLabel
                        }
                        
                        // تحديد نوع النتيجة: حرف أم كلمة
                        val isWord = cleanLabel.length > 1 || cleanLabel.contains(" ") || 
                                    cleanLabel in listOf("أحبك", "مرحباً", "شجرة", "بطة", "قطة", "هاتف", 
                                                         "موافق", "أنت", "لا", "سؤال", "اقتباس")
                        
                        // إضافة النتيجة للنص المتراكم
                        val currentAccumulated = _uiState.value.accumulatedText.trim()
                        val newAccumulated = if (currentAccumulated.isEmpty()) {
                            // إذا كان النص فارغاً، أضف النتيجة مباشرة
                            cleanLabel + if (isWord) " " else ""
                        } else if (isWord) {
                            // إذا كانت كلمة، أضفها مع مسافة قبلها
                            if (!currentAccumulated.endsWith(" ")) {
                                currentAccumulated + " " + cleanLabel + " "
                            } else {
                                currentAccumulated + cleanLabel + " "
                            }
                        } else {
                            // إذا كان حرف، أضفه مباشرة (بدون مسافة)
                            if (currentAccumulated.endsWith(cleanLabel)) {
                                currentAccumulated // تجنب التكرار
                            } else {
                                currentAccumulated + cleanLabel
                            }
                        }
                        
                        // تحديد نوع النتيجة للعرض
                        val resultType = if (isWord) "كلمة" else "حرف"
                        
                        _uiState.value = _uiState.value.copy(
                            detectedText = displayLabel,
                            accumulatedText = newAccumulated.trim(),
                            confidence = classificationConfidence,
                            isProcessing = false,
                            sequenceBufferSize = if (useLSTM) 0 else 1,
                            currentLandmarks = landmarks // حفظ landmarks للاستخدام مع زر التأكيد
                        )
                        Log.d("SignToTextViewModel", "${if (useLSTM) "LSTM" else "Dense"} ✅ Detected $resultType: $cleanLabel (${(classificationConfidence * 100).toInt()}%) - Accumulated: $newAccumulated")
                        
                        // حفظ تلقائي للنص المتراكم في التاريخ
                        val autoSave = prefs.getBoolean("auto_save_text", true)
                        if (autoSave && newAccumulated.trim().isNotEmpty()) {
                            launch {
                                val textToSave = newAccumulated.trim()
                                if (textToSave.isNotEmpty()) {
                                    repository.insert(
                                        HistoryEntity(
                                            text = textToSave,
                                            translationType = "sign_to_text",
                                            confidence = classificationConfidence
                                        )
                                    )
                                    Log.d("SignToTextViewModel", "Auto-saved text to history: $textToSave")
                                }
                            }
                        }
                        
                        // الحفظ اليدوي فقط - تم تعطيل الحفظ التلقائي
                        
                        // Speak the detected text if sound is enabled
                        val enableSound = prefs.getBoolean("enable_sound", true)
                        if (enableSound) {
                            ttsHelper.speak(cleanLabel, enableSound)
                        }
                    } else {
                        // عرض النتيجة حتى لو كانت الثقة منخفضة (للنموذج التجريبي)
                        val cleanLabel = label.replace("_", " ")
                        
                        // إذا كانت الثقة أعلى من 1%، اعرضها مع تحذير
                        if (classificationConfidence >= 0.01f) {
                            val message = when {
                                !isConfirmed -> "🔄 $cleanLabel ($consecutiveSameDetections/$MIN_SAME_DETECTIONS) - ثبّت يدك"
                                !meetsConfidence -> "⚠️ $cleanLabel (ثقة: ${(classificationConfidence * 100).toInt()}%)"
                                else -> "⏳ $cleanLabel"
                            }
                            
                            Log.w("SignToTextViewModel", "⚠️ عرض نتيجة بثقة منخفضة: confidence=${(classificationConfidence*100).toInt()}%, label='$label'")
                            _uiState.value = _uiState.value.copy(
                                detectedText = message,
                                confidence = classificationConfidence,
                                isProcessing = false,
                                sequenceBufferSize = if (useLSTM) frameBuffer.size else 0
                            )
                        } else {
                            // ثقة منخفضة جداً - لا تعرض
                            Log.w("SignToTextViewModel", "❌ ثقة منخفضة جداً: confidence=${(classificationConfidence*100).toInt()}%, label='$label'")
                            _uiState.value = _uiState.value.copy(
                                detectedText = "⏳ جاري التصنيف...",
                                confidence = classificationConfidence,
                                isProcessing = false,
                                sequenceBufferSize = if (useLSTM) frameBuffer.size else 0
                            )
                        }
                    }
                } else {
                    // Classification failed - لا توجد نتيجة
                    Log.w("SignToTextViewModel", "⚠️ التصنيف فشل - result is null")
                    
                    // إذا كان Classifier يعيد null باستمرار، قد يكون النموذج غير متوافق
                    val errorMsg = if (_uiState.value.isHandDetected) {
                        "⚠️ النموذج لا يعطي نتائج - جرب إشارة أخرى"
                    } else {
                        ""
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        detectedText = errorMsg,
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
     * حساب مقدار الحركة بين إطارين
     */
    private fun calculateMovement(current: FloatArray, previous: FloatArray): Float {
        if (current.size != previous.size) return 1f
        
        var totalMovement = 0f
        for (i in current.indices) {
            totalMovement += kotlin.math.abs(current[i] - previous[i])
        }
        return totalMovement / current.size // متوسط الحركة
    }
    
    /**
     * مسح Frame Buffer (مفيد عند تغيير الإشارة)
     * يتم مسح:
     * - frameBuffer: قائمة الإطارات المسجلة
     * - lastHandPosition: آخر موضع لليد
     * - stableFrameCount: عداد الإطارات المستقرة
     * - consecutiveSameDetections: عداد التأكيد
     */
    fun clearFrameBuffer() {
        frameBuffer.clear()
        lastHandPosition = null
        stableFrameCount = 0
        consecutiveSameDetections = 0
        lastDetectedLabel = null
        movementHistory.clear()
        _uiState.value = _uiState.value.copy(
            sequenceBufferSize = 0,
            detectedText = ""
        )
        Log.d("SignToTextViewModel", "🧹 تم مسح كل البيانات المؤقتة")
    }
    
    fun addToAccumulated() {
        val currentText = _uiState.value.detectedText
        if (currentText.isNotEmpty()) {
            val newAccumulated = _uiState.value.accumulatedText + currentText + " "
            _uiState.value = _uiState.value.copy(accumulatedText = newAccumulated)
        }
    }
    
    /**
     * إضافة حرف إلى النص المتراكم (للاستخدام مع API)
     */
    private fun addToAccumulatedText(letter: String) {
        val newAccumulated = _uiState.value.accumulatedText + letter
        _uiState.value = _uiState.value.copy(accumulatedText = newAccumulated)
    }
    
    /**
     * التعرف على الحروف باستخدام API
     */
    private suspend fun recognizeWithApi(landmarks: List<com.example.handspeak.data.model.HandLandmark>) {
        try {
            val useLSTM = prefs.getBoolean("use_lstm", USE_LSTM_DEFAULT)
            
            val recognitionResult = if (useLSTM && frameBuffer.size >= SEQUENCE_LENGTH) {
                // استخدام LSTM - إرسال تسلسل
                val sequence = frameBuffer.toList()
                frameBuffer.clear()
                apiRepository.recognizeLetterFromSequence(sequence)
            } else {
                // استخدام Dense - إرسال landmarks مباشرة
                apiRepository.recognizeLetterFromLandmarks(landmarks)
            }
            
            if (recognitionResult != null) {
                val letter = recognitionResult.letter
                val confidence = recognitionResult.confidence
                
                // استخدام حد أدنى للثقة
                val minConfidence = 0.5f
                
                if (confidence >= minConfidence) {
                    _uiState.value = _uiState.value.copy(
                        detectedText = letter,
                        confidence = confidence,
                        isProcessing = false,
                        useApiRecognition = true
                    )
                    
                    Log.d("SignToTextViewModel", "API Recognized: $letter (${(confidence * 100).toInt()}%)")
                    
                    // إضافة الحرف إلى النص المتراكم
                    addToAccumulatedText(letter)
                } else {
                    _uiState.value = _uiState.value.copy(
                        detectedText = "",
                        confidence = confidence,
                        isProcessing = false,
                        useApiRecognition = true
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = "فشل التعرف عبر API"
                )
            }
        } catch (e: Exception) {
            Log.e("SignToTextViewModel", "Error recognizing with API", e)
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                errorMessage = "خطأ في الاتصال بالـ API: ${e.message}"
            )
        }
    }
    
    /**
     * التعرف على الحروف من صورة مباشرة (بدون landmarks)
     */
    fun recognizeFromImage(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isProcessing = true)
                
                val recognitionResult = apiRepository.recognizeLetterFromImage(bitmap)
                
                if (recognitionResult != null) {
                    val letter = recognitionResult.letter
                    val confidence = recognitionResult.confidence
                    
                    if (confidence >= 0.5f) {
                        _uiState.value = _uiState.value.copy(
                            detectedText = letter,
                            confidence = confidence,
                            isProcessing = false,
                            useApiRecognition = true
                        )
                        
                        addToAccumulatedText(letter)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            detectedText = ""
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        errorMessage = "فشل التعرف على الحرف"
                    )
                }
            } catch (e: Exception) {
                Log.e("SignToTextViewModel", "Error recognizing from image", e)
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = "خطأ: ${e.message}"
                )
            }
        }
    }
    
    /**
     * تفعيل/إلغاء تفعيل API Recognition
     */
    fun setUseApiRecognition(useApi: Boolean) {
        prefs.edit().putBoolean("use_api_recognition", useApi).apply()
        _uiState.value = _uiState.value.copy(useApiRecognition = useApi)
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
    
    fun toggleCamera() {
        _uiState.value = _uiState.value.copy(
            useFrontCamera = !_uiState.value.useFrontCamera
        )
        // مسح landmarks عند تبديل الكاميرا
        _uiState.value = _uiState.value.copy(
            currentLandmarks = emptyList(),
            detectedText = "",
            isHandDetected = false
        )
    }
    
    /**
     * بدء وضع التعلم - لحفظ إشارة جديدة
     * @param label الحرف أو الكلمة المراد تعلمها
     */
    fun startLearningMode(label: String) {
        _uiState.value = _uiState.value.copy(
            isLearningMode = true,
            learningLabel = label,
            learningSamplesCollected = 0,
            detectedText = "📚 وضع التعلم: $label",
            showLearningSavedMessage = false
        )
        clearFrameBuffer()
        Log.d("SignToTextViewModel", "🎓 بدء وضع التعلم للحرف: $label")
    }
    
    /**
     * إيقاف وضع التعلم
     */
    fun stopLearningMode() {
        _uiState.value = _uiState.value.copy(
            isLearningMode = false,
            learningLabel = "",
            learningSamplesCollected = 0,
            detectedText = "",
            showLearningSavedMessage = false
        )
        clearFrameBuffer()
        Log.d("SignToTextViewModel", "🛑 إيقاف وضع التعلم")
    }
    
    /**
     * حفظ الإشارة الحالية كعينة تدريب
     */
    fun saveCurrentSignAsTrainingSample() {
        val landmarks = _uiState.value.currentLandmarks
        val label = _uiState.value.learningLabel
        
        if (landmarks.isEmpty() || label.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "⚠️ لا توجد إشارة لحفظها. ضع يدك على شكل الحرف أولاً."
            )
            return
        }
        
        viewModelScope.launch {
            try {
                val success = AdaptiveLearningHelper.saveTrainingSample(
                    getApplication(),
                    landmarks,
                    label
                )
                
                if (success) {
                    val samplesCount = _uiState.value.learningSamplesCollected + 1
                    _uiState.value = _uiState.value.copy(
                        learningSamplesCollected = samplesCount,
                        showLearningSavedMessage = true,
                        detectedText = "✅ تم حفظ عينة #$samplesCount للحرف: $label"
                    )
                    Log.d("SignToTextViewModel", "✅ تم حفظ عينة تدريب #$samplesCount للحرف: $label")
                    
                    // إخفاء الرسالة بعد 3 ثواني (وقت أطول للقراءة)
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(3000)
                        _uiState.value = _uiState.value.copy(
                            showLearningSavedMessage = false
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "❌ فشل حفظ العينة. ربما وصلت للحد الأقصى (100 عينة)."
                    )
                }
            } catch (e: Exception) {
                Log.e("SignToTextViewModel", "خطأ في حفظ العينة", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "❌ خطأ في حفظ العينة: ${e.message}"
                )
            }
        }
    }
    
    /**
     * تأكيد الإشارة المكتشفة كعينة صحيحة
     * (يستخدم عندما يكتشف التطبيق إشارة ويريد المستخدم تأكيدها)
     */
    fun confirmDetectedSignAsCorrect() {
        val landmarks = _uiState.value.currentLandmarks
        val label = _uiState.value.detectedText
            .replace("✅ تم كشف: ", "")
            .replace("⚠️ ثقة منخفضة: ", "")
            .replace("🔄 كشف: ", "")
            .split(" ")[0] // أول كلمة فقط
            .replace(Regex("\\(.*\\)"), "") // إزالة النسبة المئوية
            .trim()
        
        Log.d("SignToTextViewModel", "🔍 محاولة حفظ - عدد landmarks: ${landmarks.size}, النص: '$label'")
        
        if (landmarks.isEmpty()) {
            Log.e("SignToTextViewModel", "❌ خطأ: landmarks فارغة")
            _uiState.value = _uiState.value.copy(
                errorMessage = "⚠️ لا توجد نقاط يد محفوظة. قم بالكشف عن إشارة أولاً."
            )
            return
        }
        
        if (label.isEmpty() || label.startsWith("📸") || label.startsWith("✋")) {
            Log.e("SignToTextViewModel", "❌ خطأ: النص غير صالح: '$label'")
            _uiState.value = _uiState.value.copy(
                errorMessage = "⚠️ لا يوجد نص مكتشف لحفظه. قم بالكشف عن إشارة أولاً."
            )
            return
        }
        
        viewModelScope.launch {
            try {
                val success = AdaptiveLearningHelper.saveTrainingSample(
                    getApplication(),
                    landmarks,
                    label
                )
                
                if (success) {
                    val totalSamples = AdaptiveLearningHelper.getTotalSampleCount(getApplication())
                    _uiState.value = _uiState.value.copy(
                        detectedText = "✅ تم تأكيد وحفظ: $label",
                        showLearningSavedMessage = true,
                        learningSamplesCollected = totalSamples
                    )
                    Log.d("SignToTextViewModel", "✅ تم تأكيد وحفظ الإشارة: $label (إجمالي العينات: $totalSamples)")
                    
                    // إخفاء الرسالة بعد 3 ثواني (وقت أطول للقراءة)
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(3000)
                        _uiState.value = _uiState.value.copy(
                            showLearningSavedMessage = false
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "❌ فشل حفظ العينة."
                    )
                }
            } catch (e: Exception) {
                Log.e("SignToTextViewModel", "خطأ في تأكيد الإشارة", e)
            }
        }
    }
    
    /**
     * الحصول على إحصائيات التعلم
     */
    fun getLearningStats(): Map<String, Int> {
        return AdaptiveLearningHelper.getLearningStats(getApplication())
    }
    
    /**
     * الحصول على عدد العينات المحفوظة
     */
    fun getTotalLearningSamples(): Int {
        return AdaptiveLearningHelper.getTotalSampleCount(getApplication())
    }
    
    /**
     * حفظ الإشارة الحالية مع اسم مخصص
     * @param customLabel الاسم المخصص للإشارة
     */
    fun saveSignWithCustomLabel(customLabel: String) {
        val landmarks = _uiState.value.currentLandmarks
        val label = customLabel.trim()
        
        if (landmarks.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "⚠️ لا توجد إشارة لحفظها. ضع يدك أمام الكاميرا أولاً."
            )
            return
        }
        
        if (label.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "⚠️ يرجى إدخال اسم للإشارة."
            )
            return
        }
        
        viewModelScope.launch {
            try {
                val success = AdaptiveLearningHelper.saveTrainingSample(
                    getApplication(),
                    landmarks,
                    label
                )
                
                if (success) {
                    val totalSamples = AdaptiveLearningHelper.getTotalSampleCount(getApplication())
                    _uiState.value = _uiState.value.copy(
                        showLearningSavedMessage = true,
                        learningSamplesCollected = totalSamples,
                        errorMessage = null
                    )
                    Log.d("SignToTextViewModel", "✅ تم حفظ الإشارة مع الاسم: $label (إجمالي العينات: $totalSamples)")
                    
                    // إخفاء الرسالة بعد 3 ثواني
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(3000)
                        _uiState.value = _uiState.value.copy(
                            showLearningSavedMessage = false
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "❌ فشل حفظ العينة. ربما وصلت للحد الأقصى (100 عينة لكل تصنيف)."
                    )
                }
            } catch (e: Exception) {
                Log.e("SignToTextViewModel", "خطأ في حفظ الإشارة", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "❌ خطأ في حفظ العينة: ${e.message}"
                )
            }
        }
    }
    
    /**
     * التحقق من حالة التعلم
     */
    fun isLearningEnabled(): Boolean {
        return prefs.getBoolean("enable_adaptive_learning", true)
    }
    
    /**
     * تفعيل/تعطيل التعلم التلقائي
     */
    fun setLearningEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("enable_adaptive_learning", enabled).apply()
        Log.d("SignToTextViewModel", "🎓 التعلم التلقائي: ${if (enabled) "مفعّل ✅" else "معطّل ❌"}")
    }
    
    /**
     * الحصول على رسالة حالة التعلم
     */
    fun getLearningStatusMessage(): String {
        val isEnabled = isLearningEnabled()
        val totalSamples = getTotalLearningSamples()
        return if (isEnabled) {
            "✅ التعلم مفعّل | $totalSamples عينة محفوظة"
        } else {
            "❌ التعلم معطّل"
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        classifier?.close()
        handDetectionHelper?.close()
        ttsHelper.shutdown()
    }
}

