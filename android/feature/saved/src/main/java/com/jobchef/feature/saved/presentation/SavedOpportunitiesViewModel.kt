package com.jobchef.feature.saved.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jobchef.core.domain.model.JobStatus
import com.jobchef.core.domain.model.GenerationStatus
import com.jobchef.core.domain.repository.JobRepository
import com.jobchef.core.domain.repository.TailoredApplicationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SavedOpportunitiesViewModel(
    private val repository: JobRepository,
    private val applications: TailoredApplicationRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SavedJobsState())
    val state: StateFlow<SavedJobsState> = _state.asStateFlow()
    private val statusRefreshRequested = mutableSetOf<String>()
    private val statusRefreshAttempted = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            combine(repository.getSavedJobs(), applications.getAllApplications()) { jobs, tailored ->
                jobs.map { job ->
                    val application = tailored.firstOrNull { it.jobId == job.id }
                    SavedJobItem(
                        id = job.id,
                        company = job.company,
                        role = job.title,
                        logo = job.company.firstOrNull()?.uppercase() ?: "?",
                        location = job.location,
                        pay = formatSalary(job.salaryMin, job.salaryMax, job.salaryCurrency),
                        status = application?.let(::displayStatus) ?: "Saved",
                        matchScore = job.matchScore,
                        matchMethod = job.matchMethod
                    )
                }
            }.collect { items ->
                _state.update { it.copy(jobs = items, isLoading = false) }
                items.asSequence()
                    .filter { it.status !in REFRESHABLE_STATUSES }
                    .map { it.id }
                    .forEach(statusRefreshAttempted::remove)
                items.asSequence()
                    .filter { it.status in REFRESHABLE_STATUSES }
                    .map { it.id }
                    .filter(statusRefreshAttempted::add)
                    .filter(statusRefreshRequested::add)
                    .forEach(::refreshStatus)
            }
        }
    }

    fun onEvent(event: SavedOpportunitiesEvent) {
        when (event) {
            is SavedOpportunitiesEvent.OnFilterSelected ->
                _state.update { it.copy(activeFilter = event.filter) }
            is SavedOpportunitiesEvent.OnJobClicked -> Unit
            SavedOpportunitiesEvent.OnRefreshStatuses -> refreshStatuses()
            is SavedOpportunitiesEvent.OnRemoveSaved -> removeSaved(event.jobId)
        }
    }

    fun refreshStatuses() {
        _state.update { it.copy(errorMessage = null) }
        state.value.jobs.asSequence()
            .filter { it.status in REFRESHABLE_STATUSES }
            .map { it.id }
            .onEach(statusRefreshAttempted::add)
            .filter(statusRefreshRequested::add)
            .forEach(::refreshStatus)
    }

    private fun refreshStatus(jobId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshingStatus = true) }
            try {
                for (attempt in 0 until MAX_STATUS_REFRESH_ATTEMPTS) {
                    applications.syncApplicationStatus(jobId)
                    val status = applications.getApplicationByJobId(jobId)?.status
                    if (status == null || status !in PENDING_GENERATION_STATUSES) break
                    if (attempt < MAX_STATUS_REFRESH_ATTEMPTS - 1) delay(STATUS_REFRESH_DELAY_MS)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.update {
                    it.copy(errorMessage = "Some saved-job statuses could not be refreshed. Check your connection and retry.")
                }
            } finally {
                statusRefreshRequested.remove(jobId)
                _state.update { it.copy(isRefreshingStatus = statusRefreshRequested.isNotEmpty()) }
            }
        }
    }

    private fun removeSaved(jobId: String) {
        viewModelScope.launch {
            try {
                repository.updateJobStatus(jobId, JobStatus.UNSEEN)
                _state.update { it.copy(errorMessage = null) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.update { it.copy(errorMessage = "This job could not be removed from saved items. Try again.") }
            }
        }
    }

    private fun formatSalary(min: Double?, max: Double?, currency: String?): String {
        if (min == null && max == null) return "Salary undisclosed"
        val code = currency?.takeIf { it.isNotBlank() } ?: return "Salary listed"
        val low = min?.toLong()?.toString() ?: "—"
        val high = max?.toLong()?.toString()
        return if (high == null) "$code $low" else "$code $low–$high"
    }

    private fun displayStatus(application: com.jobchef.core.domain.model.TailoredApplication): String =
        when (application.status) {
            GenerationStatus.WAITING_FOR_RESUME -> "Waiting for resume"
            GenerationStatus.QUEUED -> "Queued"
            GenerationStatus.PROCESSING -> "Tailoring"
            GenerationStatus.READY -> if (application.demoMode) "Demo preview" else "Ready"
            GenerationStatus.FAILED -> "Failed"
            GenerationStatus.QUOTA_BLOCKED -> "Daily limit reached"
            GenerationStatus.CANCELLED -> "Cancelled"
        }

    private companion object {
        val REFRESHABLE_STATUSES = setOf("Waiting for resume", "Queued", "Tailoring")
        val PENDING_GENERATION_STATUSES = setOf(
            GenerationStatus.WAITING_FOR_RESUME,
            GenerationStatus.QUEUED,
            GenerationStatus.PROCESSING
        )
        const val MAX_STATUS_REFRESH_ATTEMPTS = 6
        const val STATUS_REFRESH_DELAY_MS = 2_500L
    }
}
