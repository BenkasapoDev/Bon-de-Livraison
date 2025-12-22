package com.infosetgroup.delivery.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.infosetgroup.delivery.data.AppDatabase
import com.infosetgroup.delivery.data.HistoryEntity
import com.infosetgroup.delivery.data.RemoteKeys
import com.infosetgroup.delivery.network.NetworkClient
import com.infosetgroup.delivery.network.NetworkResult
import org.json.JSONArray

private const val HISTORY_REPO_KEY = "HISTORY"

@OptIn(ExperimentalPagingApi::class)
class HistoryRemoteMediator(
    private val database: AppDatabase,
    private val keyword: String?
) : RemoteMediator<Int, HistoryEntity>() {
    private val dao = database.deliveryDao()
    private val remoteKeysDao = database.remoteKeysDao()

    override suspend fun initialize(): InitializeAction {
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, HistoryEntity>
    ): MediatorResult {
        return try {
            val page = when (loadType) {
                LoadType.REFRESH -> 1
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val keys = remoteKeysDao.remoteKeysByRepoId(HISTORY_REPO_KEY)
                    keys?.nextKey ?: return MediatorResult.Success(endOfPaginationReached = true)
                }
            }

            val limit = state.config.pageSize.coerceAtMost(50)
            when (val res = NetworkClient.getHistory(page = page, limit = limit, keyword = keyword)) {
                is NetworkResult.Success -> {
                    if (res.code in 200..299) {
                        val body = res.body ?: "[]"
                        val arr = JSONArray(body)
                        val items = mutableListOf<HistoryEntity>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            // use `code` as unique id if server doesn't provide explicit id
                            val id = obj.optString("id", obj.optString("code", "${page}_$i"))
                            // Some endpoints return base64 in `receiverProof` while cached field is `receiverProofPath`.
                            val receiverPath = when {
                                obj.has("receiverProofPath") && !obj.isNull("receiverProofPath") -> obj.getString("receiverProofPath")
                                obj.has("receiverProof") && !obj.isNull("receiverProof") -> obj.getString("receiverProof")
                                else -> null
                            }
                            val createdAt = if (obj.has("createdAt") && !obj.isNull("createdAt")) obj.getString("createdAt") else null
                            val rowOrder = ((page - 1) * limit + i).toLong()
                            items.add(
                                HistoryEntity(
                                    id = id,
                                    item = obj.optString("item"),
                                    serialNumber = obj.optString("serialNumber"),
                                    sim = obj.optString("sim"),
                                    merchant = obj.optString("merchant"),
                                    shop = obj.optString("shop"),
                                    receiver = obj.optString("receiver"),
                                    deliveryAgent = obj.optString("deliveryAgent"),
                                    code = obj.optString("code"),
                                    receiverProofPath = receiverPath,
                                    createdAt = createdAt,
                                    rowOrder = rowOrder
                                )
                            )
                        }

                        val endOfPaginationReached = arr.length() < limit
                        // use withTransaction (suspend) so suspend DAO methods are allowed
                        database.withTransaction {
                            if (loadType == LoadType.REFRESH) {
                                dao.clearHistory()
                                remoteKeysDao.clearRemoteKeys()
                            }
                            dao.insertHistory(items)
                            val nextKey = if (endOfPaginationReached) null else page + 1
                            val keys = RemoteKeys(repoId = HISTORY_REPO_KEY, prevKey = if (page == 1) null else page - 1, nextKey = nextKey)
                            remoteKeysDao.insertAll(listOf(keys))
                        }

                        MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
                    } else {
                        MediatorResult.Error(Exception("Server returned code ${res.code}"))
                    }
                }
                is NetworkResult.Failure -> MediatorResult.Error(res.throwable ?: Exception("Unknown network error"))
            }
        } catch (_: Throwable) {
            MediatorResult.Error(Exception("History mediator failed"))
        }
    }
}
