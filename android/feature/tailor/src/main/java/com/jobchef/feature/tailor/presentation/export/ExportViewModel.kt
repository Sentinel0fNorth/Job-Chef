package com.jobchef.feature.tailor.presentation.export

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jobchef.core.database.dao.BaseResumeDao
import com.jobchef.core.database.dao.JobDao
import com.jobchef.core.database.entity.BaseResumeEntity
import com.jobchef.core.database.entity.JobEntity
import com.jobchef.core.database.entity.TailoredApplicationEntity
import com.jobchef.core.domain.repository.TailoredApplicationRepository
import com.jobchef.core.network.api.JobChefApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.File

data class ExportScreenState(
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val status: String = "Loading application status…",
    val statusMessage: String? = null,
    val demoMode: Boolean = false,
    val originalJobUrl: String? = null,
    val selectedKind: ExportKind = ExportKind.RESUME,
    val canEditDraft: Boolean = false,
    val draftText: String = "",
    val draftDirty: Boolean = false,
    val previewPages: List<Bitmap> = emptyList(),
    val canExport: Boolean = false,
    val canRegenerate: Boolean = false,
    val canDelete: Boolean = false,
    val shareUri: Uri? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class ExportViewModel(
    private val jobId: String,
    private val context: Context,
    private val applications: TailoredApplicationRepository,
    private val jobs: JobDao,
    private val resumes: BaseResumeDao,
    private val api: JobChefApiService,
    private val documents: DocumentExportService
) : ViewModel() {
    private val _state = MutableStateFlow(ExportScreenState())
    val state: StateFlow<ExportScreenState> = _state.asStateFlow()
    private var loaded: LoadedExport? = null
    private val drafts = mutableMapOf<ExportKind, String>()
    private var shareFile: File? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true, isWorking = false, errorMessage = null, successMessage = null
            )
            try {
                var application: TailoredApplicationEntity? = null
                for (attempt in 0 until 6) {
                    try {
                        applications.syncApplicationStatus(jobId)
                    } catch (error: HttpException) {
                        if (error.code() != 404) throw error
                    }
                    application = applications.getApplicationByJobId(jobId)?.toEntityForExport()
                    if (application?.status in TERMINAL_OR_EXPORTABLE) break
                    if (attempt < 5) delay(POLL_DELAY_MS)
                }
                val job = withContext(Dispatchers.IO) { jobs.getJob(jobId) }
                    ?: error("The saved job is no longer available on this device.")
                val applicationForExport = application
                    ?: error("The saved job has not reached the server yet. Check your connection and retry.")
                val previous = loaded?.application
                if (previous == null
                    || previous.id != applicationForExport.id
                    || previous.generationVersion != applicationForExport.generationVersion
                ) {
                    drafts.clear()
                }
                val sourceResume = if (applicationForExport.status == "READY") {
                    val remoteResumeId = applicationForExport.resumeId
                        ?: error("The server did not report which resume version was used.")
                    val version = applicationForExport.resumeVersion
                        ?: error("The server did not report which resume version was used.")
                    withContext(Dispatchers.IO) { resumes.getByRemoteVersion(remoteResumeId, version) }
                        ?: error("The source resume is not available locally. This device cannot recover resumes from another installation.")
                } else {
                    null
                }
                loaded = LoadedExport(applicationForExport, job, sourceResume)
                val draft = if (sourceResume != null) {
                    drafts[ExportKind.RESUME] ?: documents.preview(
                        ExportKind.RESUME, applicationForExport, job
                    ).joinToString("\n") { it.text }.also { drafts[ExportKind.RESUME] = it }
                } else {
                    ""
                }
                val preview = if (sourceResume != null) {
                    withContext(Dispatchers.IO) {
                        documents.renderPreviewPages(
                            ExportKind.RESUME, applicationForExport, job, sourceResume, draft
                        )
                    }
                } else {
                    emptyList()
                }
                _state.value = ExportScreenState(
                    isLoading = false,
                    status = displayStatus(applicationForExport.status),
                    statusMessage = applicationForExport.errorMessage,
                    demoMode = applicationForExport.demoMode,
                    originalJobUrl = job.redirectUrl,
                    canEditDraft = sourceResume != null && applicationForExport.status == "READY",
                    draftText = draft,
                    previewPages = preview,
                    canExport = sourceResume != null && applicationForExport.status == "READY",
                    canRegenerate = applicationForExport.status == "READY" || applicationForExport.status == "FAILED",
                    canDelete = true
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "The application is unavailable. Check your connection, then retry."
                )
            }
        }
    }

    fun regenerate() {
        val current = loaded?.application ?: return
        if (current.status != "READY" && current.status != "FAILED") return
        if (_state.value.draftDirty) {
            _state.value = _state.value.copy(
                errorMessage = "Update the preview before regenerating so local edits are not lost."
            )
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isWorking = true, errorMessage = null, successMessage = null)
            try {
                withContext(Dispatchers.IO) { api.regenerate(jobId) }
                loaded = null
                refresh()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    isWorking = false,
                    errorMessage = "Regeneration was not accepted. The daily limit may have been reached; retry later."
                )
            }
        }
    }

    fun deleteApplication() {
        if (!_state.value.canDelete || _state.value.isWorking) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isWorking = true, errorMessage = null, successMessage = null)
            try {
                applications.deleteApplication(jobId)
                loaded = null
                shareFile?.delete()
                shareFile = null
                _state.value = _state.value.copy(
                    isWorking = false,
                    status = "Deleted",
                    statusMessage = null,
                    canEditDraft = false,
                    draftText = "",
                    draftDirty = false,
                    previewPages = emptyList(),
                    canExport = false,
                    canRegenerate = false,
                    canDelete = false,
                    shareUri = null,
                    successMessage = "Generated application data was removed from this device. Server deletion is queued until connectivity is available."
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    isWorking = false,
                    errorMessage = "Application data could not be removed from this device."
                )
            }
        }
    }

    fun save(kind: ExportKind, destination: Uri) {
        val source = loaded ?: return
        if (_state.value.draftDirty) {
            _state.value = _state.value.copy(errorMessage = "Update the preview before saving this draft.")
            return
        }
        if (kind != _state.value.selectedKind) {
            _state.value = _state.value.copy(errorMessage = "Select and preview this document before exporting it.")
            return
        }
        val resume = source.sourceResume ?: run {
            _state.value = _state.value.copy(errorMessage = "The original contact details are not available locally.")
            return
        }
        val draft = _state.value.draftText
        viewModelScope.launch {
            _state.value = _state.value.copy(isWorking = true, errorMessage = null, successMessage = null)
            try {
                withContext(Dispatchers.IO) {
                    val file = documents.createPdf(kind, source.application, source.job, resume, draft)
                    try {
                        documents.copyTo(context.contentResolver, destination, file)
                    } finally {
                        file.delete()
                    }
                }
                _state.value = _state.value.copy(isWorking = false, successMessage = "PDF saved to the selected location.")
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.value = _state.value.copy(isWorking = false, errorMessage = "The PDF could not be saved.")
            }
        }
    }

    fun share(kind: ExportKind) {
        val source = loaded ?: return
        if (_state.value.draftDirty) {
            _state.value = _state.value.copy(errorMessage = "Update the preview before sharing this draft.")
            return
        }
        if (kind != _state.value.selectedKind) {
            _state.value = _state.value.copy(errorMessage = "Select and preview this document before sharing it.")
            return
        }
        val resume = source.sourceResume ?: run {
            _state.value = _state.value.copy(errorMessage = "The original contact details are not available locally.")
            return
        }
        val draft = _state.value.draftText
        viewModelScope.launch {
            _state.value = _state.value.copy(isWorking = true, errorMessage = null, successMessage = null)
            var file: File? = null
            try {
                val (readyFile, shareUri) = withContext(Dispatchers.IO) {
                    val generated = documents.createPdf(kind, source.application, source.job, resume, draft)
                    file = generated
                    generated to FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", generated
                    )
                }
                shareFile?.delete()
                shareFile = readyFile
                _state.value = _state.value.copy(isWorking = false, shareUri = shareUri)
                viewModelScope.launch {
                    delay(SHARE_FILE_RETENTION_MS)
                    if (shareFile == readyFile) {
                        readyFile.delete()
                        shareFile = null
                    }
                }
            } catch (error: CancellationException) {
                file?.delete()
                throw error
            } catch (_: Exception) {
                file?.delete()
                _state.value = _state.value.copy(isWorking = false, errorMessage = "The PDF could not be prepared for sharing.")
            }
        }
    }

    fun clearShareUri() {
        _state.value = _state.value.copy(shareUri = null)
    }

    fun selectKind(kind: ExportKind) {
        if (_state.value.isWorking || _state.value.selectedKind == kind) return
        val source = loaded ?: return
        val resume = source.sourceResume ?: return
        if (_state.value.draftDirty) {
            _state.value = _state.value.copy(
                errorMessage = "Update the current preview before switching document tabs."
            )
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isWorking = true,
                selectedKind = kind,
                draftText = drafts[kind].orEmpty(),
                previewPages = emptyList(),
                canExport = false,
                errorMessage = null
            )
            try {
                val draft = drafts[kind] ?: documents.preview(
                    kind, source.application, source.job
                ).joinToString("\n") { it.text }.also { drafts[kind] = it }
                _state.value = _state.value.copy(draftText = draft)
                val pages = withContext(Dispatchers.IO) {
                    documents.renderPreviewPages(kind, source.application, source.job, resume, draft)
                }
                _state.value = _state.value.copy(
                    isWorking = false, previewPages = pages, canExport = true
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    isWorking = false,
                    canExport = false,
                    errorMessage = "The generated PDF could not be rendered for preview."
                )
            }
        }
    }

    fun updateDraft(text: String) {
        if (!_state.value.canEditDraft || _state.value.isWorking) return
        if (text.length > MAX_DRAFT_CHARACTERS) {
            _state.value = _state.value.copy(
                errorMessage = "The edited draft exceeds the 50,000 character limit."
            )
            return
        }
        drafts[_state.value.selectedKind] = text
        _state.value = _state.value.copy(
            draftText = text,
            draftDirty = true,
            canExport = false,
            errorMessage = null
        )
    }

    fun updatePreview() {
        if (!_state.value.canEditDraft || !_state.value.draftDirty || _state.value.isWorking) return
        val source = loaded ?: return
        val resume = source.sourceResume ?: return
        val kind = _state.value.selectedKind
        val draft = _state.value.draftText
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isWorking = true, canExport = false, errorMessage = null, previewPages = emptyList()
            )
            try {
                val pages = withContext(Dispatchers.IO) {
                    documents.renderPreviewPages(kind, source.application, source.job, resume, draft)
                }
                _state.value = _state.value.copy(
                    isWorking = false,
                    draftDirty = false,
                    previewPages = pages,
                    canExport = true
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    isWorking = false,
                    errorMessage = "The edited PDF could not be rendered. The previous document was not exported."
                )
            }
        }
    }

    fun shareFailed() {
        shareFile?.delete()
        shareFile = null
        _state.value = _state.value.copy(
            shareUri = null,
            errorMessage = "No compatible PDF-sharing app is available on this device."
        )
    }

    fun jobLinkFailed() {
        _state.value = _state.value.copy(
            errorMessage = "The original job link is invalid or could not be opened."
        )
    }

    private fun com.jobchef.core.domain.model.TailoredApplication.toEntityForExport() =
        TailoredApplicationEntity(
            id = id,
            jobId = jobId,
            resumeId = resumeId,
            resumeVersion = resumeVersion,
            status = status.name,
            outputPdfUrl = outputPdfUrl,
            resumeContentJson = resumeContentJson,
            coverLetter = coverLetter,
            generationVersion = generationVersion,
            errorCode = errorCode,
            errorMessage = errorMessage,
            demoMode = demoMode,
            createdAt = createdAt
        )

    private fun displayStatus(status: String): String = when (status) {
        "WAITING_FOR_RESUME" -> "Waiting for an active resume"
        "QUEUED" -> "Queued"
        "PROCESSING" -> "Generating"
        "READY" -> "Ready"
        "FAILED" -> "Generation failed"
        "QUOTA_BLOCKED" -> "Daily limit reached"
        "CANCELLED" -> "Cancelled"
        else -> "Status unavailable"
    }

    private data class LoadedExport(
        val application: TailoredApplicationEntity,
        val job: JobEntity,
        val sourceResume: BaseResumeEntity?
    )

    private companion object {
        val TERMINAL_OR_EXPORTABLE = setOf("READY", "FAILED", "QUOTA_BLOCKED", "CANCELLED")
        const val POLL_DELAY_MS = 2_500L
        const val SHARE_FILE_RETENTION_MS = 10 * 60 * 1_000L
    }

    override fun onCleared() {
        shareFile?.delete()
        shareFile = null
        super.onCleared()
    }
}
