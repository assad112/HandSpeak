package com.example.handspeak.util

import android.content.Context
import com.example.handspeak.data.model.FavoriteItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object FavoriteManager {
    private const val PREFS = "favorites_prefs"
    private const val KEY = "favorites_json"
    private val gson = Gson()
    private val type = object : TypeToken<MutableList<FavoriteItem>>() {}.type

    fun getFavorites(context: Context): MutableList<FavoriteItem> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY, "[]") ?: "[]"
        return gson.fromJson(json, type)
    }

    fun addFavorite(context: Context, item: FavoriteItem) {
        val list = getFavorites(context)
        if (list.none { it.label == item.label && it.folder == item.folder }) {
            list.add(item)
            save(context, list)
        }
    }

    fun removeFavorite(context: Context, item: FavoriteItem) {
        val list = getFavorites(context)
        val newList = list.filterNot { it.label == item.label && it.folder == item.folder }
        save(context, newList.toMutableList())
    }

    private fun save(context: Context, list: MutableList<FavoriteItem>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY, gson.toJson(list)).apply()
    }
}


