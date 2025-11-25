package com.example.handspeak.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * ImageDownloader - يحمّل الصور من الإنترنت ويخزنها محلياً
 * 
 * الوظائف:
 * 1. تحميل صورة من URL
 * 2. حفظها في Internal Storage
 * 3. تحميلها من Storage المحلي
 * 4. إدارة الذاكرة والمساحة
 */
object ImageDownloader {
    
    private const val TAG = "ImageDownloader"
    private const val IMAGES_DIR = "sign_images"
    private const val MAX_CACHE_SIZE = 50 * 1024 * 1024 // 50 MB
    
    /**
     * يحصل على مجلد تخزين الصور
     */
    private fun getImagesDirectory(context: Context): File {
        val dir = File(context.filesDir, IMAGES_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * يحصل على مسار الملف المحلي للصورة
     */
    private fun getImageFile(context: Context, folder: String, index: Int): File {
        val dir = File(getImagesDirectory(context), folder)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "$index.png")
    }
    
    /**
     * يحمّل صورة من URL ويحفظها محلياً
     * 
     * @param imageUrl رابط الصورة
     * @param folder اسم المجلد (مثل "alef", "marhaba")
     * @param index رقم الصورة (1, 2, 3, ...)
     * @return Bitmap الصورة المحمّلة، أو null في حالة الفشل
     */
    suspend fun downloadImage(
        context: Context,
        imageUrl: String,
        folder: String,
        index: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Downloading image from: $imageUrl")
            
            // تحميل الصورة من الإنترنت
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000 // 10 seconds
            connection.readTimeout = 10000
            connection.doInput = true
            connection.connect()
            
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Failed to download image. Response code: ${connection.responseCode}")
                return@withContext null
            }
            
            // قراءة الصورة
            val inputStream = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            connection.disconnect()
            
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from URL: $imageUrl")
                return@withContext null
            }
            
            // حفظ الصورة محلياً
            saveImageLocally(context, bitmap, folder, index)
            
