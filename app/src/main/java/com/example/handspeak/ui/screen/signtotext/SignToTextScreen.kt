package com.example.handspeak.ui.screen.signtotext

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import java.util.concurrent.Executors
import com.example.handspeak.ui.components.MainBottomBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SignToTextScreen(
    navController: NavController,
    viewModel: SignToTextViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إشارة إلى نص") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        bottomBar = { MainBottomBar(navController) }
    ) { padding ->
        if (cameraPermissionState.status.isGranted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Camera Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    CameraPreview(
                        onFrame = { bitmap ->
                            // التأكد من أن bitmap صالح
                            if (bitmap != null && !bitmap.isRecycled) {
                                viewModel.processFrame(bitmap)
                            }
                        },
                        landmarks = uiState.currentLandmarks,
                        useFrontCamera = uiState.useFrontCamera
                    )
                    
                    // Hand detection indicator with sequence progress - محسّن
                    if (uiState.isHandDetected) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PanTool,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "✅ يد مكتشفة",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                
                                // عرض وضع LSTM أو Dense
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (uiState.useLSTM) Icons.Default.Memory else Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (uiState.useLSTM) "🧠 وضع LSTM (دقيق)" else "⚡ وضع Dense (سريع)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                
                                // عرض تقدم تسجيل الإشارة (LSTM mode)
                                if (uiState.useLSTM && uiState.sequenceBufferSize > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    val maxFrames = 5 // SEQUENCE_LENGTH
                                    val progress = (uiState.sequenceBufferSize.toFloat() / maxFrames).coerceIn(0f, 1f)
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "📸 تسجيل الإشارة...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "${uiState.sequenceBufferSize}/$maxFrames",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    
                                    // شريط التقدم المحسّن
                                    Spacer(modifier = Modifier.height(6.dp))
                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = progress,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                } else if (!uiState.useLSTM) {
                                    // وضع Dense - تحليل فوري
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "⚡ تحليل فوري - ثبّت يدك",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Processing indicator
                    if (uiState.isProcessing) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "جاري التحليل...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                    
                    // إشعار الحفظ التلقائي
                    if (uiState.showLearningSavedMessage) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
                            ),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "✅ تم الحفظ بنجاح!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "شكراً لمساعدتك في تحسين التطبيق 🎓",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                    
                    // Movement indicator (LSTM buffer status)
                    if (uiState.useLSTM && uiState.sequenceBufferSize > 0 && uiState.sequenceBufferSize < 10) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PanTool,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "تسجيل الحركة",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "${uiState.sequenceBufferSize}/10 إطارات",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Camera switch button
                    FloatingActionButton(
                        onClick = {
                            viewModel.toggleCamera()
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "تبديل الكاميرا",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    
                    // Detected text overlay with improved design - عرض الحرف المكتشف بشكل كبير وواضح
                    if (uiState.detectedText.isNotEmpty()) {
                        // تحديد نوع النتيجة (حرف أم كلمة)
                        val isWord = uiState.detectedText.length > 1 || 
                                    uiState.detectedText.contains(" ") ||
                                    uiState.detectedText in listOf("أحبك", "مرحباً", "شجرة", "بطة", "قطة", "هاتف", 
                                                                  "موافق", "أنت", "لا", "سؤال", "اقتباس", "أراك لاحقاً",
                                                                  "أضحكتني", "هذا رهيب", "أنا أراقبك", "عمل جيد",
                                                                  "أتمنى لك حياة سعيدة", "لست متأكد", "هذا ممتاز")
                        val resultType = if (isWord) "كلمة" else "حرف"
                        
                        // عرض الحرف/الكلمة المكتشفة بشكل كبير وواضح في الوسط
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(0.95f)
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF667EEA).copy(alpha = 0.98f)
                                ),
                                elevation = CardDefaults.cardElevation(16.dp),
                                shape = RoundedCornerShape(32.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // عنوان يوضح نوع النتيجة
                                    Text(
                                        text = "تم اكتشاف $resultType:",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                    
                                    // الحرف/الكلمة المكتشفة - حجم كبير جداً
                                    Text(
                                        text = uiState.detectedText,
                                        style = if (isWord) {
                                            MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp)
                                        } else {
                                            MaterialTheme.typography.displayLarge.copy(fontSize = 120.sp)
                                        },
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .padding(vertical = 24.dp)
                                            .fillMaxWidth()
                                    )
                                    
                                    // زر تشغيل الصوت
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.speakDetectedText() },
                                            modifier = Modifier
                                                .size(56.dp)
                                                .background(
                                                    Color.White.copy(alpha = 0.3f),
                                                    shape = CircleShape
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "تشغيل الصوت",
                                                tint = Color.White,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // مؤشر الثقة
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = Color.White.copy(alpha = 0.9f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "مستوى الثقة: ${(uiState.confidence * 100).toInt()}%",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Accumulated text section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "النص المتراكم:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (uiState.accumulatedText.isNotEmpty()) {
                                Text(
                                    text = "${uiState.accumulatedText.split(" ").size} كلمة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Accumulated text display - يظهر النص مباشرة وبشكل واضح
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = if (uiState.accumulatedText.isNotEmpty()) {
                                androidx.compose.foundation.BorderStroke(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            } else null
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = if (uiState.accumulatedText.isEmpty()) 
                                    Alignment.Center 
                                else 
                                    Alignment.TopStart
                            ) {
                                if (uiState.accumulatedText.isEmpty()) {
                                    Text(
                                        text = "النص المكتشف سيظهر هنا مباشرة...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    // النص يظهر مباشرة وبشكل واضح
                                    Text(
                                        text = uiState.accumulatedText,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = MaterialTheme.typography.titleLarge.lineHeight * 1.3,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        
                        // مؤشر الحفظ التلقائي
                        if (uiState.accumulatedText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تم الحفظ تلقائياً في التاريخ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Action buttons with better styling
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.addToAccumulated() },
                                enabled = uiState.detectedText.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة")
                            }
                            
                            Button(
                                onClick = { viewModel.clearAccumulated() },
                                enabled = uiState.accumulatedText.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مسح")
                            }
                            
                            Button(
                                onClick = {
                                    viewModel.saveToHistory()
                                    navController.navigateUp()
                                },
                                enabled = uiState.accumulatedText.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حفظ")
                            }
                        }
                        
                    }
                }
            }
        } else {
            // Permission not granted
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "يحتاج التطبيق إلى إذن الكاميرا لترجمة لغة الإشارة",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("منح الإذن")
                }
            }
        }
        
        // Error dialog
        uiState.errorMessage?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("خطأ") },
                text = { Text(error) },
                confirmButton = {
                    Button(onClick = { viewModel.clearError() }) {
                        Text("حسناً")
                    }
                }
            )
        }
    }
}

