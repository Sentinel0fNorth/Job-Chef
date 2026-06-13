package com.jobchef.feature.discovery.presentation

sealed interface SearchEvent {
    data class OnRoleChanged(val role: String) : SearchEvent
    data class OnLocationChanged(val location: String) : SearchEvent
    data class OnKeywordsChanged(val keywords: String) : SearchEvent
    data class OnFilterToggled(val filter: String) : SearchEvent
    data object OnSubmitSearch : SearchEvent
}
