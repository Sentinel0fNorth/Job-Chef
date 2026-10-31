package com.jobchef.feature.tailor.presentation.export

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.jobchef.core.designsystem.components.ScreenHeader
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ExportScreen(
    jobId: String,
    modifier: Modifier = Modifier
) {
    val viewModel: ExportViewModel = koinViewModel(parameters = { parametersOf(jobId) })
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val saveResume = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) viewModel.save(ExportKind.RESUME, uri)
    }
    val saveLetter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) viewModel.save(ExportKind.COVER_LETTER, uri)
    }

    LaunchedEffect(state.shareUri) {
        val uri = state.shareUri ?: return@LaunchedEffect
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(send, "Share generated PDF"))
        } catch (_: ActivityNotFoundException) {
            viewModel.shareFailed()
        } finally {
            viewModel.clearShareUri()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        ScreenHeader(
            eyebrow = "06 — EXPORT",
            title = "Application documents",
            subtitle = "Review the generated content. PDFs are built on this device with contact details from the matching local resume.",
            modifier = Modifier.padding(top = 48.dp, bottom = 20.dp, start = 24.dp, end = 24.dp)
        )

        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Text(state.status, modifier = Modifier.padding(top = 12.dp), textAlign = TextAlign.Center)
            }
        } else {
            Text(
                text = "Application status: ${state.status}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            state.statusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
            if (state.status == "Daily limit reached") {
                Text(
                    text = "No paid fallback was attempted. After the daily allowance resets, remove and save this job again to retry the same draft.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
            if (state.demoMode) {
                Text(
                    text = "Synthetic demo output — not for application use.",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
            if (state.previewPages.isNotEmpty() || state.canEditDraft) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(ExportKind.RESUME, ExportKind.COVER_LETTER).forEach { kind ->
                        if (state.selectedKind == kind) {
                            Button(onClick = { viewModel.selectKind(kind) }) {
                                Text(if (kind == ExportKind.RESUME) "Resume" else "Cover letter")
                            }
                        } else {
                            OutlinedButton(onClick = { viewModel.selectKind(kind) }) {
                                Text(if (kind == ExportKind.RESUME) "Resume" else "Cover letter")
                            }
                        }
                    }
                }
                if (state.previewPages.isNotEmpty()) {
                    DocumentPreview(pages = state.previewPages, modifier = Modifier.padding(horizontal = 24.dp))
                }
                Text(
                    text = if (state.draftDirty) {
                        "Local edits are not included in the PDF until you update the preview."
                    } else {
                        "Edit this document locally. Contact details remain on this device."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp)
                )
                OutlinedTextField(
                    value = state.draftText,
                    onValueChange = viewModel::updateDraft,
                    enabled = !state.isWorking,
                    label = { Text("Document text") },
                    minLines = 10,
                    maxLines = 24,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    supportingText = { Text("${state.draftText.length} / 50,000 characters") }
                )
                Button(
                    onClick = viewModel::updatePreview,
                    enabled = state.draftDirty && !state.isWorking,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(if (state.isWorking) "Rendering preview…" else "Update PDF preview")
                }
            }

            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
                TextButton(
                    onClick = viewModel::refresh,
                    enabled = !state.isWorking,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Retry loading application") }
            }
            state.successMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }

            if (state.canExport) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (state.selectedKind == ExportKind.RESUME) {
                                saveResume.launch("JobChef_resume.pdf")
                            } else {
                                saveLetter.launch("JobChef_cover_letter.pdf")
                            }
                        },
                        enabled = !state.isWorking,
                        modifier = Modifier.weight(1f)
                    ) { Text("Save PDF") }
                    OutlinedButton(
                        onClick = { viewModel.share(state.selectedKind) },
                        enabled = !state.isWorking,
                        modifier = Modifier.weight(1f)
                    ) { Text("Share PDF") }
                }
            }
            if (state.canRegenerate) {
                TextButton(
                    onClick = viewModel::regenerate,
                    enabled = !state.isWorking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isWorking) "Working…" else "Regenerate (daily limit applies)")
                }
            }
            if (state.canDelete) {
                TextButton(
                    onClick = { showDeleteConfirmation = true },
                    enabled = !state.isWorking,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Delete generated data and saved job", color = MaterialTheme.colorScheme.error) }
            }
            state.originalJobUrl?.takeIf(String::isNotBlank)?.let { url ->
                TextButton(
                    onClick = {
                        val uri = runCatching { url.toUri() }.getOrNull()
                        if (uri == null || uri.scheme != "https" || uri.host.isNullOrBlank()
                            || uri.userInfo != null
                        ) {
                            viewModel.jobLinkFailed()
                        } else {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                .onFailure { viewModel.jobLinkFailed() }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open original job listing") }
            }
        }
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete this application?") },
                text = {
                    Text("This removes generated drafts and private previews from this project. The deletion will sync when this device is online.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmation = false
                            viewModel.deleteApplication()
                        }
                    ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) { Text("Keep it") }
                }
            )
        }
    }
}

@Composable
private fun DocumentPreview(pages: List<android.graphics.Bitmap>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(24.dp)
    ) {
        pages.forEachIndexed { index, bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Rendered PDF page ${index + 1} of ${pages.size}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
