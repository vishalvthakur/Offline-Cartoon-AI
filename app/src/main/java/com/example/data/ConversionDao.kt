package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversionDao {
    @Query("SELECT * FROM conversion_history ORDER BY timestamp DESC")
    fun getAllConversions(): Flow<List<ConversionHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversion(history: ConversionHistory): Long

    @Query("SELECT * FROM conversion_history WHERE id = :id")
    suspend fun getConversionById(id: Int): ConversionHistory?

    @Query("DELETE FROM conversion_history WHERE id = :id")
    suspend fun deleteConversionById(id: Int)
}
