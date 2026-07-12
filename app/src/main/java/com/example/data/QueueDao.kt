package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueDao {
    @Query("SELECT * FROM conversion_queue ORDER BY timestamp ASC")
    fun getAllQueueItems(): Flow<List<QueueItem>>

    @Query("SELECT * FROM conversion_queue WHERE status = 'PENDING' ORDER BY timestamp ASC LIMIT 1")
    suspend fun getNextPendingItem(): QueueItem?

    @Query("SELECT * FROM conversion_queue WHERE status = 'PROCESSING' LIMIT 1")
    suspend fun getProcessingItem(): QueueItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItem(item: QueueItem): Long

    @Update
    suspend fun updateQueueItem(item: QueueItem)

    @Query("DELETE FROM conversion_queue WHERE id = :id")
    suspend fun deleteQueueItemById(id: Int)

    @Query("DELETE FROM conversion_queue")
    suspend fun clearQueue()

    @Query("DELETE FROM conversion_queue WHERE status = 'COMPLETED' OR status = 'FAILED'")
    suspend fun clearCompletedQueueItems()
}
