package com.example.handspeak.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val translationType: String, // "sign_to_text", "text_to_sign", "voice_to_sign"
    val confidence: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)











