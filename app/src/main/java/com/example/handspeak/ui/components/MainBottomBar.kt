package com.example.handspeak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.handspeak.navigation.Screen

@Composable
fun MainBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    NavigationBar(
        containerColor = Color(0xFFE0E0E0),
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == Screen.Home.route,
            onClick = { if (currentRoute != Screen.Home.route) navController.navigate(Screen.Home.route) },
            icon = {
                if (currentRoute == Screen.Home.route) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFC0C0C0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = Color.Black
                    )
                }
            }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Learn.route,
            onClick = { if (currentRoute != Screen.Learn.route) navController.navigate(Screen.Learn.route) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = "Learn",
                    tint = Color.Black
                )
            }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.SignToText.route,
            onClick = { if (currentRoute != Screen.SignToText.route) navController.navigate(Screen.SignToText.route) },
            icon = {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera",
                    tint = Color.Black
                )
            }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.VoiceToSign.route,
            onClick = { if (currentRoute != Screen.VoiceToSign.route) navController.navigate(Screen.VoiceToSign.route) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Mic",
                    tint = Color.Black
                )
            }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.TextToSign.route,
            onClick = { if (currentRoute != Screen.TextToSign.route) navController.navigate(Screen.TextToSign.route) },
            icon = {
                Icon(
                    imageVector = Icons.Default.TextFields,
                    contentDescription = "Text",
                    tint = Color.Black
                )
            }
        )
    }
}



