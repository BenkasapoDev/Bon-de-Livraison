package com.infosetgroup.delivery.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.infosetgroup.delivery.DeliveryItem
import com.infosetgroup.delivery.paging.HistoryPagingSource
import com.infosetgroup.delivery.paging.HistoryRemoteMediator
import com.infosetgroup.delivery.data.AppDatabaseHolder
import com.infosetgroup.delivery.data.HistoryEntity
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * ViewModel exposing paged history data and search keyword state.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPagingApi::class)
@FlowPreview
class HistoryViewModel : ViewModel() {
    private val _keyword = MutableStateFlow<String?>(null)
    val keyword: StateFlow<String?> get() = _keyword

    fun setKeyword(q: String?) {
        _keyword.value = q
    }

    // Pager recreated when keyword changes; debounce applied to avoid excessive network calls while typing
    val historyFlow: Flow<PagingData<DeliveryItem>> = _keyword
        .debounce(400)
        .distinctUntilChanged()
        .flatMapLatest { k ->
            // Explicitly produce a Flow<PagingData<DeliveryItem>> from either branch so Kotlin can infer a single type
            val db = AppDatabaseHolder.instance
            val flow: Flow<PagingData<DeliveryItem>> = if (db == null) {
                Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
                    HistoryPagingSource(k)
                }.flow
            } else {
                Pager(
                    config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                    remoteMediator = HistoryRemoteMediator(db, k)
                ) {
                    db.deliveryDao().getHistoryPaging()
                }.flow.map { pagingData: PagingData<HistoryEntity> ->
                    // Map cached HistoryEntity -> DeliveryItem so consumers always see DeliveryItem
                    pagingData.map { he: HistoryEntity ->
                        DeliveryItem(
                            item = he.item,
                            serialNumber = he.serialNumber,
                            sim = he.sim,
                            merchant = he.merchant,
                            shop = he.shop,
                            receiver = he.receiver,
                            deliveryAgent = he.deliveryAgent,
                            code = he.code,
                            receiverProofPath = he.receiverProofPath ?: ""
                        )
                    }
                }
            }
            flow
        }
        .cachedIn(viewModelScope)
}
