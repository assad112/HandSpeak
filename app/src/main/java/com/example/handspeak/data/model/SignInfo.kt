package com.example.handspeak.data.model

data class SignInfo(
    val label: String,
    val type: String, // "images" or "video"
    val folder: String? = null,
    val videoRes: Int? = null
)











