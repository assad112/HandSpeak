package com.example.handspeak.ui.screen.learn

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.handspeak.ui.components.MainBottomBar
import com.example.handspeak.util.ImageHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    navController: NavController,
    viewModel: LearnViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تعلم لغة الإشارة") },
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
                .padding(16.dp)
        ) {
            // Instructions Card
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
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "ابحث عن أي حرف أو كلمة لتتعلم كيفية أدائها بلغة الإشارة",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ابحث عن حرف أو كلمة") },
                placeholder = { Text("مثال: أ، مرحبا، شكرا") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Quick Access Buttons
            if (uiState.searchQuery.isEmpty()) {
                Text(
                    text = "اختيارات سريعة:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedCategory == "letters",
                        onClick = { viewModel.selectCategory("letters") },
                        label = { Text("الحروف") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.TextFields,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    
                    FilterChip(
                        selected = uiState.selectedCategory == "words",
                        onClick = { viewModel.selectCategory("words") },
                        label = { Text("الكلمات") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    
                    FilterChip(
                        selected = uiState.selectedCategory == "all",
                        onClick = { viewModel.selectCategory("all") },
                        label = { Text("الكل") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Results
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                uiState.errorMessage != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = uiState.errorMessage ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
                uiState.searchResults.isEmpty() && uiState.searchQuery.isNotEmpty() -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "لم يتم العثور على نتائج",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "جرب البحث عن حرف أو كلمة أخرى",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.searchResults) { result ->
                            SignCard(
                                label = result.label,
                                imagePaths = result.imagePaths,
                                type = result.type,
                                context = context
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SignCard(
    label: String,
    imagePaths: List<String>,
    type: String,
    context: android.content.Context
) {
    var currentImageIndex by remember { mutableIntStateOf(0) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (type == "letter") "حرف" else "كلمة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                AssistChip(
                    onClick = { /* يمكن إضافة وظيفة الحفظ في المفضلة */ },
                    label = { Text("${imagePaths.size} صورة") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Image Display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (imagePaths.isNotEmpty()) {
                        val imagePath = imagePaths[currentImageIndex]
                        val bitmap = remember(imagePath) {
                            ImageHelper.loadImageFromAssets(context, imagePath)
                        }
                        
                        if (bitmap != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(bitmap)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = label,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "لم يتم العثور على الصورة",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "لا توجد صور متاحة",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Navigation Controls (if multiple images)
            if (imagePaths.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            currentImageIndex = if (currentImageIndex > 0) {
                                currentImageIndex - 1
                            } else {
                                imagePaths.size - 1
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "السابق")
                    }
                    
                    Text(
                        text = "${currentImageIndex + 1} / ${imagePaths.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    IconButton(
                        onClick = {
                            currentImageIndex = if (currentImageIndex < imagePaths.size - 1) {
                                currentImageIndex + 1
                            } else {
                                0
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "التالي")
                    }
                }
            }
        }
    }
}

