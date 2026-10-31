package com.jobchef.feature.discovery.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jobchef.core.designsystem.theme.JobMatcherTheme
import com.jobchef.core.domain.model.Job
import com.jobchef.core.domain.model.JobStatus
import com.jobchef.core.domain.model.WorkMode
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/** Preview-only stub jobs — replaced by live Room data at runtime. */
private val previewJobs = listOf(
    Job(
        id = "preview-1",
        title = "Software Development Engineer Intern",
        company = "Nimbus Labs",
        location = "Jaipur, Rajasthan",
        description = "Work on cutting-edge microservices with Java and Spring Boot in a fast-paced product environment.",
        salaryMin = 40000.0,
        salaryMax = null,
        redirectUrl = "",
        skills = listOf("Java", "Spring Boot", "Kotlin", "Compose"),
        workMode = WorkMode.HYBRID,
        status = JobStatus.UNSEEN
    ),
    Job(
        id = "preview-2",
        title = "Backend Engineer I",
        company = "Vector & Co.",
        location = "Bangalore",
        description = "Design and maintain distributed systems handling millions of requests per day.",
        salaryMin = 1800000.0,
        salaryMax = null,
        redirectUrl = "",
        skills = listOf("Go", "PostgreSQL", "Docker"),
        workMode = WorkMode.ON_SITE,
        status = JobStatus.UNSEEN
    )
)

/**
 * The Swipe Deck screen displaying job opportunities as an interactive stack of cards.
 *
 * @param state The current UI state containing the list of jobs.
 * @param onEvent Callback for handling MVI user gestures/actions.
 * @param modifier Modifier for styling the layout.
 */
