package com.example.handspeak.data.model

data class TranslationResult(
    val text: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)











