package com.example.handspeak.ui.screen.voicetosign

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
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
import java.util.*
import com.example.handspeak.ui.components.MainBottomBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
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
            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "اضغط على الميكروفون وتحدث بالعربية لترجمة كلامك إلى لغة الإشارة",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
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
            
            // Recognized text - always show if not empty
            if (uiState.recognizedText.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "تم التعرف على:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.recognizedText,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Show message if listening
            if (uiState.isListening) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "جاري الاستماع... تحدث الآن",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            items(sequence) { si ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = si.label,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    val folder = si.folder
                                    if (folder != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(com.example.handspeak.util.ImageHelper.getImagePath(LocalContext.current, folder, 1))
                                                .build(),
                                            contentDescription = si.label,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(140.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PanTool,
                                            contentDescription = null,
                                            modifier = Modifier.size(100.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
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
                .size(120.dp)
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
                modifier = Modifier.size(64.dp),
                tint = if (isListening) 
                    MaterialTheme.colorScheme.onError 
                else 
                    MaterialTheme.colorScheme.onPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (isListening) "جاري الاستماع..." else "اضغط للتحدث",
            style = MaterialTheme.typography.titleMedium,
            color = if (isListening) 
                MaterialTheme.colorScheme.error 
            else 
                MaterialTheme.colorScheme.primary
        )
    }
}


