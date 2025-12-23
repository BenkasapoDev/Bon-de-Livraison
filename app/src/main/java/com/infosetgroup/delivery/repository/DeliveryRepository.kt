package com.infosetgroup.delivery.repository

import android.content.Context
import android.util.Base64
import com.infosetgroup.delivery.data.AppDatabase
import com.infosetgroup.delivery.data.AppDatabaseHolder
import com.infosetgroup.delivery.data.DeliveryDao
import com.infosetgroup.delivery.data.DeliveryEntity
import com.infosetgroup.delivery.network.NetworkClient
import com.infosetgroup.delivery.network.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import androidx.paging.PagingSource



sealed class SubmitResult {
    object Sent : SubmitResult()
    data class Queued(val id: Long) : SubmitResult()
    data class Failure(val error: String?) : SubmitResult()
}

sealed class SyncResult {
    data class Success(val syncedCount: Int) : SyncResult()
    data class Failure(val error: String?) : SyncResult()
    object NothingToSync : SyncResult()
}

class DeliveryRepository private constructor(context: Context) {
    // Store application context only to avoid leaking Activity/Service contexts in a static singleton
    private val appContext: Context = context.applicationContext

    // Use AppDatabaseHolder to ensure initialization goes through the safe path which can
    // remove a stale DB file on schema mismatch during development.
    init {
        try {
            AppDatabaseHolder.init(appContext)
        } catch (_: Throwable) {
            // best-effort; AppDatabaseHolder already handles deletion/retry internally
        }
    }

    // renamed private backing property to avoid JVM signature clash with public getDao()
    private val dbDao: DeliveryDao
        get() = AppDatabaseHolder.instance?.deliveryDao() ?: AppDatabase.getInstance(appContext).deliveryDao()

    // expose DAO for callers that need direct DB access (paging, advanced queries)
    fun getDao(): DeliveryDao = dbDao

    // Convenience: expose the PagingSource directly so callers (ViewModels) can use Pager without touching DAO
    fun getPendingPagingSource(): PagingSource<Int, DeliveryEntity> = dbDao.getAllPendingPaging()

    fun observePendingCount() = dbDao.getPendingCountFlow()

    // return suspending list directly from DAO
    suspend fun getAllDeliveries(): List<DeliveryEntity> {
        return dbDao.getAllDeliveries()
    }

    // expose the DAO flow so callers can observe live updates
    fun observeAllPending(): Flow<List<DeliveryEntity>> = dbDao.getAllPending()

    // convenience wrapper to reuse the existing sync logic
    suspend fun syncDeliveries(): SyncResult {
        return syncPending()
    }

    // Sync a single pending delivery by id. Returns SubmitResult.Sent on success, Queued on failure
    suspend fun syncSinglePending(id: Long): SubmitResult = withContext(Dispatchers.IO) {
        val entity = dbDao.getPendingById(id) ?: return@withContext SubmitResult.Failure("Not found")
        try {
            val imageBase64 = fileToBase64(entity.receiverProofPath)
            val json = JSONObject().apply {
                put("item", entity.item)
                put("serialNumber", entity.serialNumber)
                put("sim", entity.sim)
                put("merchant", entity.merchant)
                put("shop", entity.shop)
                put("receiver", entity.receiver)
                put("deliveryAgent", entity.deliveryAgent)
                put("receiverProof", imageBase64)
            }.toString()

            when (val res = NetworkClient.postDelivery(json)) {
                is NetworkResult.Success -> {
                    if (res.code in 200..299) {
                        // on success remove local DB entry and delete file
                        entity.receiverProofPath?.let { File(it).takeIf { f -> f.exists() }?.delete() }
                        dbDao.deleteByIds(listOf(entity.id))
                        return@withContext SubmitResult.Sent
                    } else {
                        dbDao.updateRetryCount(entity.id, entity.retryCount + 1)
                        return@withContext SubmitResult.Queued(entity.id)
                    }
                }
                is NetworkResult.Failure -> {
                    dbDao.updateRetryCount(entity.id, entity.retryCount + 1)
                    return@withContext SubmitResult.Queued(entity.id)
                }
            }
        } catch (_: Throwable) {
            dbDao.updateRetryCount(entity.id, entity.retryCount + 1)
            return@withContext SubmitResult.Queued(entity.id)
        }
    }

