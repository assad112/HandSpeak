package com.example.handspeak.ml

import android.content.Context
import android.util.Log
import com.example.handspeak.data.model.HandLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * AdaptiveLearningHelper - نظام التعلم التكيفي
 * 
 * يجمع بيانات التدريب من المستخدم ويخزنها لإعادة التدريب
 * 
 * الوظائف:
 * 1. جمع بيانات التدريب (landmarks + label)
 * 2. حفظ البيانات في CSV
 * 3. تصدير البيانات للتدريب
 * 4. إحصائيات التعلم
 */
object AdaptiveLearningHelper {
    
    private const val TAG = "AdaptiveLearningHelper"
    private const val TRAINING_DATA_DIR = "training_data"
    private const val CSV_FILE_NAME = "user_training_data.csv"
    private const val MAX_SAMPLES_PER_LABEL = 100 // حد أقصى لكل تصنيف
    
    /**
     * يحصل على مجلد بيانات التدريب
     */
    private fun getTrainingDataDirectory(context: Context): File {
        val dir = File(context.filesDir, TRAINING_DATA_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * يحفظ عينة تدريب جديدة
     * 
     * @param context Context
     * @param landmarks 21 landmark من MediaPipe
     * @param label اسم التصنيف (مثل "أ", "مرحبا")
     * @return true إذا نجح الحفظ
     */
    suspend fun saveTrainingSample(
        context: Context,
        landmarks: List<HandLandmark>,
        label: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (landmarks.size != 21) {
                Log.w(TAG, "Invalid landmarks count: ${landmarks.size}, expected 21")
                return@withContext false
            }
            
            // تحقق من الحد الأقصى
            val currentCount = getSampleCountForLabel(context, label)
            if (currentCount >= MAX_SAMPLES_PER_LABEL) {
                Log.d(TAG, "Max samples reached for label: $label ($currentCount/$MAX_SAMPLES_PER_LABEL)")
                return@withContext false
            }
            
            val csvFile = File(getTrainingDataDirectory(context), CSV_FILE_NAME)
            val fileExists = csvFile.exists()
            
            FileWriter(csvFile, true).use { writer ->
                // كتابة Header إذا كان الملف جديد
                if (!fileExists) {
                    val header = buildHeader()
                    writer.append(header)
                    writer.append("\n")
                }
                
                // كتابة البيانات
                val row = buildDataRow(landmarks, label)
                writer.append(row)
                writer.append("\n")
            }
            
            Log.d(TAG, "Saved training sample for label: $label (Total: ${getSampleCountForLabel(context, label)})")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error saving training sample", e)
            false
        }
    }
    
    /**
     * يبني Header للـ CSV
     */
    private fun buildHeader(): String {
        val header = StringBuilder("label")
        
        // 21 landmark × 3 coordinates = 63 features
        for (i in 0 until 21) {
            header.append(",x$i,y$i,z$i")
        }
        
        header.append(",timestamp")
        return header.toString()
    }
    
    /**
     * يبني صف بيانات للـ CSV
     */
    private fun buildDataRow(landmarks: List<HandLandmark>, label: String): String {
        val row = StringBuilder(label)
        
        landmarks.forEach { landmark ->
            row.append(",")
            row.append(landmark.x)
            row.append(",")
            row.append(landmark.y)
            row.append(",")
            row.append(landmark.z)
        }
        
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        row.append(",")
        row.append(timestamp)
        
        return row.toString()
    }
    
    /**
     * يحصل على عدد العينات المحفوظة لتصنيف معين
     */
    fun getSampleCountForLabel(context: Context, label: String): Int {
        return try {
            val csvFile = File(getTrainingDataDirectory(context), CSV_FILE_NAME)
            if (!csvFile.exists()) {
                return 0
            }
            
            csvFile.readLines()
                .drop(1) // Skip header
                .count { it.startsWith("$label,") }
        } catch (e: Exception) {
            Log.e(TAG, "Error counting samples", e)
            0
        }
    }
    
    /**
     * يحصل على إجمالي عدد العينات المحفوظة
     */
    fun getTotalSampleCount(context: Context): Int {
        return try {
            val csvFile = File(getTrainingDataDirectory(context), CSV_FILE_NAME)
            if (!csvFile.exists()) {
                return 0
            }
            
            csvFile.readLines().size - 1 // Exclude header
        } catch (e: Exception) {
            Log.e(TAG, "Error counting total samples", e)
            0
        }
    }
    
    /**
     * يحصل على إحصائيات التعلم
     */
    fun getLearningStats(context: Context): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()
        
        try {
            val csvFile = File(getTrainingDataDirectory(context), CSV_FILE_NAME)
            if (!csvFile.exists()) {
                return stats
            }
            
            csvFile.readLines()
                .drop(1) // Skip header
                .forEach { line ->
                    val label = line.split(",").firstOrNull() ?: return@forEach
                    stats[label] = stats.getOrDefault(label, 0) + 1
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting learning stats", e)
        }
        
        return stats
    }
    
    /**
     * يحذف جميع بيانات التدريب
     */
    fun clearTrainingData(context: Context): Boolean {
        return try {
            val csvFile = File(getTrainingDataDirectory(context), CSV_FILE_NAME)
            if (csvFile.exists()) {
                csvFile.delete()
                Log.d(TAG, "Training data cleared")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing training data", e)
            false
        }
    }
    
    /**
     * يحصل على مسار ملف CSV للتصدير
     */
    fun getCsvFilePath(context: Context): String? {
        val csvFile = File(getTrainingDataDirectory(context), CSV_FILE_NAME)
        return if (csvFile.exists()) {
            csvFile.absolutePath
        } else {
            null
        }
    }
    
    /**
     * يحصل على حجم ملف CSV
     */
    fun getCsvFileSize(context: Context): Long {
        val csvFile = File(getTrainingDataDirectory(context), CSV_FILE_NAME)
        return if (csvFile.exists()) {
            csvFile.length()
        } else {
            0L
        }
    }
}










