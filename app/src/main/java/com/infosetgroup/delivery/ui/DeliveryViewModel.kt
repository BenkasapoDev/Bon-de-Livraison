package com.infosetgroup.delivery.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.infosetgroup.delivery.data.DeliveryEntity
import com.infosetgroup.delivery.repository.DeliveryRepository
import com.infosetgroup.delivery.repository.SubmitResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for the Delivery form actions (submit/save) and related UI state.
 * Uses Application-aware AndroidViewModel so we can obtain the repository singleton.
 */
class DeliveryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DeliveryRepository = DeliveryRepository.getInstance(application)

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    // last submit result (null means no recent result). UI can observe this to show feedback.
    private val _lastSubmitResult = MutableStateFlow<SubmitResult?>(null)
    val lastSubmitResult: StateFlow<SubmitResult?> = _lastSubmitResult.asStateFlow()

    fun submitDelivery(entity: DeliveryEntity) {
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val res = repository.submitDelivery(entity)
                _lastSubmitResult.value = res
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    // Allow UI to acknowledge/clear the last result after presenting feedback
    fun clearLastSubmitResult() {
        _lastSubmitResult.value = null
    }
}
