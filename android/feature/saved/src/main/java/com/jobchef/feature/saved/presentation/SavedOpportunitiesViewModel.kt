package com.jobchef.feature.saved.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SavedOpportunitiesViewModel : ViewModel() {

    private val _state = MutableStateFlow(dummySavedJobsState)
    val state: StateFlow<SavedJobsState> = _state.asStateFlow()

    fun onEvent(event: SavedOpportunitiesEvent) {
        when (event) {
            is SavedOpportunitiesEvent.OnFilterSelected -> {
                _state.update { it.copy(activeFilter = event.filter) }
            }
            is SavedOpportunitiesEvent.OnJobClicked -> {
                // Navigates to details / export tailor lab (handled by navigation callbacks)
            }
        }
    }
}
