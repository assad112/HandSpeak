package com.example.handspeak.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Helper لاختيار الصور من الجهاز
 */
object ImagePickerHelper {
    private const val TAG = "ImagePickerHelper"
    
    /**
     * Launcher لاختيار صورة واحدة
     */
    @Composable
    fun rememberImagePicker(
        onImageSelected: (Uri?) -> Unit
    ) = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onImageSelected(uri)
    }
    
    /**
     * Launcher لاختيار عدة صور
     */
    @Composable
    fun rememberMultipleImagePicker(
        onImagesSelected: (List<Uri>) -> Unit
    ) = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        onImagesSelected(uris)
    }
}

