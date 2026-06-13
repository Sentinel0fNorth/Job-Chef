package com.jobchef.feature.resume.presentation.vault

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ResumeVaultViewModel : ViewModel() {

    private val _state = MutableStateFlow(dummyResumeState)
    val state: StateFlow<ResumeVaultState> = _state.asStateFlow()

    fun onEvent(event: ResumeVaultEvent) {
        when (event) {
            is ResumeVaultEvent.OnSetActive -> {
                _state.update { currentState ->
                    val updatedResumes = currentState.resumes.map {
                        it.copy(active = it.title == event.resumeTitle)
                    }
                    currentState.copy(resumes = updatedResumes)
                }
            }
            ResumeVaultEvent.OnUploadPdfClicked -> {
                // Future integration hook
            }
        }
    }
}
