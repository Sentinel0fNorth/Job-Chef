package com.jobchef.feature.saved.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jobchef.core.designsystem.components.ScreenHeader
import com.jobchef.core.designsystem.theme.JobMatcherTheme

data class SavedJobsState(
    val activeFilter: String = "All",
    val filters: List<String> = listOf("All", "In progress", "Ready", "Needs attention"),
    val jobs: List<SavedJobItem> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshingStatus: Boolean = false,
    val errorMessage: String? = null
)

data class SavedJobItem(
    val id: String,
    val company: String,
    val role: String,
    val logo: String,
    val location: String,
    val pay: String,
    val status: String,
    val matchScore: Int,
    val matchMethod: String
)

@Composable
fun SavedOpportunitiesScreen(
    state: SavedJobsState,
    onEvent: (SavedOpportunitiesEvent) -> Unit,
    onJobDetailsClick: (SavedJobItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenHeader(
            eyebrow = "04 — Shortlist",
            title = "Saved opportunities",
            subtitle = "${state.jobs.size} saved job${if (state.jobs.size == 1) "" else "s"} · generation status is shown as reported by the backend.",
            modifier = Modifier.padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.filters.forEach { filter ->
                val isSelected = filter == state.activeFilter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                        .border(
                            width = if (isSelected) 0.dp else 1.dp,
                            color = if (isSelected) Color.Transparent else Color.Black.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable { onEvent(SavedOpportunitiesEvent.OnFilterSelected(filter)) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
            }
            TextButton(
                onClick = { onEvent(SavedOpportunitiesEvent.OnRefreshStatuses) },
                enabled = !state.isRefreshingStatus
            ) {
                Text(if (state.isRefreshingStatus) "Refreshing…" else "Refresh statuses")
            }
        }

        val filteredJobs = state.jobs.filter { job ->
            when (state.activeFilter) {
                "In progress" -> job.status in setOf("Waiting for resume", "Queued", "Tailoring")
                "Ready" -> job.status == "Ready" || job.status == "Demo preview"
                "Needs attention" -> job.status == "Failed" || job.status == "Daily limit reached"
                else -> true
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredJobs.isEmpty()) {
                item {
                    Text(
                        text = if (state.isLoading) "Loading saved opportunities…" else "No saved opportunities in this filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(filteredJobs, key = { it.id }) { job ->
                SavedJobCard(
                    job = job,
                    onClick = {
                        onEvent(SavedOpportunitiesEvent.OnJobClicked(job))
                        onJobDetailsClick(job)
                    },
                    onRemove = { onEvent(SavedOpportunitiesEvent.OnRemoveSaved(job.id)) }
                )
            }
        }
    }
}

@Composable
fun SavedJobCard(
    job: SavedJobItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Color.Black.copy(alpha = 0.05f), MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(job.logo, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = job.role,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${job.company} · ${job.location}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                val isReady = job.status == "Ready" || job.status == "Demo preview"
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = job.status,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = job.pay,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (job.matchScore > 0) "${job.matchScore}% match" else "Match unavailable",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        TextButton(onClick = onRemove) { Text("Remove") }
        Spacer(modifier = Modifier.width(4.dp))
        Icon(Icons.Default.ChevronRight, contentDescription = "Open saved job", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
    }
}

@Preview(showBackground = true)
@Composable
private fun SavedOpportunitiesScreenPreview() {
    JobMatcherTheme {
        SavedOpportunitiesScreen(state = SavedJobsState(isLoading = false), onEvent = {}, onJobDetailsClick = {})
    }
}
