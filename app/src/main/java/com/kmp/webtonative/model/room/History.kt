package com.kmp.webtonative.model.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "History")
data class History(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "visitCount") val visitCount: Int = 1,
    @ColumnInfo(name = "lastVisitedTime") val lastVisitedTime: Long
)