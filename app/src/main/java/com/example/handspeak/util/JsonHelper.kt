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
    
    /**
     * البحث الذكي في sign_map
     * يحاول البحث بعدة طرق: تطابق مباشر، تطابق غير حساس لحالة الأحرف، تطابق جزئي
     */
    fun findSignInfo(signMap: Map<String, SignInfo>, query: String): SignInfo? {
        if (query.isEmpty()) return null
        
        val trimmedQuery = query.trim()
        
        // 1. تطابق مباشر
        signMap[trimmedQuery]?.let { return it }
        
        // 2. تطابق غير حساس لحالة الأحرف
        signMap.entries.firstOrNull { 
            it.key.equals(trimmedQuery, ignoreCase = true) 
        }?.value?.let { return it }
        
        // 3. تطابق جزئي (يحتوي على)
        signMap.entries.firstOrNull { 
            it.key.contains(trimmedQuery, ignoreCase = true) || 
            trimmedQuery.contains(it.key, ignoreCase = true)
        }?.value?.let { return it }
        
        // 4. البحث عن أول كلمة في النص
        val firstWord = trimmedQuery.split("\\s+".toRegex()).firstOrNull()
        firstWord?.let { word ->
            signMap[word]?.let { return it }
            signMap.entries.firstOrNull { 
                it.key.contains(word, ignoreCase = true) || 
                word.contains(it.key, ignoreCase = true)
            }?.value?.let { return it }
        }
        
        return null
    }
    
    /**
     * البحث عن جميع الإشارات التي تطابق الاستعلام
     */
    fun searchSigns(signMap: Map<String, SignInfo>, query: String): List<Pair<String, SignInfo>> {
        if (query.isEmpty()) return signMap.map { Pair(it.key, it.value) }
        
        val trimmedQuery = query.trim().lowercase()
        
        return signMap.entries
            .filter { 
                it.key.contains(trimmedQuery, ignoreCase = true) ||
                it.value.label.contains(trimmedQuery, ignoreCase = true)
            }
            .map { Pair(it.key, it.value) }
            .sortedBy { it.first }
    }
}











