package com.example.handspeak.navigation

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.handspeak.data.repository.AuthRepository
import com.example.handspeak.ui.screen.auth.LoginScreen
import com.example.handspeak.ui.screen.auth.SignUpScreen
import com.example.handspeak.ui.screen.history.HistoryScreen
import com.example.handspeak.ui.screen.home.HomeScreen
import com.example.handspeak.ui.screen.favorite.FavoriteScreen
import com.example.handspeak.ui.screen.settings.ImageDownloadSettingsScreen
import com.example.handspeak.ui.screen.settings.LearningStatsScreen
import com.example.handspeak.ui.screen.settings.SettingsScreen
import com.example.handspeak.ui.screen.signtotext.SignToTextScreen
import com.example.handspeak.ui.screen.texttosign.TextToSignScreen
import com.example.handspeak.ui.screen.voicetosign.VoiceToSignScreen
import com.example.handspeak.ui.screen.account.AccountScreen
import com.example.handspeak.ui.screen.splash.SplashScreen
import androidx.compose.ui.platform.LocalContext

@Composable
fun NavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    var isFirstLaunch by remember { 
        mutableStateOf(prefs.getBoolean("is_first_launch", true))
    }
    
    val authRepository = AuthRepository()
    val isSignedIn = authRepository.isUserSignedIn()
    
    val startDestination = if (isFirstLaunch) {
        Screen.Splash.route
    } else if (isSignedIn) {
        Screen.Home.route
    } else {
        Screen.Login.route
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                navController = navController,
                onNavigateToNext = {
                    // Mark first launch as complete
                    prefs.edit().putBoolean("is_first_launch", false).apply()
                    isFirstLaunch = false
                    
                    // Navigate to appropriate screen
                    if (isSignedIn) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        
        composable(Screen.SignUp.route) {
            SignUpScreen(navController)
        }
        
        composable(Screen.Home.route) {
            com.example.handspeak.ui.screen.home.BasicMainScreen(navController)
        }
        
        composable(Screen.SignToText.route) {
            SignToTextScreen(navController)
        }
        
        composable(Screen.TextToSign.route) {
            TextToSignScreen(navController)
        }
        
        composable(Screen.VoiceToSign.route) {
            VoiceToSignScreen(navController)
        }
        
        composable(Screen.History.route) {
            HistoryScreen(navController)
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(navController)
        }
        
        composable(Screen.Favorites.route) {
            FavoriteScreen(navController)
        }
        
        composable(Screen.ImageDownload.route) {
            ImageDownloadSettingsScreen(navController)
        }
        
        composable(Screen.LearningStats.route) {
            LearningStatsScreen(navController)
        }
        
        composable(Screen.Account.route) {
            AccountScreen(navController)
        }
    }
}


