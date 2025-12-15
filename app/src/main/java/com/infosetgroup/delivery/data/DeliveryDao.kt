package com.infosetgroup.delivery.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPending(entity: DeliveryEntity): Long

    @Query("SELECT * FROM pending_deliveries ORDER BY createdAt ASC")
    fun getAllPending(): Flow<List<DeliveryEntity>>

    @Query("SELECT COUNT(*) FROM pending_deliveries")
    fun getPendingCountFlow(): Flow<Int>

    @Query("DELETE FROM pending_deliveries WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int

    @Query("UPDATE pending_deliveries SET retryCount = :retryCount WHERE id = :id")
    suspend fun updateRetryCount(id: Long, retryCount: Int): Int
}
