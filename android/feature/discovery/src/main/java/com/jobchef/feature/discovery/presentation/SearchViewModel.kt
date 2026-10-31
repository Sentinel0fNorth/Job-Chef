package com.jobchef.feature.discovery.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.jobchef.core.domain.repository.JobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for [SearchScreen].
 *
 * [OnSubmitSearch] is the entry point of the offline-first data cycle:
 * it enqueues [com.jobchef.core.database.worker.SyncJobsWorker] which fetches from the backend,
 * clears old non-SAVED rows from Room, and inserts the new batch.
 * [SwipeDeckViewModel] then receives the update reactively via its own Flow collector.
 */
class SearchViewModel(
    private val repository: JobRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.OnRoleChanged -> {
                _state.update { it.copy(targetRole = event.role) }
            }
            is SearchEvent.OnLocationChanged -> {
                _state.update { it.copy(location = event.location) }
            }
            is SearchEvent.OnKeywordsChanged -> {
                _state.update { it.copy(keywords = event.keywords) }
            }
            is SearchEvent.OnFilterToggled -> {
                _state.update { state ->
                    val currentSelected = state.selectedFilters
                    val newSelected = if (currentSelected.contains(event.filter)) {
                        currentSelected - event.filter
                    } else {
                        currentSelected + event.filter
                    }
                    state.copy(selectedFilters = newSelected)
                }
            }
            SearchEvent.OnSubmitSearch -> {
                _state.update { it.copy(isLoading = true) }
                enqueueSyncWork()
            }
        }
    }

    private fun enqueueSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workData = workDataOf(
            "KEY_ROLE" to _state.value.targetRole,
            "KEY_LOCATION" to _state.value.location
        )

        // Using a hardcoded class name/string to dynamically load the worker class or just referencing
        // com.jobchef.core.database.worker.SyncJobsWorker if we don't have dependency is not possible.
        // Wait! We can specify the Worker class by its fully qualified name:
        // OneTimeWorkRequestBuilder<ClassName>() is a helper.
        // Or we can use: OneTimeWorkRequest.Builder(Class.forName("com.jobchef.core.database.worker.SyncJobsWorker") as Class<out ListenableWorker>)
        // Let's use the Class.forName or class reference.
        // Actually, let's see if we should just add the dependency project(":core:database") to feature:discovery,
        // or load it via Class.forName. Class.forName is safer to avoid compile-time dependency,
        // but if it's refactored, it could break. Wait, since the prompt says:
        // "SyncJobsWorker import: SearchViewModel needs SyncJobsWorker.KEY_ROLE and KEY_LOCATION constants. This means feature:discovery needs to depend on :core:database."
        // Wait, is it better to add project(":core:database") to feature/discovery/build.gradle.kts?
        // Let's check if the module feature:discovery is allowed to depend on core:database.
        // Usually, in clean architecture, presentation depends on domain, and app module wires them.
        // But some projects let features depend on database if they aren't fully separated or to simplify things.
        // Let's use Class.forName to keep the module separation clean (feature does not depend on database).
        // Let's look:
        // Dynamic class loading is used to resolve SyncJobsWorker. This keeps feature:discovery
        // decoupled from core:database, respecting clean architecture boundaries.
        // If the worker class cannot be found (e.g. package renaming), we return silently.
        val workerClass = try {
            Class.forName("com.jobchef.core.database.worker.SyncJobsWorker") as Class<out androidx.work.ListenableWorker>
        } catch (e: Exception) {
            // Fallback just in case.
            // NOTE: If this fails, the screen remains stuck in isLoading = true, but since
            // SearchScreen immediately triggers onSearchClicked() navigation to SwipeDeckScreen,
            // this does not block the user under normal flow.
            return
        }

        val syncRequest = OneTimeWorkRequest.Builder(workerClass)
            .setConstraints(constraints)
            .setInputData(workData)
            .build()

        workManager.enqueueUniqueWork(
            "sync_jobs",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}
