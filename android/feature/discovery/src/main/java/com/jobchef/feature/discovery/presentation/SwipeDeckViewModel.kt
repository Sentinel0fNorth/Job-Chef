package com.jobchef.feature.discovery.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import com.jobchef.core.domain.model.JobStatus
import com.jobchef.core.domain.repository.JobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for [SwipeDeckScreen].
 *
 * Implements the offline-first loop:
 * 1. Collects [JobRepository.getUnseenJobs] reactively — the UI updates automatically
 *    whenever Room emits a new batch (e.g. after [SearchViewModel] triggers a refresh).
 * 2. Swipe gestures dispatch [SwipeDeckEvent] which mutate [JobStatus] via the repository,
 *    which in turn causes Room to emit a new list, completing the MVI loop.
 */
class SwipeDeckViewModel(
    private val repository: JobRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SwipeDeckState(isLoading = true))
    val state: StateFlow<SwipeDeckState> = _state.asStateFlow()
    private var lastSwipedJobId: String? = null

    init {
        observeUnseenJobs()
    }

    private fun observeUnseenJobs() {
        repository.getUnseenJobs()
            .onEach { jobs ->
                _state.update { it.copy(jobs = jobs, isLoading = false, error = null) }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: SwipeDeckEvent) {
        when (event) {
            SwipeDeckEvent.OnLike -> swipeCurrentCard(JobStatus.SAVED)
            SwipeDeckEvent.OnReject -> swipeCurrentCard(JobStatus.REJECTED)
            SwipeDeckEvent.OnRewind -> rewind()
        }
    }

    private fun swipeCurrentCard(newStatus: JobStatus) {
        val currentJob = _state.value.jobs.getOrNull(_state.value.currentIndex) ?: return
        viewModelScope.launch {
            try {
                repository.updateJobStatus(currentJob.id, newStatus)
                lastSwipedJobId = currentJob.id
                _state.update { it.copy(canUndo = true, error = null) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.update { it.copy(error = "The decision could not be saved on this device.") }
            }
        }
    }

    private fun rewind() {
        val jobId = lastSwipedJobId ?: return
        viewModelScope.launch {
            try {
                repository.updateJobStatus(jobId, JobStatus.UNSEEN)
                lastSwipedJobId = null
                _state.update { it.copy(canUndo = false, error = null) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.update { it.copy(error = "The last decision could not be undone.") }
            }
        }
    }
}
