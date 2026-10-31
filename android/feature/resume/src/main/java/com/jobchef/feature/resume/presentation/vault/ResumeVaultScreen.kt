package com.jobchef.feature.resume.presentation.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jobchef.core.designsystem.components.ScreenHeader
import com.jobchef.core.designsystem.theme.JobMatcherTheme

data class ResumeVaultState(
    val resumes: List<ResumeItemState> = emptyList(),
    val pendingDraft: ResumeDraft? = null,
    val titleDraft: String = "",
    val sanitizedTextDraft: String = "",
    val contactNameDraft: String = "",
    val contactEmailDraft: String = "",
    val contactPhoneDraft: String = "",
    val contactLocationDraft: String = "",
    val isImporting: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

data class ResumeItemState(
    val id: String,
    val title: String,
    val updated: String,
    val tags: List<String>,
    val active: Boolean,
    val syncPending: Boolean,
    val characterCount: Int
)

@Composable
fun ResumeVaultScreen(
    state: ResumeVaultState,
    onEvent: (ResumeVaultEvent) -> Unit,
    onPickPdf: () -> Unit,
    onRevokeDevice: () -> Unit = {},
    isRevokingDevice: Boolean = false,
    deviceAccessError: String? = null,
    modifier: Modifier = Modifier
) {
    var showRevokeConfirmation by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenHeader(
            eyebrow = "01 — Vault",
            title = "The Resume Vault",
            subtitle = "Your source files and contact details stay on this device.",
            modifier = Modifier.padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 32.dp)
        )
        TextButton(
            onClick = { showRevokeConfirmation = true },
            enabled = !isRevokingDevice,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) { Text(if (isRevokingDevice) "Revoking device access…" else "Revoke device access") }
        deviceAccessError?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        UploadArea(
            onUploadClicked = onPickPdf,
            isImporting = state.isImporting,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        state.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp)
            )
        }

        if (showRevokeConfirmation) {
            AlertDialog(
                onDismissRequest = { showRevokeConfirmation = false },
                title = { Text("Revoke this device?") },
                text = {
                    Text(
                        "The server credential will be revoked and this app will return to enrollment. Local resumes remain on this device; a new enrollment code is required to sync again."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showRevokeConfirmation = false
                            onRevokeDevice()
                        },
                        enabled = !isRevokingDevice
                    ) { Text("Revoke device") }
                },
                dismissButton = {
                    TextButton(onClick = { showRevokeConfirmation = false }) { Text("Cancel") }
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "LIBRARY · ${state.resumes.size.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (state.resumes.isEmpty()) {
                item {
                    Text(
                        text = "No resumes yet. Import a text-based PDF to create your on-device vault.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(state.resumes, key = { it.id }) { resume ->
                ResumeCard(
                    resume = resume,
                    onClick = { onEvent(ResumeVaultEvent.OnSetActive(resume.id)) },
                    onDelete = { onEvent(ResumeVaultEvent.OnDelete(resume.id)) }
                )
            }
        }
    }

    val draft = state.pendingDraft
    if (draft != null) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isSaving) onEvent(ResumeVaultEvent.OnDiscardReview)
            },
            title = { Text("Review before sync") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Only the reviewed, sanitized text is uploaded. The original PDF and detected contact details remain local.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = state.titleDraft,
                        onValueChange = { onEvent(ResumeVaultEvent.OnTitleChanged(it)) },
                        label = { Text("Resume title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.contactNameDraft,
                        onValueChange = { onEvent(ResumeVaultEvent.OnContactNameChanged(it)) },
                        label = { Text("Name (kept on device)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.contactEmailDraft,
                        onValueChange = { onEvent(ResumeVaultEvent.OnContactEmailChanged(it)) },
                        label = { Text("Email (kept on device)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.contactPhoneDraft,
                        onValueChange = { onEvent(ResumeVaultEvent.OnContactPhoneChanged(it)) },
                        label = { Text("Phone (kept on device)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.contactLocationDraft,
                        onValueChange = { onEvent(ResumeVaultEvent.OnContactLocationChanged(it)) },
                        label = { Text("Location (kept on device)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.sanitizedTextDraft,
                        onValueChange = { onEvent(ResumeVaultEvent.OnSanitizedTextChanged(it)) },
                        label = { Text("Sanitized resume text") },
                        minLines = 8,
                        maxLines = 12,
                        modifier = Modifier.fillMaxWidth()
                    )
                    state.errorMessage?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onEvent(ResumeVaultEvent.OnSaveReview) },
                    enabled = !state.isSaving && state.titleDraft.isNotBlank() && state.sanitizedTextDraft.length >= 80
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Save on device")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onEvent(ResumeVaultEvent.OnDiscardReview) },
                    enabled = !state.isSaving
                ) { Text("Discard") }
            }
        )
    }
}

@Composable
fun UploadArea(
    onUploadClicked: () -> Unit,
    isImporting: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = !isImporting) { onUploadClicked() }
            .padding(vertical = 24.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            if (isImporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.FileUpload, "Choose PDF", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = if (isImporting) "Reading PDF…" else "Import a text-based PDF",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Up to 10 MB and 20 pages · no OCR",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResumeCard(
    resume: ResumeItemState,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(
                1.dp,
                if (resume.active) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.05f),
                MaterialTheme.shapes.medium
            )
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Description, contentDescription = "Resume", tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(resume.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${resume.updated} · ${if (resume.syncPending) "saved on device, sync pending" else "synced"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete resume")
            }
        }

        if (resume.active) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "ACTIVE RESUME · ${resume.characterCount} sanitized characters",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (resume.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    resume.tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(tag, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Tap to make active", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResumeVaultScreenPreview() {
    JobMatcherTheme {
        ResumeVaultScreen(state = ResumeVaultState(), onEvent = {}, onPickPdf = {})
    }
}
