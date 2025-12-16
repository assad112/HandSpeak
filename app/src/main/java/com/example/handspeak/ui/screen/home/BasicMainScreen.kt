package com.example.handspeak.ui.screen.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.handspeak.navigation.Screen
import com.example.handspeak.ui.screen.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicMainScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val settingsState by settingsViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "الإعدادات",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* stay */ },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.SignToText.route) },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Camera") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.VoiceToSign.route) },
                    icon = { Icon(Icons.Default.Mic, contentDescription = "Mic") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.TextToSign.route) },
                    icon = { Icon(Icons.Default.TextFields, contentDescription = "Text") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Account Card
                SettingRow(
                    icon = Icons.Default.AccountCircle,
                    text = "الحساب",
                    description = "إدارة معلومات حسابك",
                    iconGradient = listOf(
                        Color(0xFF667EEA),
                        Color(0xFF764BA2)
                    ),
                    trailing = {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = { navController.navigate(com.example.handspeak.navigation.Screen.Account.route) }
                )
                
                // Dark Mode Card
                SettingRow(
                    icon = Icons.Default.NightsStay,
                    text = "الوضع الليلي",
                    description = if (settingsState.darkMode) "مفعل" else "معطل",
                    iconGradient = listOf(
                        Color(0xFF4FACFE),
                        Color(0xFF00F2FE)
                    ),
                    trailing = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Switch(
                                checked = settingsState.darkMode,
                                onCheckedChange = { settingsViewModel.setDarkMode(it) }
                            )
                        }
                    }
                )
                
                // Favorites Card
                SettingRow(
                    icon = Icons.Default.FavoriteBorder,
                    text = "المفضلة",
                    description = "عرض العناصر المحفوظة",
                    iconGradient = listOf(
                        Color(0xFFFF6B6B),
                        Color(0xFFFFE66D)
                    ),
                    trailing = {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = { navController.navigate(Screen.Favorites.route) }
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Log out Card
                SettingRow(
                    icon = Icons.Default.Logout,
                    text = "تسجيل الخروج",
                    description = "الخروج من حسابك",
                    iconGradient = listOf(
                        Color(0xFFF5576C),
                        Color(0xFFF093FB)
                    ),
                    isDestructive = true,
                    trailing = {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    description: String? = null,
    iconGradient: List<Color>,
    trailing: @Composable RowScope.() -> Unit,
    isDestructive: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )
    
    val cardColor = if (isDestructive) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val textColor = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    val interactionSource = remember { MutableInteractionSource() }
    
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isPressed = true
                }
                is PressInteraction.Release -> {
                    isPressed = false
                    onClick?.invoke()
                }
                is PressInteraction.Cancel -> {
                    isPressed = false
                }
                else -> {}
            }
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {}
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 2.dp else 4.dp,
            pressedElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with gradient background
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(iconGradient)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Trailing content
            Row(
                content = trailing,
                verticalAlignment = Alignment.CenterVertically
            )
        }
    }
}


