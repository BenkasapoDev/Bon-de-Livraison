package com.infosetgroup.delivery.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.infosetgroup.delivery.data.DeliveryEntity
import com.infosetgroup.delivery.repository.DeliveryRepository
import com.infosetgroup.delivery.repository.SyncResult
import com.infosetgroup.delivery.repository.SubmitResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import androidx.paging.cachedIn

/**
 * PendingViewModel now extends AndroidViewModel so it can be instantiated by the default
 * ViewModelProvider used by Compose's viewModel() helper without needing a custom factory.
 */
class PendingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DeliveryRepository = DeliveryRepository.getInstance(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing

    // single-item syncing state (detail view can observe this if needed)
    private val _singleSyncing = MutableStateFlow<Long?>(null)
    val singleSyncing: StateFlow<Long?> = _singleSyncing

    // emit one-time sync results for the UI to show messages
    private val _syncEvents = MutableSharedFlow<SyncResult>()
    val syncEvents: SharedFlow<SyncResult> = _syncEvents

    // observe repository flow directly for live updates
    val list: StateFlow<List<DeliveryEntity>> = repository.observeAllPending()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // PagingData flow for infinite scrolling in PendingScreen
    val pagingFlow: Flow<PagingData<DeliveryEntity>> = Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
        repository.getDao().getAllPendingPaging()
    }.flow.cachedIn(viewModelScope)

    // derived pending count (live)
    val pendingCount: StateFlow<Int> = list
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // kept for compatibility — triggers initial load in UI (no-op now)
    fun fetchDeliveries() {
        // repository flow is observed automatically; keep function for backward compatibility
    }

    fun syncDeliveries() {
        viewModelScope.launch {
            _syncing.value = true
            when (val result = repository.syncDeliveries()) {
                is SyncResult.Success -> {
                    // emit event so UI can show snackbar / toast
                    _syncEvents.emit(result)
                    // nothing extra needed; DAO flow will update after deletion
                }
                is SyncResult.Failure -> {
                    // emit failure event for UI
                    _syncEvents.emit(result)
                    // Could surface error via another StateFlow or Log — kept simple for now
                }
                SyncResult.NothingToSync -> {
                    _syncEvents.emit(SyncResult.NothingToSync)
                    // no-op
                }
            }
            _syncing.value = false
        }
    }

    // Sync a single pending delivery by id (used from detail screen). Emits SyncResult via _syncEvents
    fun syncSingle(id: Long) {
        viewModelScope.launch {
            _singleSyncing.value = id
            when (val res = repository.syncSinglePending(id)) {
                is SubmitResult.Sent -> {
                    // treat as success of single item
                    _syncEvents.emit(SyncResult.Success(1))
                }
                is SubmitResult.Queued -> {
                    _syncEvents.emit(SyncResult.Failure("Envoi différé"))
                }
                is SubmitResult.Failure -> {
                    _syncEvents.emit(SyncResult.Failure(res.error))
                }
            }
            _singleSyncing.value = null
        }
    }
}
