package com.example.handspeak.ui.screen.texttosign

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.handspeak.data.model.FavoriteItem
import com.example.handspeak.util.FavoriteManager
import com.example.handspeak.util.ImageHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.example.handspeak.ui.components.MainBottomBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextToSignScreen(
    navController: NavController,
    viewModel: TextToSignViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("نص إلى إشارة") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = { MainBottomBar(navController) }
    ) { padding ->
        val context = LocalContext.current
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
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
                        text = "اكتب كلمة أو جملة عربية وشاهد كيفية أداء الإشارة المقابلة",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            item {
                // Input field
                OutlinedTextField(
                value = uiState.inputText,
                onValueChange = { viewModel.onTextChanged(it) },
                label = { Text("أدخل النص العربي") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3,
                trailingIcon = {
                    if (uiState.inputText.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clear() }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                }
            )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                // Example button
                OutlinedButton(
                    onClick = {
                        viewModel.onTextChanged("تعلم")
                        // Trigger translation after a short delay to ensure text is set
                        scope.launch {
                            delay(100)
                            viewModel.translateToSign()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مثال: تعلم")
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                // Translate button
                Button(
                onClick = { viewModel.translateToSign() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.inputText.isNotEmpty()
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ترجم إلى إشارة")
            }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            item {
                // Sign display - single or sequence
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Sound button
                                IconButton(
                                    onClick = { viewModel.speakText(uiState.inputText.trim()) }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "تشغيل الصوت",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                // Favorite button
                                IconButton(
                                    onClick = {
                                        signInfo.folder?.let { folder ->
                                            FavoriteManager.addFavorite(
                                                context,
                                                FavoriteItem(
                                                    label = signInfo.label ?: uiState.inputText.trim(),
                                                    folder = folder
                                                )
                                            )
                                            scope.launch {
                                                snackbarHostState.showSnackbar("تم الحفظ في المفضلة")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FavoriteBorder,
                                        contentDescription = "حفظ في المفضلة",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            when (signInfo.type) {
                                "images" -> {
                                    signInfo.folder?.let { folder ->
                                        SignImagesPlayer(
                                            folder = folder,
                                            isPlaying = uiState.isPlaying,
                                            onPlayingChanged = { viewModel.setIsPlaying(it) }
                                        )
                                    }
                                }
                                "video" -> {
                                    Text(
                                        text = "عرض الفيديو (قريباً)",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                } ?: run {
                    // Sequence of letters
                    val sequence = uiState.signSequence ?: emptyList()
                    SignSequenceGrid(
                        sequence = sequence,
                        onFavorite = { label, folder ->
                            FavoriteManager.addFavorite(context, FavoriteItem(label, folder))
                            scope.launch { snackbarHostState.showSnackbar("تم حفظ $label في المفضلة") }
                        }
                    )
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
private fun SignSequenceGrid(
    sequence: List<com.example.handspeak.data.model.SignInfo>,
    onFavorite: (String, String) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sequence.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { si ->
                    Card(
                        modifier = Modifier.weight(1f),
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = si.label, style = MaterialTheme.typography.titleMedium)
                                if (si.folder != null) {
                                    IconButton(onClick = { onFavorite(si.label, si.folder) }) {
                                        Icon(Icons.Default.FavoriteBorder, contentDescription = null)
                                    }
                                }
                            }
                            val folder = si.folder
                            if (folder != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(com.example.handspeak.util.ImageHelper.getImagePath(context, folder, 1))
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
                // Fill empty space if odd number of items
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun SignImagesPlayer(
    folder: String,
    isPlaying: Boolean,
    onPlayingChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var currentImageIndex by remember { mutableStateOf(0) }
    
    // Get actual image count from assets
    val imageCount = remember(folder) {
        ImageHelper.getImageCount(context, folder).takeIf { it > 0 } ?: 5
    }
    
    // Auto-play animation
    LaunchedEffect(isPlaying, imageCount) {
        if (isPlaying && imageCount > 0) {
            while (isActive && isPlaying) {
                delay(500) // Change image every 500ms
                currentImageIndex = (currentImageIndex + 1) % imageCount
            }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Image display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            if (imageCount > 0 && ImageHelper.imageExists(context, folder, currentImageIndex + 1)) {
                // Load actual image from assets
                var hasError by remember(currentImageIndex) { mutableStateOf(false) }
                
                if (hasError) {
                    // Fallback to icon if image fails to load
                    Icon(
                        imageVector = Icons.Default.PanTool,
                        contentDescription = "Sign image error",
                        modifier = Modifier.size(200.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(ImageHelper.getImagePath(context, folder, currentImageIndex + 1))
                            .build(),
                        contentDescription = "Sign image ${currentImageIndex + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        onError = {
                            hasError = true
                        }
                    )
                }
            } else {
                // Placeholder when no images found
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PanTool,
                        contentDescription = "Sign image placeholder",
                        modifier = Modifier.size(200.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "لا توجد صور متاحة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "أضف الصور في: assets/signs/$folder/",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            if (imageCount > 0) {
                Text(
                    text = "صورة ${currentImageIndex + 1}/$imageCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Play controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    currentImageIndex = if (currentImageIndex > 0) currentImageIndex - 1 else imageCount - 1
                }
            ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "السابق")
            }
            
            IconButton(
                onClick = { onPlayingChanged(!isPlaying) },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "إيقاف" else "تشغيل",
                    modifier = Modifier.size(48.dp)
                )
            }
            
            IconButton(
                onClick = {
                    currentImageIndex = (currentImageIndex + 1) % imageCount
                }
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "التالي")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Removed extra info text to focus on images only
    }
}


