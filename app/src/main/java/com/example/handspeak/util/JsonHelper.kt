package com.example.handspeak.util

import android.content.Context
import com.example.handspeak.data.model.SignInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

object JsonHelper {
    
    fun loadLabels(context: Context): List<String> {
        return try {
            val json = context.assets.open("labels.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: IOException) {
            emptyList()
        }
    }
    
    fun loadSignMap(context: Context): Map<String, SignInfo> {
        return try {
            val json = context.assets.open("sign_map.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<String, SignInfo>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: IOException) {
            emptyMap()
        }
    }
}











