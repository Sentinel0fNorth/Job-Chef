package com.jobchef.feature.resume.presentation.vault

sealed interface ResumeVaultEvent {
    data class OnSetActive(val resumeTitle: String) : ResumeVaultEvent
    data object OnUploadPdfClicked : ResumeVaultEvent
}
