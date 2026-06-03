package com.kmp.webtonative.model.repository

import com.kmp.webtonative.model.room.History
import com.kmp.webtonative.model.room.HistoryDao
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val dao: HistoryDao) {

    // Expose history as a cold Flow — ViewModel collects this
    val allHistory: Flow<List<History>> = dao.getAllHistory()

    // Called from WebView whenever URL changes
    suspend fun recordVisit(url: String, title: String) {
        dao.recordVisit(url, title)
    }

    // Called from History screen's clear button
    suspend fun clearHistory() {
        dao.clearHistory()
    }
}