package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversion_history")
data class ConversionHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalUri: String,
    val processedPath: String,
    val styleName: String,
    val resolution: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long,
    val sizeBytes: Long
)
