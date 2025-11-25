package com.example.handspeak.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object SignToText : Screen("sign_to_text")
    object TextToSign : Screen("text_to_sign")
    object VoiceToSign : Screen("voice_to_sign")
    object History : Screen("history")
    object Settings : Screen("settings")
    object ImageDownload : Screen("image_download")
    object LearningStats : Screen("learning_stats")
    object Favorites : Screen("favorites")
    object Account : Screen("account")
}


