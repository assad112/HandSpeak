package com.example.handspeak.ui.screen.settings

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.handspeak.util.ImageDownloader
import com.example.handspeak.util.ImagePickerHelper
import kotlinx.coroutines.launch

/**
 * شاشة إعدادات تحميل الصور
 * تسمح للمستخدم بتحميل الصور من الإنترنت وحفظها محلياً
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDownloadSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var imageUrl by remember { mutableStateOf("") }
    var folderName by remember { mutableStateOf("") }
    var imageIndex by remember { mutableStateOf("1") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadMessage by remember { mutableStateOf("") }
    var cacheSize by remember { mutableStateOf(0L) }
    
    // حساب حجم Cache
    LaunchedEffect(Unit) {
        cacheSize = ImageDownloader.getTotalCacheSize(context)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تحميل الصور") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // معلومات Cache
                Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "معلومات التخزين",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    
                    Text(
                        text = "حجم الصور المحفوظة: ${String.format("%.2f", cacheSize / 1024.0 / 1024.0)} MB",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Button(
                        onClick = {
                            scope.launch {
                                ImageDownloader.manageCacheSize(context)
                                cacheSize = ImageDownloader.getTotalCacheSize(context)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تنظيف Cache")
                    }
                }
            }
            }
            
            item {
                HorizontalDivider()
            }
            
            item {
                // اختيار الصور من الجهاز
                Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "اختيار الصور من الجهاز",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    
                    // اسم المجلد
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("اسم المجلد") },
                        placeholder = { Text("مثال: alef, marhaba") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // رقم الصورة الأولى
                    OutlinedTextField(
                        value = imageIndex,
                        onValueChange = { imageIndex = it },
                        label = { Text("رقم الصورة الأولى") },
                        placeholder = { Text("1, 2, 3, ...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Launcher لاختيار صورة واحدة
                    val singleImagePicker = ImagePickerHelper.rememberImagePicker { uri ->
                        uri?.let {
                            scope.launch {
                                isDownloading = true
                                downloadMessage = ""
                                
                                try {
                                    val index = imageIndex.toIntOrNull() ?: 1
                                    val success = ImageDownloader.copyImageFromUri(
                                        context,
                                        it,
                                        folderName,
                                        index
                                    )
                                    
                                    if (success) {
                                        downloadMessage = "✅ تم نسخ الصورة بنجاح!"
                                        cacheSize = ImageDownloader.getTotalCacheSize(context)
                                    } else {
                                        downloadMessage = "❌ فشل نسخ الصورة"
                                    }
                                } catch (e: Exception) {
                                    downloadMessage = "❌ خطأ: ${e.message}"
                                } finally {
                                    isDownloading = false
                                }
                            }
                        }
                    }
                    
                    // Launcher لاختيار عدة صور
                    val multipleImagePicker = ImagePickerHelper.rememberMultipleImagePicker { uris ->
                        if (uris.isNotEmpty() && folderName.isNotEmpty()) {
                            scope.launch {
                                isDownloading = true
                                downloadMessage = ""
                                
                                try {
                                    val startIndex = imageIndex.toIntOrNull() ?: 1
                                    val successCount = ImageDownloader.copyMultipleImagesFromUris(
                                        context,
                                        uris,
                                        folderName,
                                        startIndex
                                    )
                                    
                                    downloadMessage = "✅ تم نسخ $successCount/${uris.size} صورة بنجاح!"
                                    cacheSize = ImageDownloader.getTotalCacheSize(context)
                                } catch (e: Exception) {
                                    downloadMessage = "❌ خطأ: ${e.message}"
                                } finally {
                                    isDownloading = false
                                }
                            }
                        } else {
                            downloadMessage = "⚠️ يرجى تحديد اسم المجلد أولاً"
                        }
                    }
                    
                    // زر اختيار صورة واحدة
                    OutlinedButton(
                        onClick = {
                            if (folderName.isNotEmpty()) {
                                singleImagePicker.launch("image/*")
                            } else {
                                downloadMessage = "⚠️ يرجى تحديد اسم المجلد أولاً"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isDownloading && folderName.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختر صورة واحدة")
                    }
                    
                    // زر اختيار عدة صور
                    OutlinedButton(
                        onClick = {
                            if (folderName.isNotEmpty()) {
                                multipleImagePicker.launch("image/*")
                            } else {
                                downloadMessage = "⚠️ يرجى تحديد اسم المجلد أولاً"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isDownloading && folderName.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Collections, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختر عدة صور")
                    }
                    
                    // رسالة النتيجة
                    if (downloadMessage.isNotEmpty()) {
                        Text(
                            text = downloadMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (downloadMessage.startsWith("✅")) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            }
            
            item {
                HorizontalDivider()
            }
            
            item {
                // نموذج التحميل من URL (الطريقة القديمة)
                Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "تحميل من رابط (URL)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    
                    // رابط الصورة
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("رابط الصورة (URL)") },
                        placeholder = { Text("https://example.com/image.png") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // اسم المجلد
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("اسم المجلد") },
                        placeholder = { Text("مثال: alef, marhaba") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // رقم الصورة
                    OutlinedTextField(
                        value = imageIndex,
                        onValueChange = { imageIndex = it },
                        label = { Text("رقم الصورة") },
                        placeholder = { Text("1, 2, 3, ...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // زر التحميل
                    Button(
                        onClick = {
                            if (imageUrl.isNotEmpty() && folderName.isNotEmpty() && imageIndex.isNotEmpty()) {
                                isDownloading = true
                                downloadMessage = ""
                                
                                scope.launch {
                                    try {
                                        val index = imageIndex.toIntOrNull() ?: 1
                                        val bitmap = ImageDownloader.downloadImage(
                                            context,
                                            imageUrl,
                                            folderName,
                                            index
                                        )
                                        
                                        if (bitmap != null) {
                                            downloadMessage = "✅ تم تحميل الصورة بنجاح!"
                                            cacheSize = ImageDownloader.getTotalCacheSize(context)
                                            // مسح الحقول
                                            imageUrl = ""
                                            imageIndex = "1"
                                        } else {
                                            downloadMessage = "❌ فشل تحميل الصورة. تحقق من الرابط."
                                        }
                                    } catch (e: Exception) {
                                        downloadMessage = "❌ خطأ: ${e.message}"
                                    } finally {
                                        isDownloading = false
                                    }
                                }
                            } else {
                                downloadMessage = "⚠️ يرجى ملء جميع الحقول"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isDownloading && imageUrl.isNotEmpty() && folderName.isNotEmpty()
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري التحميل...")
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تحميل الصورة")
                        }
                    }
                    
                    // رسالة النتيجة
                    if (downloadMessage.isNotEmpty()) {
                        Text(
                            text = downloadMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (downloadMessage.startsWith("✅")) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            }
            
            item {
                // إرشادات
                Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "💡 إرشادات",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    
                    Text(
                        text = "• استخدم أسماء المجلدات من sign_map.json",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• الصور تُحفظ في Internal Storage",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• الصور المحمّلة لها أولوية على الصور في Assets",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• الحد الأقصى لحجم Cache: 50 MB",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            }
        }
    }
}

