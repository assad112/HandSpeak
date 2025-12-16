package com.example.handspeak.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * ApiClient - إعداد وتكوين Retrofit للاتصال بالـ API
 * 
 * يمكنك تغيير BASE_URL حسب احتياجاتك
 */
object ApiClient {
    
    // يمكن تغيير هذا الرابط حسب API الخاص بك
    // مثال: "https://api.handspeak.com/api/v1/"
    // أو: "https://your-server.com/api/"
    private const val BASE_URL = "https://api.example.com/api/v1/"
    
    // تفعيل Logging (يمكن تغييره إلى false للإنتاج)
    private const val ENABLE_LOGGING = true
    
    /**
     * إنشاء OkHttpClient مع Logging Interceptor
     */
    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (ENABLE_LOGGING) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * إنشاء Retrofit instance
     */
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(createOkHttpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    /**
     * الحصول على API Service
     */
    val apiService: SignLanguageApi = retrofit.create(SignLanguageApi::class.java)
    
    /**
     * تغيير BASE_URL ديناميكياً (مفيد للإعدادات)
     */
    fun createApiService(baseUrl: String): SignLanguageApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        return retrofit.create(SignLanguageApi::class.java)
    }
}

