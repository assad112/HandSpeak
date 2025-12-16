package com.example.handspeak.data.api.model

/**
 * نماذج البيانات للـ API
 */

/**
 * استجابة معلومات إشارة واحدة
 */
data class SignInfoResponse(
    val label: String,
    val type: String, // "images" or "video"
    val folder: String?,
    val imageUrls: List<String>? = null, // روابط الصور من الإنترنت
    val videoUrl: String? = null
)

/**
 * استجابة التحديثات
 */
data class UpdateResponse(
    val hasUpdate: Boolean,
    val version: String?,
    val changelog: String?,
    val downloadUrl: String?
)

/**
 * استجابة البحث
 */
data class SearchResponse(
    val results: List<SignInfoResponse>,
    val total: Int
)

/**
 * استجابة التعرف على الحروف
 */
data class RecognitionResponse(
    val letter: String, // الحرف المعرّف (مثل: "أ", "ب", "ج")
    val confidence: Float, // نسبة الثقة (0.0 - 1.0)
    val alternatives: List<LetterAlternative>? = null // بدائل محتملة
)

/**
 * بديل محتمل للحرف
 */
data class LetterAlternative(
    val letter: String,
    val confidence: Float
)

/**
 * طلب التعرف من Landmarks
 */
data class LandmarksRequest(
    val landmarks: List<LandmarkPoint>, // 21 نقطة
    val imageWidth: Int? = null,
    val imageHeight: Int? = null
)

/**
 * نقطة Landmark
 */
data class LandmarkPoint(
    val x: Float,
    val y: Float,
    val z: Float? = null
)

/**
 * طلب التعرف من تسلسل Landmarks (LSTM)
 */
data class LandmarksSequenceRequest(
    val sequence: List<List<LandmarkPoint>>, // تسلسل من landmarks
    val imageWidth: Int? = null,
    val imageHeight: Int? = null
)

