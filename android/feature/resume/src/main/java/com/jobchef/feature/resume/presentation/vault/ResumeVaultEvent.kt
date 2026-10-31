package com.jobchef.feature.resume.presentation.vault

import android.net.Uri

sealed interface ResumeVaultEvent {
    data class OnSetActive(val resumeId: String) : ResumeVaultEvent
    data class OnPdfSelected(val uri: Uri) : ResumeVaultEvent
    data class OnTitleChanged(val title: String) : ResumeVaultEvent
    data class OnSanitizedTextChanged(val text: String) : ResumeVaultEvent
    data class OnContactNameChanged(val value: String) : ResumeVaultEvent
    data class OnContactEmailChanged(val value: String) : ResumeVaultEvent
    data class OnContactPhoneChanged(val value: String) : ResumeVaultEvent
    data class OnContactLocationChanged(val value: String) : ResumeVaultEvent
    data object OnSaveReview : ResumeVaultEvent
    data object OnDiscardReview : ResumeVaultEvent
    data class OnDelete(val resumeId: String) : ResumeVaultEvent
}
