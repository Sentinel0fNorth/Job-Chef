package com.jobchef.feature.saved.presentation

sealed interface SavedOpportunitiesEvent {
    data class OnFilterSelected(val filter: String) : SavedOpportunitiesEvent
    data class OnJobClicked(val job: SavedJobItem) : SavedOpportunitiesEvent
}