    private suspend fun fileToBase64(path: String?): String {
        if (path.isNullOrBlank()) return ""
        return withContext(Dispatchers.IO) {
            try {
                val f = File(path)
                if (!f.exists()) return@withContext ""
                val bytes = f.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } catch (_: Throwable) {
                ""
            }
        }
    }

    suspend fun submitDelivery(entity: DeliveryEntity): SubmitResult = withContext(Dispatchers.IO) {
        try {
            val imageBase64 = fileToBase64(entity.receiverProofPath)
            val json = JSONObject().apply {
                put("item", entity.item)
                put("serialNumber", entity.serialNumber)
                put("sim", entity.sim)
                put("merchant", entity.merchant)
                put("shop", entity.shop)
                put("receiver", entity.receiver)
                put("deliveryAgent", entity.deliveryAgent)
                put("receiverProof", imageBase64)
            }.toString()

            when (val res = NetworkClient.postDelivery(json)) {
                is NetworkResult.Success -> {
                    if (res.code in 200..299) {
                        // optionally delete local file after successful send
                        entity.receiverProofPath?.let { File(it).takeIf { f -> f.exists() }?.delete() }
                        return@withContext SubmitResult.Sent
                    } else {
                        val id = dbDao.insertPending(entity)
                        return@withContext SubmitResult.Queued(id)
                    }
                }
                is NetworkResult.Failure -> {
                    val id = dbDao.insertPending(entity)
                    return@withContext SubmitResult.Queued(id)
                }
            }
        } catch (_: Throwable) {
            val id = dbDao.insertPending(entity)
            return@withContext SubmitResult.Queued(id)
        }
    }

    suspend fun syncPending(): SyncResult = withContext(Dispatchers.IO) {
        // Refactor Flow collection and handle null safety
        val pending = dbDao.getAllPending().firstOrNull() ?: emptyList()
        if (pending.isEmpty()) return@withContext SyncResult.NothingToSync

        // build JSON array
        val arr = JSONArray()
        val ids = mutableListOf<Long>()
        // Iterate safely over the collection
        for (p in pending) {
            val imageBase64 = fileToBase64(p.receiverProofPath)
            val obj = JSONObject().apply {
                put("item", p.item)
                put("serialNumber", p.serialNumber)
                put("sim", p.sim)
                put("merchant", p.merchant)
                put("shop", p.shop)
                put("receiver", p.receiver)
                put("deliveryAgent", p.deliveryAgent)
                put("receiverProof", imageBase64)
            }
            arr.put(obj)
            ids.add(p.id)
        }

        when (val res = NetworkClient.postDeliveriesBulk(arr.toString())) {
            is NetworkResult.Success -> {
                if (res.code in 200..299) {
                    // delete DB entries and local files
                    for (p in pending) {
                        p.receiverProofPath?.let { path ->
                            val file = File(path)
                            if (file.exists()) file.delete()
                        }
                    }
                    dbDao.deleteByIds(ids)
                    return@withContext SyncResult.Success(ids.size)
                } else {
                    // non-2xx: increment retryCount
                    for (p in pending) {
                        dbDao.updateRetryCount(p.id, p.retryCount + 1)
                    }
                    return@withContext SyncResult.Failure("Server returned code ${res.code}")
                }
            }
            is NetworkResult.Failure -> {
                for (p in pending) {
                    dbDao.updateRetryCount(p.id, p.retryCount + 1)
                }
                return@withContext SyncResult.Failure(res.throwable?.message)
            }
        }
    }

    companion object {
        // Correcting the memory leak warning
        @Volatile
        private var INSTANCE: DeliveryRepository? = null

        fun getInstance(context: Context): DeliveryRepository {
            return INSTANCE ?: synchronized(this) {
                val inst = DeliveryRepository(context.applicationContext)
                INSTANCE = inst
                inst
            }
        }
    }
}
