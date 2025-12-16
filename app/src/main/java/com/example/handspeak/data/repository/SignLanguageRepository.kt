package com.example.handspeak.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.handspeak.data.api.ApiClient
import com.example.handspeak.data.api.SignLanguageApi
import com.example.handspeak.data.api.model.*
import com.example.handspeak.data.model.SignInfo
import com.example.handspeak.util.ImageDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Repository للتعامل مع API لغة الإشارة
 * 
 * يجمع بين البيانات المحلية والبيانات من الإنترنت
 */
class SignLanguageRepository(
    private val context: Context,
    private val apiService: SignLanguageApi = ApiClient.apiService
) {
    
    private val TAG = "SignLanguageRepository"
    
    /**
     * جلب معلومات إشارة من الإنترنت
     * 
     * @param word الكلمة بالعربية
     * @return SignInfo معلومات الإشارة، أو null في حالة الفشل
     */
    suspend fun getSignInfoFromApi(word: String): SignInfo? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSignInfo(word)
            
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                convertToSignInfo(apiResponse)
            } else {
                Log.w(TAG, "Failed to get sign info: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching sign info from API", e)
            null
        }
    }
    
    /**
     * جلب صور إشارة من الإنترنت وتحميلها محلياً
     * 
     * @param folder اسم المجلد
     * @return عدد الصور المحمّلة بنجاح
     */
    suspend fun downloadSignImages(folder: String): Int = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSignImages(folder)
            
            if (response.isSuccessful && response.body() != null) {
                val imageUrls = response.body()!!
                var successCount = 0
                
                imageUrls.forEachIndexed { index, imageUrl ->
                    val bitmap = ImageDownloader.downloadImage(
                        context,
                        imageUrl,
                        folder,
                        index + 1
                    )
                    if (bitmap != null) {
                        successCount++
                    }
                }
                
                Log.d(TAG, "Downloaded $successCount/${imageUrls.size} images for folder: $folder")
                successCount
            } else {
                Log.w(TAG, "Failed to get images: ${response.code()}")
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading images from API", e)
            0
        }
    }
    
    /**
     * البحث عن إشارات في API
     * 
     * @param query نص البحث
     * @return قائمة بالإشارات المطابقة
     */
    suspend fun searchSigns(query: String): List<SignInfo> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchSigns(query)
            
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.mapNotNull { convertToSignInfo(it) }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching signs", e)
            emptyList()
        }
    }
    
    /**
     * التعرف على حرف من صورة الكاميرا
     * 
     * @param bitmap صورة من الكاميرا
     * @return RecognitionResponse نتيجة التعرف، أو null في حالة الفشل
     */
    suspend fun recognizeLetterFromImage(bitmap: Bitmap): RecognitionResponse? = withContext(Dispatchers.IO) {
        try {
            // تحويل Bitmap إلى ملف مؤقت
            val tempFile = File(context.cacheDir, "temp_camera_image.jpg")
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            // إنشاء MultipartBody
            val requestFile = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", tempFile.name, requestFile)
            
            // إرسال الطلب
            val response = apiService.recognizeLetter(body)
            
            // حذف الملف المؤقت
            tempFile.delete()
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Recognized letter: ${response.body()!!.letter} (confidence: ${response.body()!!.confidence})")
                response.body()
            } else {
                Log.w(TAG, "Failed to recognize letter: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recognizing letter from image", e)
            null
        }
    }
    
    /**
     * التعرف على حرف من Landmarks
     * 
     * @param landmarks قائمة landmarks (21 نقطة)
     * @return RecognitionResponse نتيجة التعرف، أو null في حالة الفشل
     */
    suspend fun recognizeLetterFromLandmarks(
        landmarks: List<com.example.handspeak.data.model.HandLandmark>
    ): RecognitionResponse? = withContext(Dispatchers.IO) {
        try {
            // تحويل landmarks إلى LandmarkPoint
            val landmarkPoints = landmarks.map { landmark ->
                LandmarkPoint(
                    x = landmark.x,
                    y = landmark.y,
                    z = landmark.z
                )
            }
            
            val request = LandmarksRequest(landmarks = landmarkPoints)
            val response = apiService.recognizeFromLandmarks(request)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Recognized letter from landmarks: ${response.body()!!.letter}")
                response.body()
            } else {
                Log.w(TAG, "Failed to recognize from landmarks: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recognizing letter from landmarks", e)
            null
        }
    }
    
    /**
     * التعرف على حرف من تسلسل Landmarks (LSTM)
     * 
     * @param landmarksSequence تسلسل من landmarks
     * @return RecognitionResponse نتيجة التعرف، أو null في حالة الفشل
     */
    suspend fun recognizeLetterFromSequence(
        landmarksSequence: List<FloatArray>
    ): RecognitionResponse? = withContext(Dispatchers.IO) {
        try {
            // تحويل FloatArray إلى List<LandmarkPoint>
            val sequence = landmarksSequence.map { frame ->
                // كل frame يحتوي على 63 قيمة (21 landmark × 3 coordinates)
                val points = mutableListOf<LandmarkPoint>()
                for (i in 0 until 21) {
                    val index = i * 3
                    points.add(
                        LandmarkPoint(
                            x = frame[index],
                            y = frame[index + 1],
                            z = if (frame.size > index + 2) frame[index + 2] else 0f
                        )
                    )
                }
                points
            }
            
            val request = LandmarksSequenceRequest(sequence = sequence)
            val response = apiService.recognizeSequence(request)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Recognized letter from sequence: ${response.body()!!.letter}")
                response.body()
            } else {
                Log.w(TAG, "Failed to recognize from sequence: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recognizing letter from sequence", e)
            null
        }
    }
    
    /**
     * تحويل SignInfoResponse إلى SignInfo
     */
    private fun convertToSignInfo(apiResponse: SignInfoResponse): SignInfo {
        return SignInfo(
            label = apiResponse.label,
            type = apiResponse.type,
            folder = apiResponse.folder
        )
    }
}

