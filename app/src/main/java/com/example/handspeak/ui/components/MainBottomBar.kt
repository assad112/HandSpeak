package com.example.handspeak.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.handspeak.navigation.Screen

@Composable
fun MainBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Screen.Home.route,
            onClick = { if (currentRoute != Screen.Home.route) navController.navigate(Screen.Home.route) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.History.route,
            onClick = { if (currentRoute != Screen.History.route) navController.navigate(Screen.History.route) },
            icon = { Icon(Icons.Default.Book, contentDescription = "Book") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.SignToText.route,
            onClick = { if (currentRoute != Screen.SignToText.route) navController.navigate(Screen.SignToText.route) },
            icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Camera") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.VoiceToSign.route,
            onClick = { if (currentRoute != Screen.VoiceToSign.route) navController.navigate(Screen.VoiceToSign.route) },
            icon = { Icon(Icons.Default.Mic, contentDescription = "Mic") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.TextToSign.route,
            onClick = { if (currentRoute != Screen.TextToSign.route) navController.navigate(Screen.TextToSign.route) },
            icon = { Icon(Icons.Default.TextFields, contentDescription = "Text") }
        )
    }
}


