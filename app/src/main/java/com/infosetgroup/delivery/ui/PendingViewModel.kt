package com.infosetgroup.delivery.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infosetgroup.delivery.data.DeliveryEntity
import com.infosetgroup.delivery.repository.DeliveryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class PendingViewModel(private val repository: DeliveryRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing

    private val _list = MutableStateFlow<List<DeliveryEntity>>(emptyList())
    val list: StateFlow<List<DeliveryEntity>> = _list

    // derived pending count (live)
    val pendingCount: StateFlow<Int> = _list
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun fetchDeliveries() {
        viewModelScope.launch {
            _isLoading.value = true
            _list.value = repository.getAllDeliveries()
            _isLoading.value = false
        }
    }

    fun syncDeliveries() {
        viewModelScope.launch {
            _syncing.value = true
            repository.syncDeliveries()
            _syncing.value = false
        }
    }
}
