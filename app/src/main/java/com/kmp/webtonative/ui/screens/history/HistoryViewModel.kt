package com.kmp.webtonative.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmp.webtonative.model.repository.database.HistoryRepository
import com.kmp.webtonative.model.room.History
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class HistoryViewModel(
    private val repo: HistoryRepository
) : ViewModel() {

    val historyState: StateFlow<HistoryState> = repo.allHistory
        .map { list ->
            if (list.isEmpty()) HistoryState.Empty
            else HistoryState.Success(groupHistory(list))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HistoryState.Loading
        )

    private fun groupHistory(list: List<History>): LinkedHashMap<String, List<History>> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val yesterday = today.minusDays(1)

        val result = LinkedHashMap<String, List<History>>()

        val todayItems = list.filter { toLocalDate(it.lastVisitedTime, zone) == today }
        val yesterdayItems = list.filter { toLocalDate(it.lastVisitedTime, zone) == yesterday }
        val earlierItems = list.filter {
            val date = toLocalDate(it.lastVisitedTime, zone)
            date.isBefore(yesterday)
        }

        if (todayItems.isNotEmpty()) result["TODAY"] = todayItems
        if (yesterdayItems.isNotEmpty()) result["YESTERDAY"] = yesterdayItems
        if (earlierItems.isNotEmpty()) result["EARLIER"] = earlierItems

        return result
    }

    private fun toLocalDate(timestamp: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()

    fun formatTime(timestamp: Long, group: String): String {
        val zone = ZoneId.systemDefault()
        val instant = Instant.ofEpochMilli(timestamp)
        val diff = System.currentTimeMillis() - timestamp
        val hours = diff / (1000 * 60 * 60)
        val mins = diff / (1000 * 60)

        return when (group) {
            "TODAY" -> when {
                hours >= 1 -> "${hours}h ago"
                mins >= 1 -> "${mins}m ago"
                else -> "Just now"
            }
            "YESTERDAY" -> {
                val time = DateTimeFormatter
                    .ofPattern("h:mm a", Locale.getDefault())
                    .format(instant.atZone(zone))
                "Yesterday $time"
            }
            else -> DateTimeFormatter
                .ofPattern("MMM d", Locale.getDefault())
                .format(instant.atZone(zone))
        }
    }

    fun clearHistory() {
        viewModelScope.launch { repo.clearHistory() }
    }
}