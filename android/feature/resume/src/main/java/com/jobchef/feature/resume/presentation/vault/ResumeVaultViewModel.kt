package com.jobchef.feature.resume.presentation.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jobchef.core.designsystem.util.UiNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Resume Vault screen.
 * Handles toggling active status of resumes and injects a hardcoded resume stub
 * into the state upon request to facilitate offline MVP workflow verification.
 */
class ResumeVaultViewModel(
    private val uiNotificationManager: UiNotificationManager
) : ViewModel() {

    private val _state = MutableStateFlow(dummyResumeState)
    
    /**
     * UI state flow containing the list of resumes and any parsed resume text.
     */
    val state: StateFlow<ResumeVaultState> = _state.asStateFlow()

    /**
     * Processes incoming user/UI events.
     */
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
                viewModelScope.launch {
                    try {
                        _state.update { currentState ->
                            val newResume = ResumeItemState(
                                title = "Imported Resume (Stub)",
                                updated = "Updated Just now",
                                tags = listOf("Kotlin", "Compose", "Android"),
                                active = true
                            )
                            val updatedResumes = currentState.resumes.map {
                                it.copy(active = false)
                            } + newResume

                            currentState.copy(
                                resumes = updatedResumes,
                                parsedText = HARDCODED_RESUME
                            )
                        }
                    } catch (e: Exception) {
                        uiNotificationManager.showError("Failed to upload/stub resume: ${e.localizedMessage}")
                    }
                }
            }
        }
    }

    companion object {
        /**
         * Hardcoded realistic software engineering resume string used as a placeholder
         * for testing the local PII masking and generation pipeline offline.
         */
        private val HARDCODED_RESUME = """
            John Doe
            Software Engineer
            Email: john.doe@email.com
            Phone: (555) 123-4567
            
            Summary:
            Experienced Software Engineer with a strong background in Kotlin, Java, and Android development.
            Proven track record of delivering high-quality, scalable mobile and backend solutions.
            
            Experience:
            - Senior Android Developer at TechCorp (2022 - Present)
              Designed and implemented modular Android applications using Jetpack Compose and Koin.
              Optimized network layers and local database syncing, improving load times by 40%.
            - Software Engineer at AppBuilders (2020 - 2022)
              Developed native Android apps and integrated RESTful APIs with Retrofit.
            
            Skills:
            Kotlin, Java, Jetpack Compose, Room, Koin, Retrofit, Git, CI/CD
        """.trimIndent()
    }
}
