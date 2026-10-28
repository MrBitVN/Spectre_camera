package com.spectreflow.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.spectreflow.app.R
import com.spectreflow.app.ui.theme.AccentEmerald
import com.spectreflow.app.ui.theme.BgCard
import com.spectreflow.app.ui.theme.BgObsidian
import com.spectreflow.app.ui.theme.TextPrimary
import com.spectreflow.app.ui.theme.TextSecondary

data class TutorialCardData(
    val id: Int,
    val part1: String,
    val boldPart: String,
    val part2: String = "",
    val gradient: Brush,
    val icon: ImageVector,
    val modeName: String,
    val steps: List<String>
)

/**
 * Tutorials Screen matching reference UI (Image 2):
 * - Top bar: back button on left, centered title "Tutorials".
 * - Intro text: "These guides show you the power of the AI-augmented camera:"
 * - 3 rounded dark-grey cards with vector thumbnail on left and bold title phrases on right.
 * - Tapping a card opens a modal detailing 3-4 steps.
 */
@Composable
fun TutorialsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTutorial by remember { mutableStateOf<TutorialCardData?>(null) }
    val scrollState = rememberScrollState()

    val tutorialCards = listOf(
        TutorialCardData(
            id = 1,
            part1 = stringResource(R.string.tutorial_1_title_part1),
            boldPart = stringResource(R.string.tutorial_1_title_bold),
            gradient = Brush.verticalGradient(listOf(Color(0xFFE65100), Color(0xFFBF360C), Color(0xFF3E2723))),
            icon = Icons.Default.AutoAwesome,
            modeName = stringResource(R.string.mode_vanish),
            steps = listOf(
                stringResource(R.string.tutorial_1_step1),
                stringResource(R.string.tutorial_1_step2),
                stringResource(R.string.tutorial_1_step3),
                stringResource(R.string.tutorial_1_step4)
            )
        ),
        TutorialCardData(
            id = 2,
            part1 = stringResource(R.string.tutorial_2_title_part1),
            boldPart = stringResource(R.string.tutorial_2_title_bold),
            gradient = Brush.verticalGradient(listOf(Color(0xFF0D47A1), Color(0xFF311B92), Color(0xFFFF6F00))),
            icon = Icons.Default.FlashOn,
            modeName = stringResource(R.string.mode_light_trails),
            steps = listOf(
                stringResource(R.string.tutorial_2_step1),
                stringResource(R.string.tutorial_2_step2),
                stringResource(R.string.tutorial_2_step3),
                stringResource(R.string.tutorial_2_step4)
            )
        ),
        TutorialCardData(
            id = 3,
            part1 = stringResource(R.string.tutorial_3_title_part1),
            boldPart = stringResource(R.string.tutorial_3_title_bold),
            part2 = stringResource(R.string.tutorial_3_title_part2),
            gradient = Brush.verticalGradient(listOf(Color(0xFF1B5E20), Color(0xFF004D40), Color(0xFF006064))),
            icon = Icons.Default.WaterDrop,
            modeName = stringResource(R.string.mode_motion_blur),
            steps = listOf(
                stringResource(R.string.tutorial_3_step1),
                stringResource(R.string.tutorial_3_step2),
                stringResource(R.string.tutorial_3_step3),
                stringResource(R.string.tutorial_3_step4)
            )
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgObsidian)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 44.dp, start = 20.dp, end = 20.dp)
                .verticalScroll(scrollState)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BgCard)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = stringResource(R.string.tutorials_title),
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(40.dp)) // balance layout
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Intro Subtitle
            Text(
                text = stringResource(R.string.tutorials_intro),
                color = TextSecondary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3 Rounded Dark Cards
            tutorialCards.forEach { card ->
                TutorialCard(
                    data = card,
                    onClick = { activeTutorial = card }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Detail Step Dialog
        activeTutorial?.let { tutorial ->
            TutorialDetailModal(
                tutorial = tutorial,
                onDismiss = { activeTutorial = null }
            )
        }
    }
}

@Composable
private fun TutorialCard(
    data: TutorialCardData,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, Color(0xFF263238), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail Artwork
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 68.dp, height = 94.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(data.gradient)
        ) {
            Icon(
                imageVector = data.icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title with bold key phrase
        val titleText = buildAnnotatedString {
            append(data.part1)
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                append(data.boldPart)
            }
            if (data.part2.isNotEmpty()) {
                append(data.part2)
            }
        }

        Text(
            text = titleText,
            color = Color(0xFFCFD8DC),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TutorialDetailModal(
    tutorial: TutorialCardData,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1A1E24),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF37474F)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tutorial.modeName,
                        color = AccentEmerald,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                tutorial.steps.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0x331ED98B))
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = AccentEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = step,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.action_start_shooting),
                        color = BgObsidian,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
