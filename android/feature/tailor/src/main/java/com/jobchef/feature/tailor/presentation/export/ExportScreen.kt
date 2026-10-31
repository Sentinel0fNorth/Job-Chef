package com.jobchef.feature.tailor.presentation.export

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jobchef.core.designsystem.components.ScreenHeader
import com.jobchef.core.designsystem.theme.JobMatcherTheme

/**
 * Displays the final ATS-compliant PDF preview and export actions.
 *
 * @param modifier Modifier for styling the layout.
 */
@Composable
fun ExportScreen(
    jobId: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 200.dp) // padding for floating panel
        ) {
            // Screen Header
            ScreenHeader(
                eyebrow = "06 — EXPORT",
                title = "Final preview",
                subtitle = "LaTeX-rendered. ATS-clean. One tap from sent.",
                modifier = Modifier.padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
            )

            // Document preview (LaTeX style)
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1 / 1.35f) // A4 approx ratio
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    LatexDocumentPreview()
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "resume_nimbus_sde_v3.pdf · 84KB",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Floating action panel
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .padding(bottom = 56.dp) // bottom nav padding
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 20.dp, shape = RoundedCornerShape(24.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resume", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cover letter", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Go to application", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(imageVector = Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/**
 * Renders a visual preview of the LaTeX-generated resume using standard Android typography elements.
 */
@Composable
fun LatexDocumentPreview() {
    Column(modifier = Modifier.fillMaxSize()) {
        // Name and Contact Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ARJUN MEHTA",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "arjun.m@email.com · +91 98xx xxxx · Jaipur, IN",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp, fontFamily = FontFamily.Serif),
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp).height(1.dp).background(Color.Black.copy(alpha = 0.2f)))
        }

        val latexBodyStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp, fontFamily = FontFamily.Serif, color = Color.Black)

        // Experience
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Text(
                text = "EXPERIENCE",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                letterSpacing = 0.1.sp
            )
            Box(modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp).height(0.5.dp).background(Color.Black.copy(alpha = 0.3f)))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Backend Engineer · Sigma", style = latexBodyStyle, fontWeight = FontWeight.Bold)
                Text("2023 — now", style = latexBodyStyle)
            }
            
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text("· Designed 6 microservices in Java + Spring Boot, 120k req/day @ p95 38ms.", style = latexBodyStyle.copy(fontSize = 7.5.sp), modifier = Modifier.padding(bottom = 2.dp))
                Text("· Tuned Postgres indexes; cut analytics latency by 62%.", style = latexBodyStyle.copy(fontSize = 7.5.sp), modifier = Modifier.padding(bottom = 2.dp))
                Text("· Shipped CI/CD to AWS ECS, release time 40m → 6m.", style = latexBodyStyle.copy(fontSize = 7.5.sp))
            }
        }

        // Skills
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Text(
                text = "SKILLS",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                letterSpacing = 0.1.sp
            )
            Box(modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp).height(0.5.dp).background(Color.Black.copy(alpha = 0.3f)))
            
            Text("Java · Spring Boot · Postgres · AWS · Kafka · Docker · Kotlin", style = latexBodyStyle.copy(fontSize = 7.5.sp))
        }

        // Education
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Text(
                text = "EDUCATION",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                letterSpacing = 0.1.sp
            )
            Box(modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp).height(0.5.dp).background(Color.Black.copy(alpha = 0.3f)))
            
            Text("B.Tech, Computer Science · MNIT Jaipur · 2020-24", style = latexBodyStyle.copy(fontSize = 7.5.sp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExportScreenPreview() {
    JobMatcherTheme {
        ExportScreen(jobId = "preview")
    }
}
