package com.example.handspeak.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.handspeak.data.model.HandDetectionResult
import com.example.handspeak.data.model.HandLandmark
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class HandDetectionHelper(
    private val context: Context,
    private val onResults: (HandDetectionResult?) -> Unit,
    private val onError: (String) -> Unit
) {
    
    private var handLandmarker: HandLandmarker? = null
    
    companion object {
        private const val TAG = "HandDetectionHelper"
        private const val MODEL_NAME = "hand_landmarker.task"
        private const val MIN_DETECTION_CONFIDENCE = 0.5f
        private const val MIN_TRACKING_CONFIDENCE = 0.5f
    }
    
    init {
        setupHandLandmarker()
    }
    
    private fun setupHandLandmarker() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_NAME)
                .build()
            
            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinHandDetectionConfidence(MIN_DETECTION_CONFIDENCE)
                .setMinTrackingConfidence(MIN_TRACKING_CONFIDENCE)
                .setNumHands(1)
                .setRunningMode(RunningMode.IMAGE)
                .build()
            
            handLandmarker = HandLandmarker.createFromOptions(context, options)
            Log.d(TAG, "HandLandmarker initialized successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "MediaPipe native library not found. This is expected on x86 emulators.", e)
            handLandmarker = null
            // Don't call onError - just log it
            Log.w(TAG, "Hand detection will not work. Use a real ARM device or add model files.")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up HandLandmarker", e)
            handLandmarker = null
            // Don't call onError to avoid crash
            Log.w(TAG, "Hand detection initialization failed: ${e.message}")
        }
    }
    
    fun detectHands(bitmap: Bitmap): HandDetectionResult? {
        if (handLandmarker == null) {
            Log.e(TAG, "HandLandmarker not initialized")
            return null
        }
        
        try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = handLandmarker?.detect(mpImage)
            
            return result?.let { processResult(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting hands", e)
            onError("Error detecting hands: ${e.message}")
            return null
        }
    }
    
    private fun processResult(result: HandLandmarkerResult): HandDetectionResult? {
        if (result.landmarks().isEmpty()) {
            return null
        }
        
        val landmarks = result.landmarks()[0].map {
            HandLandmark(it.x(), it.y(), it.z())
        }
        
        // Get confidence from handedness
        val confidence = result.handednesses()
            .firstOrNull()
            ?.firstOrNull()
            ?.score() ?: 0f
        
        return HandDetectionResult(landmarks, confidence)
    }
    
    /**
     * Normalize landmarks to 0-1 range
     * 
     * هذا يعادل Normalization المستخدم في Colab
     * 
     * المدخلات: List<HandLandmark> (21 landmark)
     * المخرجات: FloatArray[63] (21 × 3 coordinates)
     * 
     * العملية:
     * 1. تحويل إلى array: [x1, y1, z1, x2, y2, z2, ..., x21, y21, z21]
     * 2. Normalize x و y إلى 0-1 range
     * 3. z يبقى نسبي (كما هو)
     */
    fun normalizeLandmarks(landmarks: List<HandLandmark>): FloatArray {
        if (landmarks.size != 21) {
            Log.w(TAG, "Expected 21 landmarks, got ${landmarks.size}")
        }
        
        // Step 1: Convert to array [x1, y1, z1, x2, y2, z2, ..., x21, y21, z21]
        val array = FloatArray(landmarks.size * 3)
        landmarks.forEachIndexed { index, landmark ->
            array[index * 3] = landmark.x      // x coordinate
            array[index * 3 + 1] = landmark.y  // y coordinate
            array[index * 3 + 2] = landmark.z   // z coordinate
        }
        
        // Step 2: Find min/max for normalization
        val xValues = landmarks.map { it.x }
        val yValues = landmarks.map { it.y }
        
        val minX = xValues.minOrNull() ?: 0f
        val maxX = xValues.maxOrNull() ?: 1f
        val minY = yValues.minOrNull() ?: 0f
        val maxY = yValues.maxOrNull() ?: 1f
        
        // Step 3: Normalize x and y to 0-1 range (same as Colab)
        array.forEachIndexed { index, value ->
            when (index % 3) {
                0 -> { // x coordinate
                    array[index] = if (maxX - minX != 0f) {
                        (value - minX) / (maxX - minX)
                    } else {
                        0f
                    }
                }
                1 -> { // y coordinate
                    array[index] = if (maxY - minY != 0f) {
                        (value - minY) / (maxY - minY)
                    } else {
                        0f
                    }
                }
                2 -> { // z coordinate - stays as is (relative depth)
                    // z remains unchanged
                }
            }
        }
        
        Log.d(TAG, "Normalized ${landmarks.size} landmarks to 63 features")
        Log.d(TAG, "X range: [$minX, $maxX] → [0.0, 1.0]")
        Log.d(TAG, "Y range: [$minY, $maxY] → [0.0, 1.0]")
        
        return array  // FloatArray[63] ready for Dense NN
    }
    
    fun close() {
        handLandmarker?.close()
        handLandmarker = null
    }
}