@Composable
fun SwipeDeckScreen(
    state: SwipeDeckState,
    onEvent: (SwipeDeckEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val total = state.jobs.size
    val currentIndex = state.currentIndex


    val scope = rememberCoroutineScope()
    var isSwipingAway by remember { mutableStateOf(false) }

    // Heavy spring physics for tactile feel
    val stiffness = 400f
    val dampingRatio = 0.8f

    val offsetAnim = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    val rotation = offsetAnim.value.x / 20f

    val density = LocalDensity.current
    val threshold = with(density) { 150.dp.toPx() }
    val swipeProgress = (offsetAnim.value.x.absoluteValue / threshold).coerceIn(0f, 1f)

    // Background gradient colors
    val radialGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF1f2937),
            Color(0xFF121212),
            Color(0xFF0a0a0a)
        ),
        radius = 1500f,
        center = Offset(500f, 0f)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(radialGradient)
    ) {
        if (state.jobs.isEmpty() || currentIndex >= state.jobs.size) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = "Scanning",
                    tint = Color(0xFF7D7D7D),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = if (state.isLoading) "Loading saved opportunities…" else "No unseen opportunities.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF7D7D7D),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (state.isLoading) "Your saved job batch is loading." else "Run a search to fetch another batch of up to 20 jobs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF7D7D7D).copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(error, color = MaterialTheme.colorScheme.error, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                if (state.canUndo) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = { onEvent(SwipeDeckEvent.OnRewind) }) {
                        Text("Undo last decision")
                    }
                }
            }
        } else {
            // Progress segments
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 20.dp, end = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 0 until total) {
                        val color = when {
                            i < currentIndex -> MaterialTheme.colorScheme.secondary // #A8B2C1
                            i == currentIndex -> Color.White
                            else -> Color.White.copy(alpha = 0.15f)
                        }
                        Box(
                            modifier = Modifier
                                .height(3.dp)
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(color)
                        )
                    }
                }
            }

            // Card Stack Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 112.dp, bottom = 176.dp, start = 24.dp, end = 24.dp)
            ) {
                // We draw up to 3 cards max: 2 back-to-front layers + 1 fading in
                for (i in 2 downTo 0) {
                    val jobIndex = currentIndex + i
                    if (jobIndex >= state.jobs.size) continue

                    val job: Job = state.jobs[jobIndex]
                    val effectiveLayer = i - swipeProgress

                    // If effectiveLayer is >= 2, the card is completely hidden.
                    if (effectiveLayer >= 2f) continue

                    val isTopCard = i == 0
                    val clampedLayer = if (isTopCard) 0f else effectiveLayer

                    val topPad = (24f * clampedLayer).dp
                    val horizontalPad = (12f * clampedLayer).dp
                    val bottomPad = (24f - 12f * clampedLayer).coerceAtLeast(0f).dp

                    val alphaProgress = when {
                        effectiveLayer <= 0f -> 1f // Top card remains fully opaque
                        effectiveLayer <= 1f -> 1f - (0.4f * effectiveLayer) // Layer 0 -> 1: 1.0 -> 0.6
                        else -> 0.6f * (2f - effectiveLayer) // Layer 1 -> 2: 0.6 -> 0.0 (fade in)
                    }

                    // Option 1 Deferred Content Loading: Text dynamically fades in as the card moves to position 1.
                    // At effectiveLayer >= 1f (Middle and Back cards), text is completely hidden (0f).
                    // As effectiveLayer goes from 1f -> 0f, contentAlpha goes from 0f -> 1f.
                    val contentAlpha = (1f - effectiveLayer).coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topPad, start = horizontalPad, end = horizontalPad, bottom = bottomPad)
                            .graphicsLayer { alpha = alphaProgress }
                            .then(
                                if (isTopCard) {
                                    Modifier
                                        .offset {
                                            IntOffset(offsetAnim.value.x.roundToInt(), offsetAnim.value.y.roundToInt())
                                        }
                                        .rotate(rotation)
                                        .pointerInput(Unit) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    if (isSwipingAway) return@detectDragGestures
                                                },
                                                onDragEnd = {
                                                    if (isSwipingAway) return@detectDragGestures
                                                    scope.launch {
                                                        if (offsetAnim.value.x > threshold || offsetAnim.value.x < -threshold) {
                                                            isSwipingAway = true
                                                            val direction = if (offsetAnim.value.x > 0) 1 else -1
                                                            offsetAnim.animateTo(
                                                                Offset(direction * 1000f, offsetAnim.value.y),
                                                                tween(300)
                                                            )
                                                            if (direction > 0) onEvent(SwipeDeckEvent.OnLike) else onEvent(SwipeDeckEvent.OnReject)
                                                            offsetAnim.snapTo(Offset.Zero)
                                                            isSwipingAway = false
                                                        } else {
                                                            offsetAnim.animateTo(
                                                                Offset.Zero,
                                                                spring(dampingRatio = dampingRatio, stiffness = stiffness)
                                                            )
                                                        }
                                                    }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    if (isSwipingAway) return@detectDragGestures
                                                    change.consume()
                                                    scope.launch {
                                                        offsetAnim.snapTo(offsetAnim.value + dragAmount)
                                                    }
                                                }
                                            )
                                        }
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        DeckCardItem(job = job, contentAlpha = contentAlpha)
                    }
                }
            }

            // Action Row
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 96.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reject Button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .clickable {
                            if (!isSwipingAway && currentIndex < state.jobs.size) {
                                scope.launch {
                                    isSwipingAway = true
                                    offsetAnim.animateTo(Offset(-1000f, 0f), tween(300))
                                    onEvent(SwipeDeckEvent.OnReject)
                                    offsetAnim.snapTo(Offset.Zero)
                                    isSwipingAway = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reject",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Accept Button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(elevation = 18.dp, shape = CircleShape, spotColor = Color.White.copy(alpha = 0.4f))
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            if (!isSwipingAway && currentIndex < state.jobs.size) {
                                scope.launch {
                                    isSwipingAway = true
                                    offsetAnim.animateTo(Offset(1000f, 0f), tween(300))
                                    onEvent(SwipeDeckEvent.OnLike)
                                    offsetAnim.snapTo(Offset.Zero)
                                    isSwipingAway = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Like",
                        tint = MaterialTheme.colorScheme.primary, // #1B263B
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Rewind Button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .clickable {
                            if (!isSwipingAway && state.canUndo) {
                                scope.launch {
                                    isSwipingAway = true
                                    onEvent(SwipeDeckEvent.OnRewind)
                                    offsetAnim.snapTo(Offset(-1000f, 0f))
                                    offsetAnim.animateTo(Offset.Zero, tween(300))
                                    isSwipingAway = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rewind",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * The visual UI of an individual job card within the deck.
 * Separates the background shape from the text content for deferred loading transitions.
 *
 * @param job The job details to display.
 * @param contentAlpha The opacity of the text content inside the card.
 * @param modifier Modifier for styling the layout.
 */
@Composable
fun DeckCardItem(job: Job, contentAlpha: Float = 1f, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = Color.Black.copy(alpha = 0.6f)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF283344)) // Solid color to block the background cards completely
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp))
            .padding(24.dp)
    ) {
        if (contentAlpha > 0f) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = contentAlpha }
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Logo
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.secondary,
                                            Color.White
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = job.company.first().uppercaseChar().toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = job.company,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                text = "Verified · 2h ago",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = job.workMode.name.replace('_', ' '),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Identity
                Text(
                    text = "POSITION",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.White.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = job.title,
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "SALARY",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.White.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    val salary = job.salaryMin
                    Text(
                        text = if (salary != null) "₹${(salary / 100000).toInt()}L" else "Not disclosed",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "/mo",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Logistics
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${job.location} · ${job.workMode.name.replace('_', ' ')}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Tech Stack
                Text(
                    text = "TECH STACK",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    job.skills.forEach { tech ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tech,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun SwipeDeckScreenPreview() {
    JobMatcherTheme {
        SwipeDeckScreen(
            state = SwipeDeckState(jobs = previewJobs),
            onEvent = {}
        )
    }
}
