package com.example.handspeak.navigation

import android.content.Context
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.handspeak.data.repository.AuthRepository
import com.example.handspeak.ui.screen.auth.LoginScreen
import com.example.handspeak.ui.screen.auth.SignUpScreen
import com.example.handspeak.ui.screen.home.HomeScreen
import com.example.handspeak.ui.screen.home.HomeScreenNew
import com.example.handspeak.ui.screen.favorite.FavoriteScreen
import com.example.handspeak.ui.screen.history.HistoryScreen
import com.example.handspeak.ui.screen.settings.ImageDownloadSettingsScreen
import com.example.handspeak.ui.screen.settings.LearningStatsScreen
import com.example.handspeak.ui.screen.settings.SettingsScreen
import com.example.handspeak.ui.screen.signtotext.SignToTextScreen
import com.example.handspeak.ui.screen.texttosign.TextToSignScreen
import com.example.handspeak.ui.screen.voicetosign.VoiceToSignScreen
import com.example.handspeak.ui.screen.learn.LearnScreen
import com.example.handspeak.ui.screen.account.AccountScreen
import com.example.handspeak.ui.screen.about.AboutScreen
import androidx.compose.ui.platform.LocalContext

@Composable
fun NavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    // Mark first launch as complete if not already done
    LaunchedEffect(Unit) {
        if (prefs.getBoolean("is_first_launch", true)) {
            prefs.edit().putBoolean("is_first_launch", false).apply()
        }
    }
    
    val authRepository = AuthRepository()
    val isSignedIn = authRepository.isUserSignedIn()
    
    val startDestination = if (isSignedIn) {
        Screen.Home.route
    } else {
        Screen.Login.route
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        
        composable(Screen.SignUp.route) {
            SignUpScreen(navController)
        }
        
        composable(Screen.Home.route) {
            com.example.handspeak.ui.screen.home.HomeScreenNew(navController)
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
        
        composable(Screen.Learn.route) {
            LearnScreen(navController)
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
        
        composable(Screen.History.route) {
            HistoryScreen(navController)
        }
        
        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}


