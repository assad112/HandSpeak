package com.example.handspeak.ui.screen.voicetosign

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.*
import com.example.handspeak.ui.components.MainBottomBar
import com.example.handspeak.data.model.SignInfo

data class SignImageItem(
    val signInfo: SignInfo,
    val imageIndex: Int,
    val folder: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class, ExperimentalFoundationApi::class)
@Composable
fun VoiceToSignScreen(
    navController: NavController,
    viewModel: VoiceToSignViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    
    val speechRecognizer = remember {
        SpeechRecognizer.createSpeechRecognizer(context)
    }
    
    var isRecognizerBusy by remember { mutableStateOf(false) }
    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    
    // Helper function to start listening
    fun startListeningInternal(recognizer: SpeechRecognizer) {
        if (!audioPermissionState.status.isGranted) {
            return
        }
        
        try {
            // Cancel any ongoing recognition
            try {
                recognizer.cancel()
            } catch (e: Exception) {
                // Ignore if already stopped
            }
            
            // Wait a bit before starting new recognition
            handler.postDelayed({
                try {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar")
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar")
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    }
                    recognizer.startListening(intent)
                } catch (e: Exception) {
                    viewModel.onSpeechError("خطأ في بدء التعرف على الكلام: ${e.message}")
                }
            }, 200)
        } catch (e: Exception) {
            viewModel.onSpeechError("خطأ: ${e.message}")
        }
    }
    
    DisposableEffect(Unit) {
        val recognitionListener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                viewModel.setListening(true)
                isRecognizerBusy = true
            }
            
            override fun onBeginningOfSpeech() {
                // Speech detected
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Audio level changed
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Buffer received
            }
            
            override fun onEndOfSpeech() {
                viewModel.setListening(false)
            }
            
            override fun onError(error: Int) {
                isRecognizerBusy = false
                viewModel.setListening(false)
                
                when (error) {
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                        // Retry after a short delay - don't show error
                        handler.postDelayed({
                            if (audioPermissionState.status.isGranted) {
                                startListeningInternal(speechRecognizer)
                            }
                        }, 500)
                        return
                    }
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        // This is normal - user didn't speak
                        viewModel.onSpeechError("لم يتم الكشف عن كلام. حاول مرة أخرى.")
                    }
                    SpeechRecognizer.ERROR_NO_MATCH -> {
                        viewModel.onSpeechError("لم يتم العثور على تطابق. حاول مرة أخرى.")
                    }
                    SpeechRecognizer.ERROR_AUDIO -> {
                        viewModel.onSpeechError("خطأ في التسجيل الصوتي")
                    }
                    SpeechRecognizer.ERROR_CLIENT -> {
                        viewModel.onSpeechError("خطأ من جانب العميل")
                    }
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        viewModel.onSpeechError("لا يوجد إذن للميكروفون")
                    }
                    SpeechRecognizer.ERROR_NETWORK -> {
                        viewModel.onSpeechError("خطأ في الشبكة. تحقق من اتصال الإنترنت.")
                    }
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                        viewModel.onSpeechError("انتهت مهلة الشبكة. حاول مرة أخرى.")
                    }
                    SpeechRecognizer.ERROR_SERVER -> {
                        viewModel.onSpeechError("خطأ في الخادم. حاول مرة أخرى.")
                    }
                    else -> {
                        viewModel.onSpeechError("خطأ غير معروف: $error")
                    }
                }
            }
            
            override fun onResults(results: Bundle?) {
                isRecognizerBusy = false
                viewModel.setListening(false)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    viewModel.onSpeechResult(matches[0])
                } else {
                    viewModel.onSpeechError("لم يتم التعرف على أي كلام")
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                // Partial results received
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                // Event received
            }
        }
        
        speechRecognizer.setRecognitionListener(recognitionListener)
        
        onDispose {
            try {
                speechRecognizer.cancel()
            } catch (e: Exception) {
                // Ignore errors during cleanup
            }
            handler.removeCallbacksAndMessages(null)
            speechRecognizer.destroy()
        }
    }
    
    fun startListening() {
        if (isRecognizerBusy) {
            // Already listening, don't start again
            return
        }
        
        if (audioPermissionState.status.isGranted) {
            startListeningInternal(speechRecognizer)
        } else {
            audioPermissionState.launchPermissionRequest()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("صوت إلى إشارة") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        bottomBar = { MainBottomBar(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // Microphone button
            if (audioPermissionState.status.isGranted) {
                MicrophoneButton(
                    isListening = uiState.isListening,
                    onClick = { startListening() }
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "يحتاج التطبيق إلى إذن الميكروفون",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { audioPermissionState.launchPermissionRequest() }) {
                        Text("منح الإذن")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Recognized text - تصميم مضغوط (أصغر)
            if (uiState.recognizedText.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 4.dp,
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF667EEA),
                                        Color(0xFF764BA2)
                                    )
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(vertical = 10.dp, horizontal = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(20.dp)
                                )
                        Text(
                            text = "تم التعرف على:",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.95f)
                        )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = uiState.recognizedText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Show message if listening - تصميم احترافي
            if (uiState.isListening) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 8.dp,
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF4FACFE),
                                        Color(0xFF00F2FE)
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "جاري الاستماع...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                        Text(
                                    text = "تحدث الآن",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
            }
            
            // Sign display (single or sequence)
            AnimatedVisibility(visible = uiState.signInfo != null || (uiState.signSequence?.isNotEmpty() == true)) {
                uiState.signInfo?.let { signInfo ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "إشارة: ${signInfo.label}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Simple sign display placeholder
                            Icon(
                                imageVector = Icons.Default.PanTool,
                                contentDescription = "Sign",
                                modifier = Modifier.size(200.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "عرض الإشارة لـ \"${signInfo.label}\"",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } ?: run {
                    val sequence = uiState.signSequence ?: emptyList()
                    
                    if (sequence.isNotEmpty()) {
                        // استخدام slider للأحرف/الكلمات - عرض حرف/كلمة واحدة في كل مرة
                        var currentSignIndex by remember { mutableIntStateOf(0) }
                        val currentSign = sequence[currentSignIndex]
                        val folder = currentSign.folder
                        
                        if (folder != null) {
                            // الحصول على أول صورة فقط لكل حرف/كلمة
                            val currentSignImages = remember(currentSignIndex, folder, context) {
                                val imagePaths = com.example.handspeak.util.ImageHelper.getImagePaths(context, folder)
                                if (imagePaths.isNotEmpty()) {
                                    listOf(
                                    SignImageItem(
                                        signInfo = currentSign,
                                            imageIndex = 0, // عرض صورة واحدة لكل حرف
                                        folder = folder
                                        )
                                    )
                                } else emptyList()
                            }
                            
                            if (currentSignImages.isNotEmpty()) {
                                val pagerState = rememberPagerState(
                                    initialPage = 0,
                                    pageCount = { currentSignImages.size }
                                )
                                
                                // إعادة تعيين الصفحة إلى الأولى عند تغيير الحرف/الكلمة
                                LaunchedEffect(currentSignIndex) {
                                    pagerState.animateScrollToPage(0)
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                // Image Slider للصور الحالية فقط
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    val imageItem = currentSignImages[page]
                                    val imagePath = com.example.handspeak.util.ImageHelper.getImagePath(
                                        LocalContext.current,
                                        imageItem.folder,
                                        imageItem.imageIndex + 1
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(0.95f),
                                            shape = RoundedCornerShape(28.dp),
                                            elevation = CardDefaults.cardElevation(
                                                defaultElevation = 10.dp,
                                                pressedElevation = 6.dp
                                            ),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color.White
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.White)
                                                    .padding(12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(imagePath)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = imageItem.signInfo.label,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // معلومات الحرف/الكلمة الحالية
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 8.dp),
                                    shape = RoundedCornerShape(22.dp),
                                    color = Color.White,
                                    shadowElevation = 4.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = currentSign.label,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF667EEA)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF667EEA)
                                        ) {
                                            Text(
                                                text = "${currentSignIndex + 1} / ${sequence.size}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                                
                                // Dots Indicator وزر التشغيل/الإيقاف
                                // عرض النقاط حسب عدد الأحرف في الكلمة (بدون تكرار صور الحرف)
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 12.dp),
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.White.copy(alpha = 0.9f),
                                        shadowElevation = 4.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                        // نقاط المؤشر حسب عدد الأحرف
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                            repeat(sequence.size) { index ->
                                                val isSelected = currentSignIndex == index
                                                    Surface(
                                                        modifier = Modifier
                                                            .size(if (isSelected) 12.dp else 8.dp),
                                                        shape = CircleShape,
                                                        color = if (isSelected) {
                                                            Color(0xFF667EEA)
                                    } else {
                                                            Color(0xFFB0B0B0).copy(alpha = 0.5f)
                                                        },
                                                        shadowElevation = if (isSelected) 4.dp else 0.dp
                                                    ) {}
                                            }
                                        }
                                    }
                                }
                                
                                // أزرار التنقل بين الأحرف/الكلمات
                                if (sequence.size > 1) {
                                    val coroutineScope = rememberCoroutineScope()
                                    
                                    // زر السابق
                                    FloatingActionButton(
                                        onClick = {
                                            if (currentSignIndex > 0) {
                                                currentSignIndex--
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(0)
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(start = 8.dp),
                                        containerColor = Color(0xFF667EEA),
                                        contentColor = Color.White
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "السابق",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    
                                    // زر التالي
                                    FloatingActionButton(
                                        onClick = {
                                            if (currentSignIndex < sequence.size - 1) {
                                                currentSignIndex++
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(0)
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(end = 8.dp),
                                        containerColor = Color(0xFF667EEA),
                                        contentColor = Color.White
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowForward,
                                            contentDescription = "التالي",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "لا توجد صور متاحة",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "لا يوجد مجلد للصور",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد إشارات",
                                style = MaterialTheme.typography.bodyLarge
                            )
                    }
                    }
                }
            }
        }
        
        // Error dialog
        uiState.errorMessage?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("تنبيه") },
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
fun MicrophoneButton(
    isListening: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_animation"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier
                .size(92.dp)
                .scale(if (isListening) scale else 1f),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isListening) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Microphone",
                modifier = Modifier.size(48.dp),
                tint = if (isListening) 
                    MaterialTheme.colorScheme.onError 
                else 
                    MaterialTheme.colorScheme.onPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = if (isListening) "جاري الاستماع..." else "اضغط للتحدث",
            style = MaterialTheme.typography.bodyLarge,
            color = if (isListening) 
                MaterialTheme.colorScheme.error 
            else 
                MaterialTheme.colorScheme.primary
        )
    }
}


