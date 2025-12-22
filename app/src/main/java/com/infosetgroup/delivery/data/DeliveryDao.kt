package com.infosetgroup.delivery.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingSource

@Dao
interface DeliveryDao {
    // Pending deliveries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPending(entity: DeliveryEntity): Long

    // Flow for live updates
    @Query("SELECT * FROM pending_deliveries ORDER BY createdAt ASC")
    fun getAllPending(): Flow<List<DeliveryEntity>>

    // Paging source for infinite scroll in PendingScreen
    @Query("SELECT * FROM pending_deliveries ORDER BY createdAt ASC")
    fun getAllPendingPaging(): PagingSource<Int, DeliveryEntity>

    // Fetch single pending item by id (used for single-item sync from detail)
    @Query("SELECT * FROM pending_deliveries WHERE id = :id LIMIT 1")
    suspend fun getPendingById(id: Long): DeliveryEntity?

    // Non-flow for initial load
    @Query("SELECT * FROM pending_deliveries")
    suspend fun getAllDeliveries(): List<DeliveryEntity>

    // Flow for live updates
    @Query("SELECT COUNT(*) FROM pending_deliveries")
    fun getPendingCountFlow(): Flow<Int>

    // Non-flow for initial load
    @Query("DELETE FROM pending_deliveries WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int

    // Update retryCount
    @Query("UPDATE pending_deliveries SET retryCount = :retryCount WHERE id = :id")
    suspend fun updateRetryCount(id: Long, retryCount: Int): Int

    // --- HISTORY CACHE (for RemoteMediator) ---
    // Insert or replace cached history items
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(items: List<HistoryEntity>)

    // Clear cached history (used on refresh)
    @Query("DELETE FROM history_deliveries")
    suspend fun clearHistory()

    // Paging source used by Pager with RemoteMediator
    @Query("SELECT * FROM history_deliveries ORDER BY rowOrder ASC")
    fun getHistoryPaging(): PagingSource<Int, HistoryEntity>
}
