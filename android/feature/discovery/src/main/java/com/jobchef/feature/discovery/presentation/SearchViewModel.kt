package com.jobchef.feature.discovery.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jobchef.core.domain.repository.JobRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
    private val repository: JobRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeActiveResumeTitle().collect { title ->
                _state.update { it.copy(activeResumeName = title) }
            }
        }
    }

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.OnRoleChanged ->
                _state.update { it.copy(targetRole = event.role, errorMessage = null) }
            is SearchEvent.OnLocationChanged ->
                _state.update { it.copy(location = event.location, errorMessage = null) }
            is SearchEvent.OnKeywordsChanged ->
                _state.update { it.copy(keywords = event.keywords, errorMessage = null) }
            is SearchEvent.OnFilterToggled ->
                _state.update { current ->
                    val selected = if (event.filter in current.selectedFilters) emptySet() else setOf(event.filter)
                    current.copy(selectedFilters = selected)
                }
        }
    }

    fun search(onSuccess: () -> Unit) {
        val criteria = _state.value
        val role = criteria.targetRole.trim()
        val location = criteria.location.trim()
        if (role.isBlank() || role.length > 120) {
            _state.update { it.copy(errorMessage = "Enter a role of 1–120 characters.") }
            return
        }
        if (location.length > 120) {
            _state.update { it.copy(errorMessage = "Location must be 120 characters or fewer.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                repository.refreshJobBatch(
                    query = role,
                    location = location,
                    keywords = criteria.keywords.trim().take(300),
                    workMode = when (criteria.selectedFilters.singleOrNull()) {
                        "Remote" -> "REMOTE"
                        "Hybrid" -> "HYBRID"
                        "On-site" -> "ON_SITE"
                        else -> null
                    }
                )
                _state.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Job search failed. Check your connection and configured Adzuna service, then try again."
                    )
                }
            }
        }
    }
}
