package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversion_queue")
data class QueueItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val videoUri: String,
    val videoName: String,
    val styleName: String,
    val qualityMode: String,
    val status: String, // "PENDING", "PROCESSING", "COMPLETED", "FAILED"
    val timestamp: Long = System.currentTimeMillis(),
    val progress: Float = 0f
)
