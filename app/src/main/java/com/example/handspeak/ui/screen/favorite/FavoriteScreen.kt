package com.example.handspeak.ui.screen.favorite

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.handspeak.data.model.FavoriteItem
import com.example.handspeak.data.model.SignInfo
import com.example.handspeak.util.FavoriteManager
import com.example.handspeak.util.ImageHelper
import com.example.handspeak.util.JsonHelper
import com.example.handspeak.ui.components.MainBottomBar
import androidx.compose.material.icons.filled.Delete

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(navController: NavController) {
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<FavoriteItem>>(emptyList()) }
    val signMap = remember { JsonHelper.loadSignMap(context) }
    
    LaunchedEffect(Unit) {
        items = FavoriteManager.getFavorites(context)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorite") }
            )
        },
        bottomBar = { MainBottomBar(navController) }
    ) { padding ->
        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "لا توجد عناصر مفضلة بعد",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "احفظ صورة الإشارة من صفحة نص إلى إشارة لعرضها هنا.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items.forEach { item ->
                    // تقسيم الكلمة إلى أحرف فريدة بدون تكرار
                    val uniqueLetters = remember(item.label, signMap) {
                        val letters = mutableListOf<Pair<Char, SignInfo>>()
                        val seenChars = mutableSetOf<String>() // استخدام String بدلاً من Char
                        val seenFolders = mutableSetOf<String>() // لتجنب تكرار الصور
                        
                        item.label.forEach { ch ->
                            val charStr = ch.toString()
                            // التحقق من أن الحرف ليس مسافة وأنه لم يُعرض من قبل
                            if (!ch.isWhitespace() && !seenChars.contains(charStr)) {
                                seenChars.add(charStr)
                                val signInfo = signMap[charStr]
                                if (signInfo != null && signInfo.folder != null) {
                                    // التحقق من عدم تكرار نفس المجلد
                                    if (!seenFolders.contains(signInfo.folder)) {
                                        seenFolders.add(signInfo.folder)
                                        letters.add(Pair(ch, signInfo))
                                    }
                                }
                            }
                        }
                        // تأكيد إضافي لإزالة التكرار
                        letters.distinctBy { "${it.first}_${it.second.folder}" }
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // العنوان وزر الحذف
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                            Text(
                                text = item.label,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            IconButton(onClick = {
                                FavoriteManager.removeFavorite(context, item)
                                items = FavoriteManager.getFavorites(context)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف"
                                    )
                                }
                            }
                            
                            // عرض صور الأحرف
                            if (uniqueLetters.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(uniqueLetters) { (char, signInfo) ->
                                        LetterImageCard(
                                            char = char,
                                            signInfo = signInfo,
                                            context = context
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "لا توجد صور متاحة للأحرف",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LetterImageCard(
    char: Char,
    signInfo: SignInfo,
    context: android.content.Context
) {
    val folder = signInfo.folder
    val imagePath = remember(folder) {
        if (folder != null) {
            ImageHelper.getImagePath(context, folder, 1)
        } else null
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.width(80.dp)
    ) {
        if (imagePath != null) {
            Card(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imagePath)
                        .build(),
                    contentDescription = char.toString(),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Text(
            text = char.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


