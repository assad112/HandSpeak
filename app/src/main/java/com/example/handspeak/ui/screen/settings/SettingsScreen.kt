package com.example.handspeak.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.handspeak.navigation.Screen
import com.example.handspeak.ui.components.MainBottomBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance Section
            SettingsSection(title = "المظهر") {
                SettingsSwitchItem(
                    title = "الوضع الليلي",
                    description = "تفعيل المظهر الداكن",
                    icon = Icons.Default.DarkMode,
                    checked = uiState.darkMode,
                    onCheckedChange = { viewModel.setDarkMode(it) }
                )
            }
            
            // Animation Section
            SettingsSection(title = "الرسوم المتحركة") {
                SettingsSliderItem(
                    title = "سرعة الرسوم المتحركة",
                    description = "التحكم في سرعة عرض الإشارات",
                    icon = Icons.Default.Speed,
                    value = uiState.animationSpeed,
                    valueRange = 0.5f..2f,
                    steps = 2,
                    onValueChange = { viewModel.setAnimationSpeed(it) },
                    valueLabel = { "×${String.format("%.1f", it)}" }
                )
            }
            
            // Detection Section
            SettingsSection(title = "الكشف والتعرف") {
                SettingsSliderItem(
                    title = "حد الثقة",
                    description = "الحد الأدنى للثقة لقبول الإشارة",
                    icon = Icons.Default.Analytics,
                    value = uiState.confidenceThreshold,
                    valueRange = 0.5f..0.9f,
                    steps = 3,
                    onValueChange = { viewModel.setConfidenceThreshold(it) },
                    valueLabel = { "${(it * 100).toInt()}%" }
                )
                
                SettingsSwitchItem(
                    title = "عرض نسبة الثقة",
                    description = "إظهار نسبة الثقة مع النتائج",
                    icon = Icons.Default.Percent,
                    checked = uiState.showConfidence,
                    onCheckedChange = { viewModel.setShowConfidence(it) }
                )
            }
            
            // Audio Section
            SettingsSection(title = "الصوت") {
                SettingsSwitchItem(
                    title = "تفعيل الصوت",
                    description = "تشغيل الأصوات والإشعارات الصوتية",
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    checked = uiState.enableSound,
                    onCheckedChange = { viewModel.setEnableSound(it) }
                )
            }
            
            // Images Section
            SettingsSection(title = "الصور") {
                SettingsItem(
                    title = "تحميل الصور",
                    description = "حمّل الصور من الإنترنت واحفظها محلياً",
                    icon = Icons.Default.Download,
                    onClick = { navController.navigate(Screen.ImageDownload.route) }
                )
            }
            
            // AI Learning Section
            SettingsSection(title = "التعلم التكيفي") {
                SettingsSwitchItem(
                    title = "تفعيل التعلم التكيفي",
                    description = "التطبيق يتعلم من استخدامك لتحسين الدقة",
                    icon = Icons.Default.AutoAwesome,
                    checked = uiState.enableAdaptiveLearning,
                    onCheckedChange = { viewModel.setEnableAdaptiveLearning(it) }
                )
                
                SettingsItem(
                    title = "إحصائيات التعلم",
                    description = "عرض عدد العينات المحفوظة: ${uiState.learningSampleCount}",
                    icon = Icons.Default.Analytics,
                    onClick = { navController.navigate(Screen.LearningStats.route) }
                )
            }
            
            // AI Model Section
            SettingsSection(title = "نموذج الذكاء الاصطناعي") {
                SettingsSwitchItem(
                    title = "استخدام LSTM",
                    description = if (uiState.useLSTM) "معالجة تسلسل إطارات (دقة أعلى)" else "معالجة إطار واحد (أسرع)",
                    icon = Icons.Default.Memory,
                    checked = uiState.useLSTM,
                    onCheckedChange = { viewModel.setUseLSTM(it) }
                )
            }
            
            // About Section
            SettingsSection(title = "حول التطبيق") {
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "HandSpeak",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "الإصدار 1.0",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        HorizontalDivider()
                        
                        Text(
                            text = "تطبيق HandSpeak هو مترجم ذكي بين اللغة العربية ولغة الإشارة العربية، مصمم لتسهيل التواصل بين السامعين وذوي الإعاقة السمعية.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            
            // Reset Button
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إعادة تعيين الإعدادات")
            }
        }
        
        // Reset confirmation dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("إعادة تعيين الإعدادات") },
                text = { Text("هل أنت متأكد من إعادة تعيين جميع الإعدادات إلى القيم الافتراضية؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetToDefaults()
                            showResetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("إعادة تعيين")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsSliderItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    valueLabel: (Float) -> String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Text(
                text = valueLabel(value),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


