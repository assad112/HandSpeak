package com.example.handspeak.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*

class TextToSpeechHelper(private val context: Context) {
    
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isInitializing = false
    private var initializationCallback: ((Boolean) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingTexts = mutableListOf<String>()
    
    companion object {
        private const val TAG = "TextToSpeechHelper"
        private const val INIT_TIMEOUT_MS = 5000L
        private const val SPEAK_DELAY_MS = 100L
        private const val RETRY_DELAY_MS = 500L
    }
    
    fun initialize(onInitComplete: (Boolean) -> Unit) {
        // Avoid multiple initializations
        if (isInitializing) {
            Log.w(TAG, "TTS is already initializing, adding callback")
            val existingCallback = initializationCallback
            initializationCallback = { success ->
                existingCallback?.invoke(success)
                onInitComplete(success)
            }
            return
        }
        
        if (isInitialized && tts != null) {
            Log.d(TAG, "TTS already initialized")
            onInitComplete(true)
            return
        }
        
        isInitializing = true
        initializationCallback = onInitComplete
        
        // Shutdown existing instance if any
        if (tts != null) {
            try {
                tts?.stop()
                tts?.shutdown()
            } catch (e: Exception) {
                Log.w(TAG, "Error shutting down existing TTS", e)
            }
            tts = null
        }
        
        try {
            tts = TextToSpeech(context) { status ->
                isInitializing = false
                
                if (status == TextToSpeech.SUCCESS) {
                    // Set up utterance progress listener
                    @Suppress("DEPRECATION")
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            Log.d(TAG, "TTS started: $utteranceId")
                        }
                        
                        override fun onDone(utteranceId: String?) {
                            Log.d(TAG, "TTS completed: $utteranceId")
                        }
                        
                        override fun onError(utteranceId: String?) {
                            Log.e(TAG, "TTS error: $utteranceId")
                        }
                    })
                    
                    // Try to set Arabic language
                    val result = tts?.setLanguage(Locale("ar", "SA"))
                    
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "Arabic language not supported, trying ar locale")
                        val arResult = tts?.setLanguage(Locale("ar"))
                        if (arResult == TextToSpeech.LANG_MISSING_DATA || arResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.w(TAG, "Arabic not available, using default")
                            tts?.setLanguage(Locale.getDefault())
                        }
                    }
                    
                    // Set default speech rate
                    tts?.setSpeechRate(1.0f)
                    
                    isInitialized = true
                    initializationCallback?.invoke(true)
                    initializationCallback = null
                    Log.d(TAG, "TextToSpeech initialized successfully")
                    
                    // Process any pending texts
                    processPendingTexts()
                } else {
                    Log.e(TAG, "TextToSpeech initialization failed with status: $status")
                    isInitialized = false
                    initializationCallback?.invoke(false)
                    initializationCallback = null
                }
            }
        } catch (e: Exception) {
            isInitializing = false
            Log.e(TAG, "Exception during TTS initialization", e)
            isInitialized = false
            initializationCallback?.invoke(false)
            initializationCallback = null
        }
    }
    
    private fun processPendingTexts() {
        if (pendingTexts.isNotEmpty() && isInitialized) {
            val texts = pendingTexts.toList()
            pendingTexts.clear()
            texts.forEach { text ->
                speakInternal(text)
            }
        }
    }
    
    fun speak(text: String, enableSound: Boolean = true) {
        if (!enableSound) {
            Log.d(TAG, "Sound is disabled, skipping speech")
            return
        }
        
        if (text.isBlank()) {
            Log.w(TAG, "Text is blank, skipping speech")
            return
        }
        
        // Wait for initialization if not ready
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TextToSpeech not initialized yet, adding to queue")
            // Add to pending queue
            if (!pendingTexts.contains(text)) {
                pendingTexts.add(text)
            }
            
            // Try to initialize if not already initializing
            if (!isInitializing) {
                initialize { success ->
                    if (!success) {
                        Log.e(TAG, "Failed to initialize TTS, cannot speak")
                        pendingTexts.remove(text)
                    }
                }
            }
            return
        }
        
        speakInternal(text)
    }
    
    private fun speakInternal(text: String) {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "Cannot speak: TTS not initialized")
            return
        }
        
        try {
            // Check if TTS is busy
            if (tts?.isSpeaking == true) {
                Log.d(TAG, "TTS is busy, stopping current speech")
                tts?.stop()
                // Wait a bit for TTS to be ready
                mainHandler.postDelayed({
                    speakTextNow(text)
                }, SPEAK_DELAY_MS * 2)
            } else {
                // Small delay to ensure TTS is ready
                mainHandler.postDelayed({
                    speakTextNow(text)
                }, SPEAK_DELAY_MS)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in speakInternal", e)
        }
    }
    
    private fun speakTextNow(text: String) {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "Cannot speak: TTS not initialized")
            return
        }
        
        try {
            // Generate unique utterance ID
            val utteranceId = "tts_${System.currentTimeMillis()}"
            
            // Use QUEUE_FLUSH to stop any ongoing speech
            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            
            when (result) {
                TextToSpeech.ERROR -> {
                    Log.e(TAG, "Error speaking text: $text, will retry...")
                    // Retry after delay
                    mainHandler.postDelayed({
                        retrySpeak(text)
                    }, RETRY_DELAY_MS)
                }
                TextToSpeech.SUCCESS -> {
                    Log.d(TAG, "Successfully started speaking: $text")
                }
                else -> {
                    Log.w(TAG, "Unexpected result: $result for text: $text")
                }
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "TTS is in illegal state, reinitializing...", e)
            // Reinitialize if in illegal state
            isInitialized = false
            initialize { success ->
                if (success) {
                    speakInternal(text)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while speaking", e)
        }
    }
    
    private fun retrySpeak(text: String) {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "Cannot retry: TTS not initialized")
            return
        }
        
        try {
            val utteranceId = "tts_retry_${System.currentTimeMillis()}"
            val retryResult = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            
            if (retryResult == TextToSpeech.ERROR) {
                Log.e(TAG, "Retry also failed for: $text")
            } else {
                Log.d(TAG, "Retry successful, speaking: $text")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Retry failed", e)
        }
    }
    
    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech", e)
        }
    }
    
    fun setSpeechRate(rate: Float) {
        // Rate: 0.5 to 2.0 (1.0 is normal)
        tts?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }
    
    fun isSpeaking(): Boolean {
        return tts?.isSpeaking == true
    }
    
    fun shutdown() {
        try {
            pendingTexts.clear()
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
            isInitializing = false
            initializationCallback = null
            Log.d(TAG, "TTS shutdown completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
    
    fun isReady(): Boolean {
        return isInitialized && tts != null
    }
}

