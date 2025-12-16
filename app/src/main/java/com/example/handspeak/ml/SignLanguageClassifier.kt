package com.example.handspeak.ml

import android.content.Context
import android.util.Log
import com.example.handspeak.util.JsonHelper
import com.example.handspeak.util.LabelEncoder
import org.tensorflow.lite.Interpreter
// GPU delegate temporarily disabled
// import org.tensorflow.lite.gpu.CompatibilityList
// import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class SignLanguageClassifier(private val context: Context) {
    
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var labelEncoder: LabelEncoder? = null
    // GPU delegate temporarily disabled
    // private val gpuDelegate: GpuDelegate?
    
    companion object {
        private const val TAG = "SignLanguageClassifier"
        // يمكن استخدام Dense أو LSTM - المهم هو المدخلات والمخرجات
        private const val MODEL_NAME = "arabic_sign_lstm.tflite"  // يمكن تغييره إلى arabic_sign_dense.tflite
        private const val NUM_LANDMARKS = 21
        private const val COORDINATES_PER_LANDMARK = 3 // x, y, z
        private const val INPUT_SIZE = NUM_LANDMARKS * COORDINATES_PER_LANDMARK // 63
        private const val DEFAULT_SEQUENCE_LENGTH = 10 // طول التسلسل للـ LSTM
        
        // البنية المعمارية المدعومة:
        // - Dense NN: 256 → 128 → 64 → 28 (مع Dropout) - Input: [63]
        // - LSTM: للبيانات الزمنية - Input: [sequence_length, 63] مثل [10, 63]
        // المهم: Output=28
    }
    
    init {
        Log.d(TAG, "🚀 بدء تهيئة SignLanguageClassifier...")
        
        // Load labels
        try {
            labels = JsonHelper.loadLabels(context)
            Log.d(TAG, "✅ تم تحميل labels: ${labels.size} تصنيف")
        } catch (e: Exception) {
            Log.e(TAG, "❌ فشل تحميل labels: ${e.message}", e)
            labels = emptyList()
        }
        
        if (labels.isEmpty()) {
            Log.e(TAG, "❌ قائمة labels فارغة!")
        }
        
        // Initialize LabelEncoder (equivalent to scikit-learn LabelEncoder from Colab)
        labelEncoder = LabelEncoder(labels)
        Log.d(TAG, "✅ LabelEncoder initialized with ${labels.size} labels")
        
        // GPU delegate temporarily disabled - using CPU only
        Log.d(TAG, "💻 Using CPU for inference (GPU disabled)")
        
        // Load model
        try {
            Log.d(TAG, "📦 محاولة تحميل النموذج: $MODEL_NAME")
            val model = loadModelFile()
            Log.d(TAG, "✅ تم تحميل ملف النموذج بنجاح")
            
            val options = Interpreter.Options().apply {
                setNumThreads(4)  // Multi-threaded CPU inference
                // Flex delegate will be automatically loaded if tensorflow-lite-select-tf-ops is included
                // The library is loaded automatically when the dependency is added
            }
            
            Log.d(TAG, "🔧 إنشاء Interpreter...")
            interpreter = Interpreter(model, options)
            Log.d(TAG, "✅ Model loaded successfully!")
            Log.d(TAG, "   Labels count: ${labels.size}")
            Log.d(TAG, "   Input size: $INPUT_SIZE")
            Log.d(TAG, "   Output size: ${labels.size}")
            Log.d(TAG, "✅ Interpreter جاهز للاستخدام!")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading model: ${e.message}", e)
            e.printStackTrace()
            // Check if it's a Select TF Ops error
            if (e.message?.contains("Select TensorFlow op") == true || 
                e.message?.contains("FlexTensorListReserve") == true) {
                Log.e(TAG, "❌ Model requires TensorFlow Select ops. " +
                        "Make sure 'org.tensorflow:tensorflow-lite-select-tf-ops' dependency is added to build.gradle.kts")
            }
            // Check for FULLY_CONNECTED version error
            if (e.message?.contains("FULLY_CONNECTED") == true || 
                e.message?.contains("version") == true) {
                Log.e(TAG, "❌ Model requires newer TensorFlow Lite version. " +
                        "Current version: 2.16.1. If error persists, try updating to 2.17.0 or later.")
            }
            // Model file not found is expected if you haven't added it yet
            if (e is java.io.FileNotFoundException) {
                Log.e(TAG, "❌ Model file not found: $MODEL_NAME")
                Log.e(TAG, "   Add 'arabic_sign_lstm.tflite' to app/src/main/assets/ folder.")
            }
            
            // طباعة تفاصيل الخطأ الكاملة
            Log.e(TAG, "❌ تفاصيل الخطأ الكاملة:", e)
            interpreter = null // تأكد من أن interpreter هو null
        }
        
        // التحقق النهائي
        if (interpreter == null) {
            Log.e(TAG, "❌❌❌ Interpreter is NULL - النموذج لم يتم تحميله!")
            Log.e(TAG, "   تحقق من:")
            Log.e(TAG, "   1. وجود الملف: app/src/main/assets/arabic_sign_lstm.tflite")
            Log.e(TAG, "   2. أن الملف غير مضغوط (noCompress في build.gradle.kts)")
            Log.e(TAG, "   3. Clean و Rebuild المشروع")
        } else {
            Log.d(TAG, "✅✅✅ Interpreter جاهز!")
        }
    }

    /**
     * إغلاق الـ Interpreter وتحرير الموارد
     */
    fun close() {
        try {
            interpreter?.close()
        } catch (_: Exception) {}
        interpreter = null
    }

    /**
     * إعادة تحميل labels من `assets/labels.json`
     */
    fun reloadLabels(): List<String> {
        return try {
            labels = JsonHelper.loadLabels(context)
            labelEncoder = LabelEncoder(labels)
            Log.d(TAG, "🔄 تم إعادة تحميل labels (${labels.size})")
            labels
        } catch (e: Exception) {
            Log.e(TAG, "❌ فشل إعادة تحميل labels: ${e.message}", e)
            labels
        }
    }

    /**
     * إعادة تحميل نموذج TFLite من `assets/arabic_sign_lstm.tflite`
     */
    fun reloadModel(): Boolean {
        return try {
            close()
            Log.d(TAG, "📦 إعادة تحميل النموذج: $MODEL_NAME")
            val model = loadModelFile()
            val options = Interpreter.Options().apply { setNumThreads(4) }
            interpreter = Interpreter(model, options)
            Log.d(TAG, "✅ تم إعادة تحميل النموذج بنجاح")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ فشل إعادة تحميل النموذج: ${e.message}", e)
            false
        }
    }
    
    private fun loadModelFile(): ByteBuffer {
        Log.d(TAG, "📂 محاولة تحميل ملف النموذج: $MODEL_NAME")
        
        // Prefer memory-mapped file descriptor (requires uncompressed asset)
        try {
            val fileDescriptor = context.assets.openFd(MODEL_NAME)
            val fileSize = fileDescriptor.declaredLength
            Log.d(TAG, "✅ تم فتح ملف النموذج - الحجم: ${fileSize / 1024} KB")
            
            FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                val fileChannel = inputStream.channel
                val startOffset = fileDescriptor.startOffset
                val declaredLength = fileDescriptor.declaredLength
                val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
                Log.d(TAG, "✅ تم تحميل النموذج في الذاكرة")
                return buffer
            }
        } catch (e: IOException) {
            // Fallback for compressed assets: stream into a direct ByteBuffer
            Log.w(TAG, "⚠️ Falling back to streaming model (asset likely compressed): ${e.message}")
            try {
                context.assets.open(MODEL_NAME).use { input ->
                    val bytes = input.readBytes()
                    Log.d(TAG, "✅ تم قراءة النموذج - الحجم: ${bytes.size / 1024} KB")
                    val buffer = ByteBuffer.allocateDirect(bytes.size)
                    buffer.order(ByteOrder.nativeOrder())
                    buffer.put(bytes)
                    buffer.rewind()
                    return buffer
                }
            } catch (e2: Exception) {
                Log.e(TAG, "❌ فشل تحميل النموذج: ${e2.message}", e2)
                throw e2
            }
        }
    }
    
    /**
     * تصنيف إطار واحد (Dense NN)
     * @param landmarks FloatArray[63] - إطار واحد
     */
    fun classify(landmarks: FloatArray): Pair<String, Float>? {
        if (interpreter == null) {
            Log.e(TAG, "❌ Interpreter not initialized - النموذج غير محمّل")
            return null
        }
        
        if (labels.isEmpty()) {
            Log.e(TAG, "❌ Labels list is empty - لا توجد تصنيفات محمّلة")
            return null
        }
        
        if (landmarks.size != INPUT_SIZE) {
            Log.e(TAG, "❌ Invalid landmarks size: ${landmarks.size}, expected: $INPUT_SIZE")
            return null
        }
        
        try {
            Log.d(TAG, "🔍 بدء التصنيف - landmarks: ${landmarks.size}, labels: ${labels.size}")
            
            // Prepare input
            val inputBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE).apply {
                order(ByteOrder.nativeOrder())
                landmarks.forEach { putFloat(it) }
                rewind()
            }
            
            // Prepare output
            val outputArray = Array(1) { FloatArray(labels.size) }
            
            // Run inference
            Log.d(TAG, "🚀 تشغيل النموذج...")
            interpreter?.run(inputBuffer, outputArray)
            Log.d(TAG, "✅ تم تشغيل النموذج بنجاح")
            
            // Get prediction with highest confidence
            val probabilities = outputArray[0]
            
            // التحقق من أن probabilities صالحة
            if (probabilities.isEmpty()) {
                Log.e(TAG, "❌ Output array is empty")
                return null
            }
            
            val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
            val confidence = probabilities[maxIndex]
            
            // طباعة أعلى 3 تنبؤات للمساعدة في التشخيص
            val top3 = probabilities.indices
                .sortedByDescending { probabilities[it] }
                .take(3)
                .map { idx -> 
                    val lbl = if (idx < labels.size) labels[idx] else "?"
                    "$lbl(${(probabilities[idx] * 100).toInt()}%)"
                }
            Log.d(TAG, "📊 Top 3 predictions: ${top3.joinToString(", ")}")
            
            // Use LabelEncoder to decode index to label
            val label = labelEncoder?.decode(maxIndex) ?: run {
                Log.w(TAG, "⚠️ Failed to decode index $maxIndex, using direct access")
                if (maxIndex < labels.size) {
                    labels[maxIndex]
                } else {
                    Log.e(TAG, "❌ Index $maxIndex out of bounds (labels size: ${labels.size})")
                    return null
                }
            }
            
            if (label.isEmpty()) {
                Log.e(TAG, "❌ Label is empty for index $maxIndex")
                return null
            }
            
            Log.d(TAG, "✅ Predicted: index=$maxIndex → label='$label' with confidence=${(confidence*100).toInt()}%")
            
            // إرجاع النتيجة حتى لو كانت الثقة منخفضة (للنموذج التجريبي)
            return Pair(label, confidence)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during classification: ${e.message}", e)
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * تصنيف تسلسل إطارات (LSTM)
     * @param sequence Array<FloatArray> - تسلسل إطارات، كل إطار FloatArray[63]
     * @param sequenceLength طول التسلسل (افتراضي 10)
     */
    fun classifySequence(sequence: List<FloatArray>, sequenceLength: Int = DEFAULT_SEQUENCE_LENGTH): Pair<String, Float>? {
        if (interpreter == null) {
            Log.e(TAG, "Interpreter not initialized")
            return null
        }
        
        if (sequence.isEmpty()) {
            Log.e(TAG, "Empty sequence")
            return null
        }
        
        // التحقق من صحة كل إطار
        sequence.forEach { frame ->
            if (frame.size != INPUT_SIZE) {
                Log.e(TAG, "Invalid frame size: ${frame.size}, expected: $INPUT_SIZE")
                return null
            }
        }
        
        try {
            // إذا كان التسلسل أقل من المطلوب، نكرر آخر إطار
            val paddedSequence = if (sequence.size < sequenceLength) {
                val lastFrame = sequence.last()
                sequence + List(sequenceLength - sequence.size) { lastFrame.copyOf() }
            } else {
                sequence.takeLast(sequenceLength) // نأخذ آخر sequenceLength إطار
            }
            
            // Prepare input buffer: [sequence_length, 63]
            val inputBuffer = ByteBuffer.allocateDirect(4 * sequenceLength * INPUT_SIZE).apply {
                order(ByteOrder.nativeOrder())
                paddedSequence.forEach { frame ->
                    frame.forEach { value ->
                        putFloat(value)
                    }
                }
                rewind()
            }
            
            // Prepare output
            val outputArray = Array(1) { FloatArray(labels.size) }
            
            // Run inference
            interpreter?.run(inputBuffer, outputArray)
            
            // Get prediction with highest confidence
            val probabilities = outputArray[0]
            val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
            val confidence = probabilities[maxIndex]
            
            // طباعة أعلى 5 تنبؤات
            val top5 = probabilities.indices
                .sortedByDescending { probabilities[it] }
                .take(5)
                .map { idx -> "${if (idx < labels.size) labels[idx] else "?"} (${(probabilities[idx] * 100).toInt()}%)" }
            Log.d(TAG, "LSTM Top 5 predictions: ${top5.joinToString(", ")}")
            
            // Use LabelEncoder to decode index to label
            val label = labelEncoder?.decode(maxIndex) ?: run {
                Log.w(TAG, "Failed to decode index $maxIndex, using direct access")
                if (maxIndex < labels.size) labels[maxIndex] else ""
            }
            
            Log.d(TAG, "LSTM Predicted: $label with confidence: $confidence (sequence length: ${paddedSequence.size})")
            
            return Pair(label, confidence)
        } catch (e: Exception) {
            Log.e(TAG, "Error during sequence classification", e)
            // Fallback to single frame classification
            Log.d(TAG, "Falling back to single frame classification")
            return classify(sequence.lastOrNull() ?: return null)
        }
    }
    
}

