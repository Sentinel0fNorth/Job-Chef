package com.jobchef.core.designsystem.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A central coordinator for app-wide UI notification events.
 * ViewModels can inject this manager to propagate non-persistent messages
 * (like error snackbars) to the root UI container (MainActivity) without
 * routing them through screen-specific UI states.
 */
class UiNotificationManager {
    private val _errors = MutableSharedFlow<String>()
    
    /**
     * Shared flow of error messages to be observed by the root UI scaffold/host.
     */
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    /**
     * Emits a new error message to be displayed as a snackbar.
     */
    suspend fun showError(message: String) {
        _errors.emit(message)
    }
}
