package com.example.data

import kotlinx.coroutines.flow.Flow

class ConversionRepository(
    private val conversionDao: ConversionDao,
    private val queueDao: QueueDao
) {
    val allConversions: Flow<List<ConversionHistory>> = conversionDao.getAllConversions()
    val allQueueItems: Flow<List<QueueItem>> = queueDao.getAllQueueItems()

    suspend fun insert(history: ConversionHistory): Long {
        return conversionDao.insertConversion(history)
    }

    suspend fun getById(id: Int): ConversionHistory? {
        return conversionDao.getConversionById(id)
    }

    suspend fun delete(id: Int) {
        conversionDao.deleteConversionById(id)
    }

    // Queue-related methods
    suspend fun getNextPendingQueueItem(): QueueItem? {
        return queueDao.getNextPendingItem()
    }

    suspend fun getProcessingQueueItem(): QueueItem? {
        return queueDao.getProcessingItem()
    }

    suspend fun insertQueueItem(item: QueueItem): Long {
        return queueDao.insertQueueItem(item)
    }

    suspend fun updateQueueItem(item: QueueItem) {
        queueDao.updateQueueItem(item)
    }

    suspend fun deleteQueueItem(id: Int) {
        queueDao.deleteQueueItemById(id)
    }

    suspend fun clearQueue() {
        queueDao.clearQueue()
    }

    suspend fun clearCompletedQueue() {
        queueDao.clearCompletedQueueItems()
    }
}
