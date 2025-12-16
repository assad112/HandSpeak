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
        resolvedFolderNameCache[folder]?.let { 
            Log.d(TAG, "Using cached resolved folder: $folder -> $it")
            return it 
        }
        return try {
            val entries = context.assets.list("signs")?.toList().orEmpty()
            Log.d(TAG, "Resolving folder: $folder (found ${entries.size} folders in signs/)")
            
            // exact match first
            if (entries.contains(folder)) {
                resolvedFolderNameCache[folder] = folder
                Log.d(TAG, "Exact match found: $folder")
                return folder
            }
            
            // case-insensitive match
            val match = entries.firstOrNull { it.equals(folder, ignoreCase = true) } ?: folder
            resolvedFolderNameCache[folder] = match
            Log.d(TAG, "Resolved folder: $folder -> $match")
            match
        } catch (e: IOException) {
            Log.e(TAG, "Error resolving folder: $folder", e)
            folder
        }
    }

    private fun listFolderImages(context: Context, folder: String): List<String> {
        // Cached?
        folderToAssetsCache[folder]?.let { 
            Log.d(TAG, "Using cached images for folder: $folder (${it.size} images)")
            return it 
        }
        return try {
            val resolved = resolveFolderName(context, folder)
            Log.d(TAG, "Listing images for folder: $folder (resolved: $resolved)")
            
            val all = context.assets.list("signs/$resolved")?.toList().orEmpty()
            Log.d(TAG, "Found ${all.size} files in signs/$resolved: ${all.take(5).joinToString(", ")}")
            
            if (all.isEmpty()) {
                Log.w(TAG, "No files found in signs/$resolved for folder: $folder")
                folderToAssetsCache[folder] = emptyList()
                return emptyList()
            }
            
            val images = all.filter { name ->
                val ext = name.substringAfterLast('.', "").lowercase()
                supportedExtensions.contains(ext)
            }.sortedWith(
                compareBy<String>(
                    // First priority: files starting with number (1.jpg, 2.jpg, etc.)
                    { 
                        val nameWithoutExt = it.substringBeforeLast('.')
                        // Try to extract number from start
                        val numAtStart = nameWithoutExt.toIntOrNull()
                        if (numAtStart != null) numAtStart
                        // Second priority: extract number from anywhere (Kaf_77.jpg -> 77)
                        else {
                            val numbers = Regex("\\d+").findAll(nameWithoutExt).map { it.value.toIntOrNull() ?: Int.MAX_VALUE }.toList()
                            numbers.firstOrNull() ?: Int.MAX_VALUE
                        }
                    },
                    // Secondary sort: alphabetical
                    { it }
                )
            ).map { "signs/$resolved/$it" }
            
            Log.d(TAG, "Found ${images.size} images for folder: $folder (resolved: $resolved): ${images.take(3).joinToString(", ")}")
            folderToAssetsCache[folder] = images
            images
        } catch (e: IOException) {
            Log.e(TAG, "Error listing images for folder: $folder", e)
            emptyList()
        }
    }
    
    /**
     * يحصل على قائمة مسارات الصور في مجلد إشارة معين
     * يبحث في Assets ويعيد قائمة المسارات النسبية (مثل: signs/noon/1.png)
     */
    fun getImagePaths(context: Context, folder: String): List<String> {
        return listFolderImages(context, folder)
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
            // حل اسم المجلد (case-insensitive)
            val resolvedFolder = resolveFolderName(context, folder)
            Log.d(TAG, "Loading image from folder: $folder (resolved: $resolvedFolder), index: $index")
            
            // محاولة مباشرة أولاً (أسرع - للصور المرقمة 1.png, 2.png, etc.)
            try {
                val directPath = "signs/$resolvedFolder/$index.png"
                val inputStream = context.assets.open(directPath)
                return BitmapFactory.decodeStream(inputStream).also {
                    inputStream.close()
                    Log.d(TAG, "Loaded image directly: $directPath")
                }
            } catch (e: IOException) {
                Log.d(TAG, "Direct path failed, trying listFolderImages: signs/$resolvedFolder/$index.png")
            }
            
            // إذا فشلت المحاولة المباشرة، استخدم listFolderImages
            val images = listFolderImages(context, folder)
            if (images.isEmpty()) {
                Log.w(TAG, "No images found for folder: $folder (resolved: $resolvedFolder)")
                return null
            }
            
            Log.d(TAG, "Found ${images.size} images in folder: $folder")
            
            val imagePath = images.getOrNull(index - 1)
            if (imagePath == null) {
                Log.w(TAG, "Image index $index not found for folder: $folder (total: ${images.size}), using first image")
                // Use first image if index is out of bounds
                val firstImage = images.firstOrNull() ?: return null
                val inputStream = context.assets.open(firstImage)
                return BitmapFactory.decodeStream(inputStream).also {
                    inputStream.close()
                    Log.d(TAG, "Loaded first image from assets: $firstImage")
                }
            }
            
            val inputStream = context.assets.open(imagePath)
            BitmapFactory.decodeStream(inputStream).also {
                inputStream.close()
                Log.d(TAG, "Loaded image from assets: $imagePath")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load image from assets for folder: $folder, index: $index", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading image for folder: $folder, index: $index", e)
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
            Log.d(TAG, "Using local image path: $localPath")
            return localPath
        }
        
        // ثانياً: استخدام Assets
        val images = listFolderImages(context, folder)
        if (images.isEmpty()) {
            Log.w(TAG, "No images found for folder: $folder")
            return "file:///android_asset/signs/$folder/$index.png"
        }
        
        val imagePath = images.getOrNull(index - 1)
        if (imagePath == null) {
            Log.w(TAG, "Image index $index not found for folder: $folder (total: ${images.size})")
            // Return first image if index is out of bounds
            val firstImage = images.firstOrNull()
            if (firstImage != null) {
                Log.d(TAG, "Using first image instead: $firstImage")
                return "file:///android_asset/$firstImage"
            }
            return "file:///android_asset/signs/$folder/$index.png"
        }
        
        Log.d(TAG, "Using asset image path: file:///android_asset/$imagePath")
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
    
    /**
     * يحمل صورة من Assets باستخدام المسار الكامل
     */
    fun loadImageFromAssets(context: Context, imagePath: String): Bitmap? {
        return try {
            val inputStream = context.assets.open(imagePath)
            BitmapFactory.decodeStream(inputStream).also {
                inputStream.close()
                Log.d(TAG, "Loaded image from assets: $imagePath")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load image from assets: $imagePath", e)
            null
        }
    }
}

