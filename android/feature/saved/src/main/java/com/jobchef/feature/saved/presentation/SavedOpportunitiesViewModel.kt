package com.jobchef.feature.saved.presentation

import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope
import com.jobchef.core.domain.repository.JobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.random.Random

class SavedOpportunitiesViewModel(
    private val repository: JobRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SavedJobsState())
    val state: StateFlow<SavedJobsState> = _state.asStateFlow()

    init {
        observeSavedJobs()
    }

    private fun observeSavedJobs() {
        repository.getSavedJobs()
            .onEach { jobs ->
                val savedJobItems = jobs.map { job ->
                    val min = job.salaryMin
                    val max = job.salaryMax
                    SavedJobItem(
                        id = job.id,
                        company = job.company,
                        role = job.title,
                        logo = job.company.firstOrNull()?.uppercase() ?: "?",
                        location = job.location,
                        pay = if (min != null && max != null) {
                            "₹${min.toInt()}-${max.toInt()}"
                        } else if (min != null) {
                            "₹${min.toInt()}"
                        } else {
                            "Salary undisclosed"
                        },
                        status = JobStatus.READY, // Hardcoded for now
                        matchScore = Random.nextInt(80, 99) // Mock score for now
                    )
                }
                _state.update { it.copy(jobs = savedJobItems) }
            }
            .catch { e ->
                // Handle error
            }
            .launchIn(viewModelScope)
    }

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
