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
        // Load labels
        labels = JsonHelper.loadLabels(context)
        
        // Initialize LabelEncoder (equivalent to scikit-learn LabelEncoder from Colab)
        labelEncoder = LabelEncoder(labels)
        Log.d(TAG, "LabelEncoder initialized with ${labels.size} labels")
        
        // GPU delegate temporarily disabled - using CPU only
        Log.d(TAG, "Using CPU for inference (GPU disabled)")
        
        // Load model
        try {
            val model = loadModelFile()
            val options = Interpreter.Options().apply {
                setNumThreads(4)  // Multi-threaded CPU inference
                // Flex delegate will be automatically loaded if tensorflow-lite-select-tf-ops is included
                // The library is loaded automatically when the dependency is added
            }
            interpreter = Interpreter(model, options)
            Log.d(TAG, "Model loaded successfully. Labels count: ${labels.size}")
            Log.d(TAG, "TensorFlow Lite Select TF Ops should be available (if dependency is added)")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: ${e.message}", e)
            // Check if it's a Select TF Ops error
            if (e.message?.contains("Select TensorFlow op") == true || 
                e.message?.contains("FlexTensorListReserve") == true) {
                Log.e(TAG, "Model requires TensorFlow Select ops. " +
                        "Make sure 'org.tensorflow:tensorflow-lite-select-tf-ops' dependency is added to build.gradle.kts")
            }
            // Model file not found is expected if you haven't added it yet
            if (e is java.io.FileNotFoundException) {
                Log.w(TAG, "Model file not found. Add 'arabic_sign_lstm.tflite' to assets folder.")
            }
        }
    }
    
    private fun loadModelFile(): ByteBuffer {
        // Prefer memory-mapped file descriptor (requires uncompressed asset)
        try {
            val fileDescriptor = context.assets.openFd(MODEL_NAME)
            FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                val fileChannel = inputStream.channel
                val startOffset = fileDescriptor.startOffset
                val declaredLength = fileDescriptor.declaredLength
                return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            }
        } catch (e: IOException) {
            // Fallback for compressed assets: stream into a direct ByteBuffer
            Log.w(TAG, "Falling back to streaming model (asset likely compressed): ${e.message}")
            context.assets.open(MODEL_NAME).use { input ->
                val bytes = input.readBytes()
                val buffer = ByteBuffer.allocateDirect(bytes.size)
                buffer.order(ByteOrder.nativeOrder())
                buffer.put(bytes)
                buffer.rewind()
                return buffer
            }
        }
    }
    
    /**
     * تصنيف إطار واحد (Dense NN)
     * @param landmarks FloatArray[63] - إطار واحد
     */
    fun classify(landmarks: FloatArray): Pair<String, Float>? {
        if (interpreter == null) {
            Log.e(TAG, "Interpreter not initialized")
            return null
        }
        
        if (landmarks.size != INPUT_SIZE) {
            Log.e(TAG, "Invalid landmarks size: ${landmarks.size}, expected: $INPUT_SIZE")
            return null
        }
        
        try {
            // Prepare input
            val inputBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE).apply {
                order(ByteOrder.nativeOrder())
                landmarks.forEach { putFloat(it) }
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
            
            // Use LabelEncoder to decode index to label (equivalent to label_encoder.inverse_transform in Colab)
            val label = labelEncoder?.decode(maxIndex) ?: run {
                Log.w(TAG, "Failed to decode index $maxIndex, using direct access")
                if (maxIndex < labels.size) labels[maxIndex] else ""
            }
            
            Log.d(TAG, "Predicted index: $maxIndex → label: $label with confidence: $confidence")
            
            return Pair(label, confidence)
        } catch (e: Exception) {
            Log.e(TAG, "Error during classification", e)
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
    
    fun close() {
        interpreter?.close()
        interpreter = null
        // gpuDelegate?.close()  // GPU disabled
    }
}