@Composable
fun CameraPreview(
    onFrame: (Bitmap) -> Unit,
    landmarks: List<com.example.handspeak.data.model.HandLandmark> = emptyList(),
    useFrontCamera: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        key(useFrontCamera) { // إعادة بناء عند تغيير الكاميرا
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            }
            val executor = Executors.newSingleThreadExecutor()
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder()
                        .setTargetResolution(android.util.Size(1920, 1080)) // دقة أعلى
                    .build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                
                val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(android.util.Size(1280, 720)) // دقة أعلى للمعالجة
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()
                    .also {
                        it.setAnalyzer(executor) { imageProxy ->
                            val bitmap = imageProxy.toBitmap()
                            val rotatedBitmap = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees.toFloat())
                            onFrame(rotatedBitmap)
                            imageProxy.close()
                        }
                    }
                
                    val cameraSelector = if (useFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
        }
        
        // Landmarks overlay
        if (landmarks.isNotEmpty()) {
            AndroidView(
                factory = { ctx ->
                    object : View(ctx) {
                        override fun onDraw(canvas: Canvas) {
                            super.onDraw(canvas)
                            drawLandmarks(canvas, landmarks, width.toFloat(), height.toFloat())
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    view.invalidate()
                }
            )
        }
    }
}

private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

/**
 * رسم landmarks على الكاميرا
 */
private fun drawLandmarks(
    canvas: Canvas,
    landmarks: List<com.example.handspeak.data.model.HandLandmark>,
    viewWidth: Float,
    viewHeight: Float
) {
    if (landmarks.isEmpty()) return
    
    val pointPaint = Paint().apply {
        color = android.graphics.Color.GREEN
        style = Paint.Style.FILL
        strokeWidth = 8f
        isAntiAlias = true
    }
    
    val linePaint = Paint().apply {
        color = android.graphics.Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    
    // رسم النقاط
    landmarks.forEach { landmark ->
        val x = landmark.x * viewWidth
        val y = landmark.y * viewHeight
        canvas.drawCircle(x, y, 6f, pointPaint)
    }
    
    // رسم خطوط الاتصال بين النقاط
    // Wrist to thumb
    drawLine(canvas, landmarks, 0, 1, viewWidth, viewHeight, linePaint)
    // Thumb connections
    for (i in 1..3) {
        drawLine(canvas, landmarks, i, i + 1, viewWidth, viewHeight, linePaint)
    }
    // Index finger
    drawLine(canvas, landmarks, 0, 5, viewWidth, viewHeight, linePaint)
    for (i in 5..7) {
        drawLine(canvas, landmarks, i, i + 1, viewWidth, viewHeight, linePaint)
    }
    // Middle finger
    drawLine(canvas, landmarks, 0, 9, viewWidth, viewHeight, linePaint)
    for (i in 9..11) {
        drawLine(canvas, landmarks, i, i + 1, viewWidth, viewHeight, linePaint)
    }
    // Ring finger
    drawLine(canvas, landmarks, 0, 13, viewWidth, viewHeight, linePaint)
    for (i in 13..15) {
        drawLine(canvas, landmarks, i, i + 1, viewWidth, viewHeight, linePaint)
    }
    // Pinky finger
    drawLine(canvas, landmarks, 0, 17, viewWidth, viewHeight, linePaint)
    for (i in 17..19) {
        drawLine(canvas, landmarks, i, i + 1, viewWidth, viewHeight, linePaint)
    }
}

private fun drawLine(
    canvas: Canvas,
    landmarks: List<com.example.handspeak.data.model.HandLandmark>,
    startIndex: Int,
    endIndex: Int,
    viewWidth: Float,
    viewHeight: Float,
    paint: Paint
) {
    if (startIndex < landmarks.size && endIndex < landmarks.size) {
        val start = landmarks[startIndex]
        val end = landmarks[endIndex]
        canvas.drawLine(
            start.x * viewWidth,
            start.y * viewHeight,
            end.x * viewWidth,
            end.y * viewHeight,
            paint
        )
    }
}