            Log.d(TAG, "Image downloaded and saved: $folder/$index.png")
            bitmap
            
        } catch (e: IOException) {
            Log.e(TAG, "Error downloading image from $imageUrl", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error downloading image", e)
            null
        }
    }
    
    /**
     * يحفظ صورة محلياً في Internal Storage
     */
    private fun saveImageLocally(
        context: Context,
        bitmap: Bitmap,
        folder: String,
        index: Int
    ): Boolean {
        return try {
            val imageFile = getImageFile(context, folder, index)
            
            // حفظ الصورة
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            
            Log.d(TAG, "Image saved to: ${imageFile.absolutePath}")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error saving image locally", e)
            false
        }
    }
    
    /**
     * يحمّل صورة من Storage المحلي
     * 
     * @return Bitmap الصورة، أو null إذا لم تكن موجودة
     */
    fun loadImageFromStorage(context: Context, folder: String, index: Int): Bitmap? {
        return try {
            val imageFile = getImageFile(context, folder, index)
            
            if (!imageFile.exists()) {
                Log.d(TAG, "Image not found in storage: $folder/$index.png")
                return null
            }
            
            BitmapFactory.decodeFile(imageFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image from storage", e)
            null
        }
    }
    
    /**
     * يتحقق من وجود صورة في Storage المحلي
     */
    fun imageExistsInStorage(context: Context, folder: String, index: Int): Boolean {
        val imageFile = getImageFile(context, folder, index)
        return imageFile.exists()
    }
    
    /**
     * يحصل على عدد الصور المحفوظة في مجلد معين
     */
    fun getImageCountInStorage(context: Context, folder: String): Int {
        val dir = File(getImagesDirectory(context), folder)
        if (!dir.exists()) {
            return 0
        }
        
        var count = 0
        var index = 1
        
        while (true) {
            val imageFile = File(dir, "$index.png")
            if (imageFile.exists()) {
                count++
                index++
            } else {
                break
            }
        }
        
        return count
    }
    
    /**
     * يحذف صورة من Storage المحلي
     */
    fun deleteImage(context: Context, folder: String, index: Int): Boolean {
        return try {
            val imageFile = getImageFile(context, folder, index)
            if (imageFile.exists()) {
                imageFile.delete()
                Log.d(TAG, "Image deleted: $folder/$index.png")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting image", e)
            false
        }
    }
    
    /**
     * يحذف جميع الصور في مجلد معين
     */
    fun deleteFolder(context: Context, folder: String): Boolean {
        return try {
            val dir = File(getImagesDirectory(context), folder)
            if (dir.exists()) {
                dir.deleteRecursively()
                Log.d(TAG, "Folder deleted: $folder")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting folder", e)
            false
        }
    }
    
    /**
     * يحذف جميع الصور المحفوظة (تنظيف Cache)
     */
    fun clearAllImages(context: Context): Boolean {
        return try {
            val dir = getImagesDirectory(context)
            if (dir.exists()) {
                dir.deleteRecursively()
                dir.mkdirs()
                Log.d(TAG, "All images cleared")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all images", e)
            false
        }
    }
    
    /**
     * يحسب حجم جميع الصور المحفوظة
     */
    fun getTotalCacheSize(context: Context): Long {
        val dir = getImagesDirectory(context)
        if (!dir.exists()) {
            return 0
        }
        
        var totalSize = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) {
                totalSize += file.length()
            }
        }
        
        return totalSize
    }
    
    /**
     * يحذف الصور القديمة إذا تجاوزت الحد الأقصى
     */
    fun manageCacheSize(context: Context) {
        val currentSize = getTotalCacheSize(context)
        
        if (currentSize > MAX_CACHE_SIZE) {
            Log.d(TAG, "Cache size ($currentSize) exceeds limit ($MAX_CACHE_SIZE). Cleaning...")
            // يمكن إضافة منطق لحذف الصور الأقدم
            // حالياً، سنحذف جميع الصور
            clearAllImages(context)
        }
    }
    
    /**
     * يحصل على مسار الملف المحلي للاستخدام مع Coil
     */
    fun getLocalImagePath(context: Context, folder: String, index: Int): String? {
        val imageFile = getImageFile(context, folder, index)
        return if (imageFile.exists()) {
            "file://${imageFile.absolutePath}"
        } else {
            null
        }
    }
    
    /**
     * ينسخ صورة من URI (من اختيار الملفات) إلى Storage المحلي
     * 
     * @param context Context
     * @param imageUri URI الصورة المختارة
     * @param folder اسم المجلد (مثل "alef", "marhaba")
     * @param index رقم الصورة (1, 2, 3, ...)
     * @return true إذا نجح النسخ، false في حالة الفشل
     */
    suspend fun copyImageFromUri(
        context: Context,
        imageUri: Uri,
        folder: String,
        index: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Copying image from URI: $imageUri to $folder/$index.png")
            
            // فتح InputStream من URI
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream from URI: $imageUri")
                return@withContext false
            }
            
            // قراءة الصورة
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from URI: $imageUri")
                return@withContext false
            }
            
            // حفظ الصورة محلياً
            val success = saveImageLocally(context, bitmap, folder, index)
            
            if (success) {
                Log.d(TAG, "Successfully copied image: $folder/$index.png")
            }
            
            success
        } catch (e: IOException) {
            Log.e(TAG, "Error copying image from URI", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error copying image from URI", e)
            false
        }
    }
    
    /**
     * ينسخ عدة صور دفعة واحدة
     * 
     * @param context Context
     * @param imageUris قائمة URIs للصور
     * @param folder اسم المجلد
     * @param startIndex رقم البداية (افتراضي: 1)
     * @return عدد الصور المنسوخة بنجاح
     */
    suspend fun copyMultipleImagesFromUris(
        context: Context,
        imageUris: List<Uri>,
        folder: String,
        startIndex: Int = 1
    ): Int = withContext(Dispatchers.IO) {
        var successCount = 0
        
        imageUris.forEachIndexed { index, uri ->
            val imageIndex = startIndex + index
            if (copyImageFromUri(context, uri, folder, imageIndex)) {
                successCount++
            }
        }
        
        Log.d(TAG, "Copied $successCount/${imageUris.size} images to $folder")
        successCount
    }
}

