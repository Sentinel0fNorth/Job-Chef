package com.jobchef.feature.tailor.presentation.tailor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jobchef.core.designsystem.theme.JobMatcherTheme

data class TailorUIState(
    val role: String = "SDE Intern",
    val company: String = "Nimbus Labs",
    val matchScore: Int = 94,
    val candidateName: String = "Aarav Mehta",
    val candidateEmail: String = "aarav.mehta@email.com",
    val candidatePhone: String = "+91 98xxx",
    val candidateGithub: String = "github.com/aaravm"
)

/**
 * The Tailoring Lab screen where candidates can view their AI-tailored resume and cover letter.
 *
 * @param state The current UI state containing candidate and job details.
 * @param modifier Modifier for styling the layout.
 */
@Composable
fun TailorScreen(
    state: TailorUIState = TailorUIState(),
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(TailorTab.RESUME) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header strip
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 12.dp, start = 24.dp, end = 24.dp)
            ) {
                Text(
                    text = "05 — TAILORING LAB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${state.role} · ${state.company}",
                        style = MaterialTheme.typography.headlineSmall, // text-2xl font-semibold
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${state.matchScore}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Black.copy(alpha = 0.05f)))

            // Top half — JD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "SOURCE · JOB DESCRIPTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                val highlightBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                val highlightText = MaterialTheme.colorScheme.primary
                
                val jdText = buildAnnotatedString {
                    append("We are seeking an ")
                    withStyle(SpanStyle(background = highlightBg, color = highlightText)) { append(" SDE Intern ") }
                    append(" with strong fundamentals in ")
                    withStyle(SpanStyle(background = highlightBg, color = highlightText)) { append(" Java and Spring Boot ") }
                    append(". You will design RESTful microservices, optimize ")
                    withStyle(SpanStyle(background = highlightBg, color = highlightText)) { append(" Postgres queries ") }
                    append(", and ship to AWS. Ideal candidates have built side projects with measurable impact and care about clean architecture.")
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, Color.Black.copy(alpha = 0.05f), MaterialTheme.shapes.medium)
                        .padding(16.dp)
                ) {
                    Text(
                        text = jdText,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Black.copy(alpha = 0.05f)))

            // Bottom half — AI output with tabs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(bottom = 120.dp) // padding for floating button
            ) {
                // Tabs row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(50))
                            .padding(4.dp)
                    ) {
                        TabButton(
                            text = "Resume",
                            isSelected = selectedTab == TailorTab.RESUME,
                            onClick = { selectedTab = TailorTab.RESUME }
                        )
                        TabButton(
                            text = "Cover Letter",
                            isSelected = selectedTab == TailorTab.COVER_LETTER,
                            onClick = { selectedTab = TailorTab.COVER_LETTER }
                        )
                    }
                    
                    Text(
                        text = "REGENERATE",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AGENT · TAILORED ${if (selectedTab == TailorTab.RESUME) "RESUME" else "COVER LETTER"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Content Preview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 8.dp, shape = MaterialTheme.shapes.medium, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), MaterialTheme.shapes.medium)
                        .padding(16.dp)
                ) {
                    if (selectedTab == TailorTab.RESUME) {
                        ResumePreviewContent(state)
                    } else {
                        CoverPreviewContent(state)
                    }
                }
            }
        }

        // Floating Export CTA
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .padding(bottom = 56.dp) // bottom nav padding
        ) {
            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(50), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(50)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EXPORT",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp), // label-caps tracking-[0.18em]
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Reusable tab button for switching between Resume and Cover Letter views.
 *
 * @param text The tab label.
 * @param isSelected Whether this tab is currently active.
 * @param onClick Callback triggered when the tab is clicked.
 */
@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Displays the tailored resume preview content.
 *
 * @param state The current UI state containing candidate details.
 */
@Composable
fun ResumePreviewContent(state: TailorUIState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.candidateName,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 16.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${state.candidateEmail} · ${state.candidatePhone} · ${state.candidateGithub}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp).height(1.dp).background(Color.Black.copy(alpha = 0.1f)))
        }

        val highlightBg = Color.Black.copy(alpha = 0.08f)
        val highlightText = MaterialTheme.colorScheme.onSurface

        // Education
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Text("EDUCATION", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            Text("B.Tech, Computer Science · IIT Bombay", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text("2022 – 2026 · CGPA 9.1", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Experience
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Text("EXPERIENCE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            Text("Backend Intern · Fintrail", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            
            val exp1 = buildAnnotatedString {
                append("• Designed 6 ")
                withStyle(SpanStyle(background = highlightBg, color = highlightText)) { append(" RESTful microservices ") }
                append(" in ")
                withStyle(SpanStyle(background = highlightBg, color = highlightText)) { append(" Java ") }
                append(" + ")
                withStyle(SpanStyle(background = highlightBg, color = highlightText)) { append(" Spring Boot ") }
                append(", serving 120k req/day at p95 of 38ms.")
            }
            Text(exp1, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 4.dp))
            
            val exp2 = buildAnnotatedString {
                append("• Tuned ")
                withStyle(SpanStyle(background = highlightBg, color = highlightText)) { append(" Postgres ") }
                append(" indexes & query plans, cutting analytics latency by 62%.")
            }
            Text(exp2, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 4.dp))
        }

        // Skills
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("SKILLS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            val skills = buildAnnotatedString {
                withStyle(SpanStyle(background = highlightBg, color = highlightText)) { append(" Java ") }
                append(", ")
                withStyle(SpanStyle(background = highlightBg, color = highlightText)) { append(" Spring Boot ") }
                append(", ")
                withStyle(SpanStyle(background = highlightBg, color = highlightText)) { append(" Postgres ") }
                append(", ")
                withStyle(SpanStyle(background = highlightBg, color = highlightText)) { append(" AWS ") }
                append(", Docker, Kafka, TypeScript.")
            }
            Text(skills, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp), color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/**
 * Displays the tailored cover letter preview content.
 *
 * @param state The current UI state containing candidate details.
 */
@Composable
fun CoverPreviewContent(state: TailorUIState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = state.candidateName,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 15.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "To: Hiring Team, ${state.company} · 01 May 2026",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp).height(1.dp).background(Color.Black.copy(alpha = 0.1f)))
        }

        val highlightBg = Color.Black.copy(alpha = 0.08f)
        val highlightText = MaterialTheme.colorScheme.onSurface

        Text("Dear ${state.company} team,", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 12.dp))
        
        val p1 = buildAnnotatedString {
            append("I'm applying for the ")
            withStyle(SpanStyle(background = highlightBg, color = highlightText)) { append(" ${state.role} ") }
            append(" role. Your team's bet on ")
            withStyle(SpanStyle(background = highlightBg, color = highlightText)) { append(" clean architecture ") }
            append(" and small, observable services matches how I've built every side project — measurable wins over surface area.")
        }
        Text(p1, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 12.dp))
        
        Text("Warmly,\n${state.candidateName.split(" ").first()}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp), color = MaterialTheme.colorScheme.onSurface)
    }
}

enum class TailorTab {
    RESUME, COVER_LETTER
}

@Preview(showBackground = true)
@Composable
fun TailorScreenPreview() {
    JobMatcherTheme {
        TailorScreen()
    }
}
