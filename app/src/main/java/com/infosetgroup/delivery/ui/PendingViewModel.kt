package com.infosetgroup.delivery.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infosetgroup.delivery.data.DeliveryEntity
import com.infosetgroup.delivery.repository.DeliveryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PendingViewModel(private val repository: DeliveryRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing

    private val _list = MutableStateFlow<List<DeliveryEntity>>(emptyList())
    val list: StateFlow<List<DeliveryEntity>> = _list

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
