package com.example.handspeak.data.repository

import com.example.handspeak.data.database.HistoryDao
import com.example.handspeak.data.database.HistoryEntity
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {
    
    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()
    
    fun getHistoryByType(type: String): Flow<List<HistoryEntity>> {
        return historyDao.getHistoryByType(type)
    }
    
    suspend fun insert(history: HistoryEntity) {
        historyDao.insert(history)
    }
    
    suspend fun delete(history: HistoryEntity) {
        historyDao.delete(history)
    }
    
    suspend fun deleteAll() {
        historyDao.deleteAll()
    }
}











