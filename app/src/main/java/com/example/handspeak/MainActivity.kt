package com.example.handspeak

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.handspeak.navigation.NavGraph
import com.example.handspeak.ui.theme.HandSpeakTheme
import android.content.SharedPreferences

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate called")
        
        setContent {
            Log.d(TAG, "setContent called")
            // Observe dark mode preference and update theme dynamically
            val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
            var isDark by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
            DisposableEffect(Unit) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == "dark_mode") {
                        isDark = prefs.getBoolean("dark_mode", false)
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            
            HandSpeakTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    Log.d(TAG, "NavController created")
                    NavGraph(navController = navController)
                    Log.d(TAG, "NavGraph created")
                }
            }
        }
        Log.d(TAG, "setContent completed")
    }
    
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "MainActivity onStart called")
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MainActivity onResume called")
    }
}


