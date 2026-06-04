package com.kmp.webtonative.ui.screens.history

import com.kmp.webtonative.model.room.History

sealed class HistoryState {
    object Loading : HistoryState()
    object Empty : HistoryState()
    data class Success(val groups: LinkedHashMap<String, List<History>>) : HistoryState()
}