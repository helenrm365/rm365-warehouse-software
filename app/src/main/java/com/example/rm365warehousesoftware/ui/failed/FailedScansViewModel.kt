package com.example.rm365warehousesoftware.ui.failed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.rm365warehousesoftware.data.local.ScanEntity
import com.example.rm365warehousesoftware.data.repository.ScanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the "Failed Scans" review screen. Exposes the flagged scans and lets
 * the worker dismiss or retry each one.
 */
class FailedScansViewModel(
    private val repository: ScanRepository
) : ViewModel() {

    val failedScans: StateFlow<List<ScanEntity>> = repository.failedScans
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Reactive count of failed scans, used to badge the "Failed" tab. */
    val failedCount: StateFlow<Int> = repository.failedScans
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    /** Permanently deletes a bad scan from the queue. */
    fun dismiss(id: Long) {
        viewModelScope.launch { repository.dismissScan(id) }
    }

    /** Clears the error flag so the scan is retried on the next sync. */
    fun retry(id: Long) {
        viewModelScope.launch { repository.retryScan(id) }
    }

    /** Simple factory so the screen can supply the repository dependency. */
    class Factory(private val repository: ScanRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(FailedScansViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return FailedScansViewModel(repository) as T
        }
    }
}
