package com.example.handspeak.data.model

data class HandLandmark(
    val x: Float,
    val y: Float,
    val z: Float
)

data class HandDetectionResult(
    val landmarks: List<HandLandmark>,
    val confidence: Float
)











