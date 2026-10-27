package com.spectreflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectreflow.app.R
import com.spectreflow.app.settings.CaptureDuration
import com.spectreflow.app.ui.theme.AccentEmerald
import com.spectreflow.app.ui.theme.BgCard
import com.spectreflow.app.ui.theme.BorderSubtle
import com.spectreflow.app.ui.theme.TextPrimary
import com.spectreflow.app.ui.theme.TextSecondary

@Composable
fun DurationSelector(
    selectedDuration: CaptureDuration,
    isProUnlocked: Boolean,
    onDurationSelected: (CaptureDuration) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x9914171A))
            .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CaptureDuration.entries.forEach { duration ->
            val isSelected = duration == selectedDuration
            val isLocked = duration.isProOnly && !isProUnlocked

                    val durationDesc = if (isLocked)
                        stringResource(R.string.cd_duration_pro, duration.seconds)
                    else
                        stringResource(R.string.cd_duration, duration.seconds)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                when {
                                    isSelected -> AccentEmerald
                                    else -> Color.Transparent
                                }
                            )
                            .clickable {
                                onDurationSelected(duration)
                            }
                            .semantics {
                                role = Role.RadioButton
                                contentDescription = durationDesc
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${duration.seconds}s",
                                color = when {
                                    isSelected -> Color(0xFF0C0E10)
                                    isLocked -> TextSecondary.copy(alpha = 0.5f)
                                    else -> TextPrimary
                                },
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )

                            if (isLocked) {
                                Text(
                                    text = stringResource(R.string.pro_badge),
                                    color = Color(0xFFFFAB00),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x33FFAB00))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}
