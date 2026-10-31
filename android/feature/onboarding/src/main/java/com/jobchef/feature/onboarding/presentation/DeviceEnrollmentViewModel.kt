package com.jobchef.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jobchef.core.database.worker.ResumeOutboxSyncScheduler
import com.jobchef.core.network.api.JobChefApiService
import com.jobchef.core.network.model.DeviceRegistrationRequest
import com.jobchef.core.network.session.DeviceCredentialStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceEnrollmentState(
    val isRegistered: Boolean = false,
    val isRegistering: Boolean = false,
    val isRevoking: Boolean = false,
    val errorMessage: String? = null
)

class DeviceEnrollmentViewModel(
    private val api: JobChefApiService,
    private val credentials: DeviceCredentialStore,
    private val syncScheduler: ResumeOutboxSyncScheduler
) : ViewModel() {
    private val _state = MutableStateFlow(DeviceEnrollmentState())
    val state: StateFlow<DeviceEnrollmentState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                credentials.accessToken()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                credentials.clear()
            }
            credentials.hasAccessToken.collect { hasToken ->
                _state.update { current ->
                    current.copy(
                        isRegistered = hasToken,
                        isRegistering = if (hasToken) false else current.isRegistering
                    )
                }
            }
        }
    }

    fun register(enrollmentCode: String) {
        val code = enrollmentCode.trim()
        if (code.isBlank() || code.length > 200 || _state.value.isRegistering) {
            _state.value = _state.value.copy(errorMessage = "Enter the enrollment code provided by the project administrator.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isRegistering = true, errorMessage = null)
            try {
                val response = api.registerDevice(DeviceRegistrationRequest(code))
                require(response.deviceId.isNotBlank() && response.accessToken.isNotBlank()) {
                    "The registration response was incomplete."
                }
                credentials.storeAccessToken(response.accessToken)
                syncScheduler.schedule()
                _state.value = DeviceEnrollmentState(isRegistered = true)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.value = DeviceEnrollmentState(
                    errorMessage = "Registration failed. Check the code and network connection, then try again."
                )
            }
        }
    }

    fun revoke() {
        if (!_state.value.isRegistered || _state.value.isRevoking) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isRevoking = true, errorMessage = null)
            try {
                api.revokeDevice()
                credentials.clear()
                _state.value = DeviceEnrollmentState()
            } catch (error: CancellationException) {
                throw error
            } catch (error: retrofit2.HttpException) {
                if (error.code() == 401) {
                    credentials.clear()
                    _state.value = DeviceEnrollmentState(
                        errorMessage = "The device credential was already expired or revoked. Register again with a new code."
                    )
                } else {
                    _state.value = _state.value.copy(
                        isRevoking = false,
                        errorMessage = "Device access could not be revoked. Check your connection and retry."
                    )
                }
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    isRevoking = false,
                    errorMessage = "Device access could not be revoked. Check your connection and retry."
                )
            }
        }
    }
}
