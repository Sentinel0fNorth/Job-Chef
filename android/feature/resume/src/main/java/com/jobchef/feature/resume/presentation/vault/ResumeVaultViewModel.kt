package com.jobchef.feature.resume.presentation.vault

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jobchef.feature.resume.data.ResumeVaultRepository
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResumeVaultViewModel(
    private val repository: ResumeVaultRepository,
    private val importProcessor: ResumeImportProcessor
) : ViewModel() {
    private val _state = MutableStateFlow(ResumeVaultState())
    val state: StateFlow<ResumeVaultState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeResumes().collect { stored ->
                _state.update { current ->
                    current.copy(
                        resumes = stored.map { resume ->
                            ResumeItemState(
                                id = resume.id,
                                title = resume.title,
                                updated = "Added ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(resume.createdAt))}",
                                tags = extractSkills(resume.sanitizedText),
                                active = resume.active,
                                syncPending = resume.syncPending,
                                characterCount = resume.sanitizedText.length
                            )
                        }
                    )
                }
            }
        }
    }

    fun onEvent(event: ResumeVaultEvent) {
        when (event) {
            is ResumeVaultEvent.OnSetActive -> runOperation {
                repository.setActive(event.resumeId)
            }
            is ResumeVaultEvent.OnPdfSelected -> importPdf(event.uri)
            is ResumeVaultEvent.OnTitleChanged ->
                _state.update { it.copy(titleDraft = event.title, errorMessage = null) }
            is ResumeVaultEvent.OnSanitizedTextChanged ->
                _state.update { it.copy(sanitizedTextDraft = event.text, errorMessage = null) }
            is ResumeVaultEvent.OnContactNameChanged ->
                _state.update { it.copy(contactNameDraft = event.value) }
            is ResumeVaultEvent.OnContactEmailChanged ->
                _state.update { it.copy(contactEmailDraft = event.value) }
            is ResumeVaultEvent.OnContactPhoneChanged ->
                _state.update { it.copy(contactPhoneDraft = event.value) }
            is ResumeVaultEvent.OnContactLocationChanged ->
                _state.update { it.copy(contactLocationDraft = event.value) }
            ResumeVaultEvent.OnSaveReview -> saveReview()
            ResumeVaultEvent.OnDiscardReview ->
                _state.update {
                    it.copy(
                        pendingDraft = null,
                        titleDraft = "",
                        sanitizedTextDraft = "",
                        contactNameDraft = "",
                        contactEmailDraft = "",
                        contactPhoneDraft = "",
                        contactLocationDraft = "",
                        errorMessage = null
                    )
                }
            is ResumeVaultEvent.OnDelete -> runOperation {
                repository.delete(event.resumeId)
            }
        }
    }

    private fun importPdf(uri: Uri) {
        if (_state.value.isImporting) return
        viewModelScope.launch {
            _state.update { it.copy(isImporting = true, errorMessage = null) }
            try {
                val draft = withContext(Dispatchers.IO) { importProcessor.import(uri) }
                _state.update {
                    it.copy(
                        isImporting = false,
                        pendingDraft = draft,
                        titleDraft = draft.title,
                        sanitizedTextDraft = draft.sanitizedText,
                        contactNameDraft = draft.contactName.orEmpty(),
                        contactEmailDraft = draft.contactEmail.orEmpty(),
                        contactPhoneDraft = draft.contactPhone.orEmpty(),
                        contactLocationDraft = draft.contactLocation.orEmpty()
                    )
                }
            } catch (error: IllegalArgumentException) {
                _state.update { it.copy(isImporting = false, errorMessage = error.message ?: "The PDF could not be imported.") }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.update {
                    it.copy(isImporting = false, errorMessage = "The PDF could not be read. Choose an unlocked, text-based PDF and try again.")
                }
            }
        }
    }

    private fun saveReview() {
        val current = _state.value
        val draft = current.pendingDraft ?: return
        val title = current.titleDraft.trim()
        val reviewedText = current.sanitizedTextDraft.trim()
        if (title.isEmpty() || title.length > 120) {
            _state.update { it.copy(errorMessage = "Enter a resume title of 1–120 characters.") }
            return
        }
        if (reviewedText.length < 80 || reviewedText.length > 50_000) {
            _state.update { it.copy(errorMessage = "Resume text must contain between 80 and 50,000 characters.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val sanitized = withContext(Dispatchers.Default) {
                    importProcessor.sanitizeReviewedText(reviewedText).trim()
                }
                if (sanitized.length < 80) {
                    _state.update { it.copy(isSaving = false, errorMessage = "The reviewed resume must contain at least 80 characters.") }
                    return@launch
                }
                repository.saveDraft(
                    draft.copy(
                        title = title,
                        sanitizedText = sanitized,
                        contactName = _state.value.contactNameDraft.trim().takeIf(String::isNotBlank),
                        contactEmail = _state.value.contactEmailDraft.trim().takeIf(String::isNotBlank),
                        contactPhone = _state.value.contactPhoneDraft.trim().takeIf(String::isNotBlank),
                        contactLocation = _state.value.contactLocationDraft.trim().takeIf(String::isNotBlank)
                    ),
                    active = _state.value.resumes.none { it.active }
                )
                _state.update {
                    it.copy(
                        isSaving = false,
                        pendingDraft = null,
                        titleDraft = "",
                        sanitizedTextDraft = "",
                        contactNameDraft = "",
                        contactEmailDraft = "",
                        contactPhoneDraft = "",
                        contactLocationDraft = ""
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.update { it.copy(isSaving = false, errorMessage = "The resume could not be saved locally.") }
            }
        }
    }

    private fun runOperation(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { block() }
                _state.update { it.copy(errorMessage = null) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.update { it.copy(errorMessage = "That resume change could not be saved locally.") }
            }
        }
    }

    private fun extractSkills(text: String): List<String> {
        val knownSkills = listOf(
            "Kotlin", "Java", "Python", "JavaScript", "TypeScript", "Android",
            "Spring Boot", "PostgreSQL", "SQL", "React", "AWS", "GCP", "Docker"
        )
        return knownSkills.filter { skill ->
            Regex("(?i)(?<![A-Za-z0-9])${Regex.escape(skill)}(?![A-Za-z0-9])").containsMatchIn(text)
        }.take(8)
    }
}
