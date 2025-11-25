package com.example.handspeak.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* stay */ },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.History.route) },
                    icon = { Icon(Icons.Default.Book, contentDescription = "Book") }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SettingRow(
                text = "Account",
                trailing = {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null
                    )
                },
                onClick = { navController.navigate(com.example.handspeak.navigation.Screen.Account.route) }
            )
            SettingRow(
                text = "DarkMod",
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = settingsState.darkMode,
                            onCheckedChange = { settingsViewModel.setDarkMode(it) }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(
                            imageVector = Icons.Default.NightsStay,
                            contentDescription = null
                        )
                    }
                }
            )
            SettingRow(
                text = "Favorite",
                trailing = {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null
                    )
                },
                onClick = { navController.navigate(Screen.Favorites.route) }
            )
            SettingRow(
                text = "Log out",
                textColor = MaterialTheme.colorScheme.error,
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

@Composable
private fun SettingRow(
    text: String,
    trailing: @Composable RowScope.() -> Unit,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .let { base -> if (onClick != null) base.clickable { onClick() } else base }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = text, color = textColor, style = MaterialTheme.typography.titleMedium)
            Row(content = trailing)
        }
    }
}


