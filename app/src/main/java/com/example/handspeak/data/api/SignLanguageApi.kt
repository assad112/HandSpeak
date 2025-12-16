package com.example.handspeak.data.api

import com.example.handspeak.data.api.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * API Service لجلب محتوى لغة الإشارة من الإنترنت
 * 
 * يمكن استخدام هذا API لجلب:
 * - صور الإشارات
 * - معلومات الإشارات
 * - تحديثات sign_map.json
 * - مقاطع فيديو الإشارات
 */
interface SignLanguageApi {
    
    /**
     * جلب معلومات إشارة معينة
     * 
     * @param word الكلمة بالعربية (مثل: "مرحبا", "شكرا")
     * @return SignInfoResponse معلومات الإشارة
     */
    @GET("signs/{word}")
    suspend fun getSignInfo(
        @Path("word") word: String
    ): Response<SignInfoResponse>
    
    /**
     * جلب قائمة بجميع الإشارات المتاحة
     * 
     * @return List<SignInfoResponse> قائمة بجميع الإشارات
     */
    @GET("signs")
    suspend fun getAllSigns(): Response<List<SignInfoResponse>>
    
    /**
     * جلب صور إشارة معينة
     * 
     * @param folder اسم المجلد (مثل: "marhaba", "alef")
     * @return List<String> قائمة بروابط الصور
     */
    @GET("signs/{folder}/images")
    suspend fun getSignImages(
        @Path("folder") folder: String
    ): Response<List<String>>
    
    /**
     * جلب sign_map.json محدث من السيرفر
     * 
     * @return Map<String, SignInfo> خريطة الإشارات
     */
    @GET("sign_map.json")
    suspend fun getSignMap(): Response<Map<String, SignInfoResponse>>
    
    /**
     * البحث عن إشارات
     * 
     * @param query نص البحث
     * @return List<SignInfoResponse> قائمة بالإشارات المطابقة
     */
    @GET("signs/search")
    suspend fun searchSigns(
        @Query("q") query: String
    ): Response<List<SignInfoResponse>>
    
    /**
     * جلب تحديثات sign_map.json
     * 
     * @param version الإصدار الحالي
     * @return UpdateResponse معلومات التحديث
     */
    @GET("updates")
    suspend fun checkUpdates(
        @Query("version") version: String
    ): Response<UpdateResponse>
    
    /**
     * التعرف على الحروف من صورة الكاميرا
     * 
     * @param image صورة من الكاميرا (Base64 أو Multipart)
     * @return RecognitionResponse نتيجة التعرف
     */
    @Multipart
    @POST("recognize/letter")
    suspend fun recognizeLetter(
        @Part image: okhttp3.MultipartBody.Part
    ): Response<RecognitionResponse>
    
    /**
     * التعرف على الحروف من landmarks
     * 
     * @param landmarks قائمة landmarks (21 نقطة)
     * @return RecognitionResponse نتيجة التعرف
     */
    @POST("recognize/landmarks")
    suspend fun recognizeFromLandmarks(
        @Body request: LandmarksRequest
    ): Response<RecognitionResponse>
    
    /**
     * التعرف على تسلسل من landmarks (LSTM)
     * 
     * @param landmarksSequence تسلسل من landmarks
     * @return RecognitionResponse نتيجة التعرف
     */
    @POST("recognize/sequence")
    suspend fun recognizeSequence(
        @Body request: LandmarksSequenceRequest
    ): Response<RecognitionResponse>
}

