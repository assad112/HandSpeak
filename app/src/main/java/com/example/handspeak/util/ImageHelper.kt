package com.example.handspeak.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.IOException

object ImageHelper {
    
    private const val TAG = "ImageHelper"
    private val supportedExtensions = setOf("png", "jpg", "jpeg", "webp")
    private val folderToAssetsCache: MutableMap<String, List<String>> = mutableMapOf()
    private val resolvedFolderNameCache: MutableMap<String, String> = mutableMapOf()

    private fun resolveFolderName(context: Context, folder: String): String {
        // Cache
        resolvedFolderNameCache[folder]?.let { return it }
        return try {
            val entries = context.assets.list("signs")?.toList().orEmpty()
            // exact match first
            if (entries.contains(folder)) {
                resolvedFolderNameCache[folder] = folder
                return folder
            }
            // case-insensitive match
            val match = entries.firstOrNull { it.equals(folder, ignoreCase = true) } ?: folder
            resolvedFolderNameCache[folder] = match
            match
        } catch (_: IOException) {
            folder
        }
    }

    private fun listFolderImages(context: Context, folder: String): List<String> {
        // Cached?
        folderToAssetsCache[folder]?.let { return it }
        return try {
            val resolved = resolveFolderName(context, folder)
            val all = context.assets.list("signs/$resolved")?.toList().orEmpty()
            val images = all.filter { name ->
                val ext = name.substringAfterLast('.', "").lowercase()
                supportedExtensions.contains(ext)
            }.sortedWith(
                compareBy<String>(
                    { it.substringBefore('.').toIntOrNull() ?: Int.MAX_VALUE },
                    { it }
                )
            ).map { "signs/$resolved/$it" }
            folderToAssetsCache[folder] = images
            images
        } catch (e: IOException) {
            emptyList()
        }
    }
    
    /**
     * يحصل على عدد الصور المتاحة في مجلد إشارة معين
     * يبحث أولاً في Storage المحلي، ثم في Assets
     */
    fun getImageCount(context: Context, folder: String): Int {
        // أولاً: البحث في Storage المحلي (الصور المحمّلة)
        val localCount = ImageDownloader.getImageCountInStorage(context, folder)
        if (localCount > 0) {
            Log.d(TAG, "Found $localCount images in storage for folder: $folder")
            return localCount
        }
        
        // ثانياً: البحث في Assets (الصور المدمجة)
        val images = listFolderImages(context, folder)
        if (images.isNotEmpty()) Log.d(TAG, "Found ${images.size} images in assets for folder: $folder")
        return images.size
    }
    
    /**
     * يحمل صورة من Storage المحلي أولاً، ثم من Assets
     */
    fun loadImage(context: Context, folder: String, index: Int): Bitmap? {
        // أولاً: محاولة التحميل من Storage المحلي
        val localImage = ImageDownloader.loadImageFromStorage(context, folder, index)
        if (localImage != null) {
            Log.d(TAG, "Loaded image from storage: $folder/$index.png")
            return localImage
        }
        
        // ثانياً: التحميل من Assets
        return try {
            val images = listFolderImages(context, folder)
            val imagePath = images.getOrNull(index - 1) ?: return null
            val inputStream = context.assets.open(imagePath)
            BitmapFactory.decodeStream(inputStream).also {
                inputStream.close()
                Log.d(TAG, "Loaded image from assets: $imagePath")
            }
        } catch (e: IOException) {
            null
        }
    }
    
    /**
     * يحصل على مسار الصورة كـ String لاستخدامه مع Coil
     * يبحث أولاً في Storage المحلي، ثم في Assets
     */
    fun getImagePath(context: Context, folder: String, index: Int): String {
        // أولاً: البحث في Storage المحلي
        val localPath = ImageDownloader.getLocalImagePath(context, folder, index)
        if (localPath != null) {
            return localPath
        }
        
        // ثانياً: استخدام Assets
        val images = listFolderImages(context, folder)
        val imagePath = images.getOrNull(index - 1) ?: return "file:///android_asset/signs/$folder/$index.png"
        return "file:///android_asset/$imagePath"
    }
    
    /**
     * يتحقق من وجود صورة معينة
     * يبحث أولاً في Storage المحلي، ثم في Assets
     */
    fun imageExists(context: Context, folder: String, index: Int): Boolean {
        // أولاً: البحث في Storage المحلي
        if (ImageDownloader.imageExistsInStorage(context, folder, index)) {
            return true
        }
        
        // ثانياً: البحث في Assets
        val images = listFolderImages(context, folder)
        return images.getOrNull(index - 1) != null
    }
}

