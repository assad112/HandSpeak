package com.example.handspeak.ui.screen.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.handspeak.ui.components.MainBottomBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        bottomBar = { MainBottomBar(navController) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8F9FA),
                            Color(0xFFE9ECEF)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
                // Account Card
                ModernSettingCard(
                icon = Icons.Default.AccountCircle,
                    iconGradient = listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
                    title = "Account",
                    subtitle = "Manage your account information",
                    rightIcon = Icons.Default.ChevronRight,
                onClick = { navController.navigate(Screen.Account.route) }
            )
            
                // Dark Mode Card
                ModernSettingCard(
                icon = Icons.Default.NightsStay,
                    iconGradient = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),
                    title = "Dark Mode",
                    subtitle = if (uiState.darkMode) "Enabled" else "Disabled",
                trailing = {
                    Switch(
                        checked = uiState.darkMode,
                            onCheckedChange = { viewModel.setDarkMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF667EEA),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFBDBDBD)
                            )
                    )
                }
            )
            
                // LSTM Mode Card
                ModernSettingCard(
                icon = Icons.Default.Memory,
                    iconGradient = listOf(Color(0xFF9C27B0), Color(0xFFE91E63)),
                    title = "LSTM Mode",
                    subtitle = if (uiState.useLSTM) "Advanced recognition (slower)" else "Fast recognition (Dense)",
                trailing = {
                    Switch(
                        checked = uiState.useLSTM,
                            onCheckedChange = { viewModel.setUseLSTM(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF9C27B0),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFBDBDBD)
                            )
                    )
                }
            )
            
                // Favorite Card
                ModernSettingCard(
                icon = Icons.Default.FavoriteBorder,
                    iconGradient = listOf(Color(0xFFFF6B6B), Color(0xFFFF8E8E)),
                    title = "Favorite",
                    subtitle = "View saved items",
                    rightIcon = Icons.Default.ChevronRight,
                onClick = { navController.navigate(Screen.Favorites.route) }
            )
            
                // About Card
                ModernSettingCard(
                icon = Icons.Default.Info,
                    iconGradient = listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
                    title = "About",
                    subtitle = "App info & tutorial video",
                    rightIcon = Icons.Default.ChevronRight,
                onClick = { navController.navigate(Screen.About.route) }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
                // Logout Card
                ModernSettingCard(
                icon = Icons.Default.Logout,
                    iconGradient = listOf(Color(0xFFFF6B6B), Color(0xFFFF5252)),
                    title = "Log out",
                    subtitle = "Sign out from your account",
                    rightIcon = Icons.Default.ChevronRight,
                isDestructive = true,
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
private fun ModernSettingCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconGradient: List<Color>,
    title: String,
    subtitle: String? = null,
    rightIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    isDestructive: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "card_scale"
    )

    val textColor = if (isDestructive) {
        Color(0xFFFF5252)
    } else {
        Color(0xFF1A1A1A)
    }

    val subtitleColor = if (isDestructive) {
        Color(0xFFFF5252).copy(alpha = 0.7f)
    } else {
        Color(0xFF757575)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .then(if (onClick != null) Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ) else Modifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left icon with gradient background
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(iconGradient)),
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

            // Center text
            Column(
                modifier = Modifier.weight(1f)
            ) {
                    Text(
                        text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                        color = textColor
                    )
                    if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor
                        )
                }
            }
            
            // Right icon or trailing
            if (trailing != null) {
                trailing()
            } else if (rightIcon != null) {
                Icon(
                    imageVector = rightIcon,
                    contentDescription = null,
                    tint = if (isDestructive) Color(0xFFFF5252) else Color(0xFF9E9E9E),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
