package com.example.handspeak.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.handspeak.ml.AdaptiveLearningHelper
import kotlinx.coroutines.launch

/**
 * شاشة إحصائيات التعلم التكيفي
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningStatsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var stats by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var totalSamples by remember { mutableStateOf(0) }
    var csvFileSize by remember { mutableStateOf(0L) }
    var showClearDialog by remember { mutableStateOf(false) }
    
    // تحديث الإحصائيات
    LaunchedEffect(Unit) {
        stats = AdaptiveLearningHelper.getLearningStats(context)
        totalSamples = AdaptiveLearningHelper.getTotalSampleCount(context)
        csvFileSize = AdaptiveLearningHelper.getCsvFileSize(context)
    }
    
    fun refreshStats() {
        scope.launch {
            stats = AdaptiveLearningHelper.getLearningStats(context)
            totalSamples = AdaptiveLearningHelper.getTotalSampleCount(context)
            csvFileSize = AdaptiveLearningHelper.getCsvFileSize(context)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إحصائيات التعلم") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshStats() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // إحصائيات عامة
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "إحصائيات التعلم",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "إجمالي العينات:",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "$totalSamples",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "حجم البيانات:",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "${String.format("%.2f", csvFileSize / 1024.0)} KB",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // قائمة التصنيفات
            if (stats.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "العينات حسب التصنيف",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        HorizontalDivider()
                        
                        // عرض التصنيفات مرتبة حسب العدد
                        stats.toList()
                            .sortedByDescending { it.second }
                            .forEach { (label, count) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { count.toFloat() / MAX_SAMPLES_PER_LABEL },
                                            modifier = Modifier.width(100.dp)
                                        )
                                        Text(
                                            text = "$count/$MAX_SAMPLES_PER_LABEL",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "لا توجد بيانات تعلم بعد",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "استخدم التطبيق لبدء جمع البيانات",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // زر حذف البيانات
            if (totalSamples > 0) {
                OutlinedButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حذف جميع بيانات التعلم")
                }
            }
            
            // معلومات
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
                        text = "💡 معلومات",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• التطبيق يحفظ العينات تلقائياً عند التعرف",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• الحد الأقصى: 100 عينة لكل تصنيف",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• البيانات تُحفظ في CSV للتدريب لاحقاً",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
    
    // Dialog حذف البيانات
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("حذف بيانات التعلم") },
            text = { Text("هل أنت متأكد من حذف جميع بيانات التعلم؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                Button(
                    onClick = {
                        AdaptiveLearningHelper.clearTrainingData(context)
                        refreshStats()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

private const val MAX_SAMPLES_PER_LABEL = 100










