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
    val filters: List<String> = listOf("All", "Ready", "Tailoring"),
    val jobs: List<SavedJobItem> = emptyList()
)

data class SavedJobItem(
    val company: String,
    val role: String,
    val logo: String,
    val location: String,
    val pay: String,
    val status: JobStatus,
    val matchScore: Int
)

enum class JobStatus {
    READY, TAILORING
}

val dummySavedJobsState = SavedJobsState(
    jobs = listOf(
        SavedJobItem("Nimbus Labs", "SDE Intern", "N", "Jaipur · Hybrid", "₹40k/mo", JobStatus.READY, 94),
        SavedJobItem("Vector & Co.", "Backend Engineer I", "V", "Bangalore · On-site", "₹18 LPA", JobStatus.TAILORING, 88),
        SavedJobItem("Helio Systems", "Android Developer", "H", "Remote · Full-time", "₹14 LPA", JobStatus.READY, 91),
        SavedJobItem("Forge Bank", "Java Developer", "F", "Mumbai · Hybrid", "₹16 LPA", JobStatus.TAILORING, 82),
        SavedJobItem("Orbit Health", "Full-Stack Engineer", "O", "Remote · Contract", "$45/hr", JobStatus.READY, 87)
    )
)

/**
 * Displays the list of saved job opportunities with filtering options.
 *
 * @param state The current UI state containing saved jobs and filters.
 * @param modifier Modifier for styling the layout.
 */
@Composable
fun SavedOpportunitiesScreen(
    state: SavedJobsState = dummySavedJobsState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Screen Header
        ScreenHeader(
            eyebrow = "04 — Shortlist",
            title = "Saved opportunities",
            subtitle = "Five roles in motion. Two ready to send.",
            modifier = Modifier.padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 16.dp)
        )

        // Filters
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
                        .clickable { }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
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

        // List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.jobs) { job ->
                SavedJobCard(job = job)
            }
        }
    }
}



/**
 * Individual job card displaying company, role, match score, and current status.
 *
 * @param job The data class representing the saved job.
 * @param modifier Modifier for styling the layout.
 */
@Composable
fun SavedJobCard(
    job: SavedJobItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.05f),
                shape = MaterialTheme.shapes.large
            )
            .clickable { }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = job.logo,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
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
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val statusColor = if (job.status == JobStatus.READY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = job.status.name,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (job.status == JobStatus.READY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "· ${job.pay}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = "${job.matchScore}%",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SavedOpportunitiesScreenPreview() {
    JobMatcherTheme {
        SavedOpportunitiesScreen()
    }
}
