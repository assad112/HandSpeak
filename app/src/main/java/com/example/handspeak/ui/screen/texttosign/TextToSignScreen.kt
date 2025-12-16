package com.example.handspeak.ui.screen.texttosign

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.handspeak.data.model.FavoriteItem
import com.example.handspeak.data.model.SignInfo
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

// Data class for image items
private data class SignImageItem(
    val signInfo: SignInfo,
    val imageIndex: Int,
    val folder: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SignSequenceGrid(
    sequence: List<SignInfo>,
    onFavorite: (String, String) -> Unit
) {
    val context = LocalContext.current
    
    if (sequence.isNotEmpty()) {
        // استخدام slider للأحرف/الكلمات - عرض حرف/كلمة واحدة في كل مرة
        var currentSignIndex by remember { mutableIntStateOf(0) }
        val currentSign = sequence[currentSignIndex]
        val folder = currentSign.folder
        
        if (folder != null) {
            // الحصول على أول صورة فقط لكل حرف/كلمة
            val currentSignImages = remember(currentSignIndex, folder, context) {
                val imagePaths = ImageHelper.getImagePaths(context, folder)
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
                        .height(500.dp)
                ) {
                    // Image Slider للصور الحالية فقط
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val imageItem = currentSignImages[page]
                        val imagePath = ImageHelper.getImagePath(
                            context,
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
                                        model = ImageRequest.Builder(context)
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
                            // Favorite button
                            IconButton(
                                onClick = { onFavorite(currentSign.label, folder) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.FavoriteBorder,
                                    contentDescription = "حفظ في المفضلة",
                                    tint = Color(0xFF667EEA)
                                )
                            }
                        }
                    }
                    
                    // Dots Indicator وزر التشغيل/الإيقاف
                    // عرض النقاط حسب عدد الأحرف في الكلمة
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
                        .height(500.dp),
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
                    .height(500.dp),
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
                .height(500.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "لا توجد إشارات متاحة",
                style = MaterialTheme.typography.bodyLarge
            )
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
            if (imageCount > 0) {
                // Load image directly from assets as Bitmap (Coil doesn't support file:///android_asset/ URIs well)
                val bitmap = remember(currentImageIndex, folder) {
                    ImageHelper.loadImage(context, folder, currentImageIndex + 1)
                }
                
                if (bitmap != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(bitmap)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Sign image ${currentImageIndex + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                } else {
                    // Fallback to icon if image fails to load
                    Icon(
                        imageVector = Icons.Default.PanTool,
                        contentDescription = "Sign image error",
                        modifier = Modifier.size(200.dp),
                        tint = MaterialTheme.colorScheme.primary
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


