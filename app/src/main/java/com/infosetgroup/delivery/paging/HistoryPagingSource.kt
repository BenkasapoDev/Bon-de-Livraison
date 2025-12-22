package com.infosetgroup.delivery.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.infosetgroup.delivery.network.NetworkClient
import com.infosetgroup.delivery.network.NetworkResult
import org.json.JSONArray
import com.infosetgroup.delivery.DeliveryItem

/**
 * PagingSource that loads pages from the remote history endpoint.
 */
class HistoryPagingSource(
    private val keyword: String?
) : PagingSource<Int, DeliveryItem>() {
    override fun getRefreshKey(state: PagingState<Int, DeliveryItem>): Int? {
        return state.anchorPosition?.let { anchor ->
            val page = state.closestPageToPosition(anchor)
            page?.prevKey?.plus(1) ?: page?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DeliveryItem> {
        val page = params.key ?: 1
        val limit = params.loadSize.coerceAtMost(50)
        return try {
            when (val res = NetworkClient.getHistory(page = page, limit = limit, keyword = keyword)) {
                is NetworkResult.Success -> {
                    if (res.code in 200..299) {
                        val body = res.body ?: "[]"
                        val arr = JSONArray(body)
                        val list = mutableListOf<DeliveryItem>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(
                                DeliveryItem(
                                    item = obj.optString("item"),
                                    serialNumber = obj.optString("serialNumber"),
                                    sim = obj.optString("sim"),
                                    merchant = obj.optString("merchant"),
                                    shop = obj.optString("shop"),
                                    receiver = obj.optString("receiver"),
                                    deliveryAgent = obj.optString("deliveryAgent"),
                                    code = obj.optString("code"),
                                    receiverProofPath = obj.optString("receiverProofPath", "")
                                )
                            )
                        }

                        val nextKey = if (arr.length() < limit) null else page + 1
                        LoadResult.Page(data = list, prevKey = if (page == 1) null else page - 1, nextKey = nextKey)
                    } else {
                        LoadResult.Error(Exception("Server returned code ${res.code}"))
                    }
                }
                is NetworkResult.Failure -> LoadResult.Error(res.throwable ?: Exception("Unknown network error"))
            }
        } catch (_: Throwable) {
            LoadResult.Error(Exception("History load failed"))
        }
    }
}
