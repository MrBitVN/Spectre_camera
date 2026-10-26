package com.spectreflow.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectreflow.app.R
import com.spectreflow.app.ui.theme.AccentEmerald
import com.spectreflow.app.ui.theme.AccentMint
import com.spectreflow.app.ui.theme.BgCard
import com.spectreflow.app.ui.theme.BgObsidian
import com.spectreflow.app.ui.theme.TextPrimary
import com.spectreflow.app.ui.theme.TextSecondary

data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val details: List<String>
)

@Composable
fun OnboardingScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableIntStateOf(0) }

    val pages = listOf(
        OnboardingPageData(
            title = stringResource(R.string.onboarding_step1_title),
            subtitle = stringResource(R.string.onboarding_step1_subtitle),
            icon = Icons.Default.TouchApp,
            details = listOf(
                stringResource(R.string.onboarding_step1_bullet1),
                stringResource(R.string.onboarding_step1_bullet2),
                stringResource(R.string.onboarding_step1_bullet3)
            )
        ),
        OnboardingPageData(
            title = stringResource(R.string.onboarding_step2_title),
            subtitle = stringResource(R.string.onboarding_step2_subtitle),
            icon = Icons.Default.Vibration,
            details = listOf(
                stringResource(R.string.onboarding_step2_bullet1),
                stringResource(R.string.onboarding_step2_bullet2),
                stringResource(R.string.onboarding_step2_bullet3)
            )
        ),
        OnboardingPageData(
            title = stringResource(R.string.onboarding_step3_title),
            subtitle = stringResource(R.string.onboarding_step3_subtitle),
            icon = Icons.Default.AutoAwesome,
            details = listOf(
                stringResource(R.string.onboarding_step3_bullet1),
                stringResource(R.string.onboarding_step3_bullet2),
                stringResource(R.string.onboarding_step3_bullet3)
            )
        )
    )

    val currentData = pages[currentPage]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgObsidian)
            .padding(24.dp)
    ) {
        // Skip Button top right
        if (currentPage < pages.size - 1) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 20.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_skip),
                    color = TextSecondary,
                    fontSize = 15.sp
                )
            }
        }

        // Main Page Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(BgCard)
            ) {
                Icon(
                    imageVector = currentData.icon,
                    contentDescription = null,
                    tint = AccentEmerald,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = currentData.title,
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = currentData.subtitle,
                color = AccentMint,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                currentData.details.forEach { bullet ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AccentEmerald)
                        )
                        Text(
                            text = bullet,
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // Bottom Navigation: Indicator Dots + Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.indices.forEach { index ->
                    val isSelected = index == currentPage
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (isSelected) 24.dp else 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isSelected) AccentEmerald else Color(0x4494A3B8))
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (currentPage < pages.size - 1) {
                        currentPage++
                    } else {
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
            ) {
                Text(
                    text = if (currentPage == pages.size - 1)
                        stringResource(R.string.onboarding_get_started)
                    else
                        stringResource(R.string.action_next),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
