package com.kmp.webtonative.model.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM History ORDER BY lastVisitedTime DESC")
    fun getAllHistory(): Flow<List<History>>

    @Query("SELECT * FROM History WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): History?

    @Insert
    suspend fun insert(history: History)

    @Query("""
        UPDATE History 
        SET visitCount = visitCount + 1, lastVisitedTime = :time 
        WHERE url = :url
    """)
    suspend fun incrementVisit(url: String, time: Long)

    @Query("DELETE FROM History")
    suspend fun clearHistory()

    @Transaction
    suspend fun recordVisit(url: String, title: String) {
        val existing = getByUrl(url)
        if (existing == null) {
            insert(History(url = url, title = title, lastVisitedTime = System.currentTimeMillis()))
        } else {
            incrementVisit(url, System.currentTimeMillis())
        }
    }
}