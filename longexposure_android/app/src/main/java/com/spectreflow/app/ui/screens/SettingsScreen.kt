package com.spectreflow.app.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectreflow.app.R
import com.spectreflow.app.settings.AppIconTheme
import com.spectreflow.app.settings.AppSettingsState
import com.spectreflow.app.settings.OutputResolution
import com.spectreflow.app.ui.theme.AccentEmerald
import com.spectreflow.app.ui.theme.AccentMint
import com.spectreflow.app.ui.theme.BgCard
import com.spectreflow.app.ui.theme.BgObsidian
import com.spectreflow.app.ui.theme.BorderSubtle
import com.spectreflow.app.ui.theme.TextPrimary
import com.spectreflow.app.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    settings: AppSettingsState,
    onSettingsChanged: (AppSettingsState) -> Unit,
    onOpenOnboarding: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgObsidian)
            .padding(top = 40.dp, start = 20.dp, end = 20.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BgCard)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = TextPrimary)
            }
            Text(
                text = " " + stringResource(R.string.settings_title),
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 1. Tutorials Navigation Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BgCard)
                .clickable(onClick = onOpenOnboarding)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = stringResource(R.string.tutorials_title),
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        }

        HorizontalDivider(color = BorderSubtle, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

        // 2. Grid Overlay Setting
        SettingToggleRow(
            title = stringResource(R.string.setting_grid),
            isChecked = settings.showGrid,
            onCheckedChange = { onSettingsChanged(settings.copy(showGrid = it)) }
        )

        HorizontalDivider(color = BorderSubtle, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

        // 3. Haptic Feedback Setting
        SettingToggleRow(
            title = stringResource(R.string.setting_haptics),
            isChecked = settings.hapticEnabled,
            onCheckedChange = { onSettingsChanged(settings.copy(hapticEnabled = it)) }
        )

        HorizontalDivider(color = BorderSubtle, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

        // 4. Save Companion Video Setting
        SettingToggleRow(
            title = stringResource(R.string.setting_companion_video),
            subtitle = stringResource(R.string.setting_companion_video_desc),
            isChecked = settings.saveCompanionVideo,
            onCheckedChange = { onSettingsChanged(settings.copy(saveCompanionVideo = it)) }
        )

        HorizontalDivider(color = BorderSubtle, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

        // 5. Photo Quality / Output Resolution
        Text(
            text = stringResource(R.string.setting_resolution),
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OptionChip(
                title = stringResource(R.string.resolution_high),
                isSelected = settings.resolution == OutputResolution.HIGH,
                onClick = { onSettingsChanged(settings.copy(resolution = OutputResolution.HIGH)) }
            )
            OptionChip(
                title = stringResource(R.string.resolution_balanced),
                isSelected = settings.resolution == OutputResolution.BALANCED,
                onClick = { onSettingsChanged(settings.copy(resolution = OutputResolution.BALANCED)) }
            )
        }

        HorizontalDivider(color = BorderSubtle, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

        // 6. Custom App Icon Themes
        Text(
            text = stringResource(R.string.setting_app_icon),
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AppIconTheme.entries.forEach { iconTheme ->
                val isSelected = settings.selectedAppIcon == iconTheme
                val isLocked = iconTheme != AppIconTheme.OBSIDIAN_EMERALD && !settings.isProUnlocked
                OptionChip(
                    title = iconTheme.displayName,
                    isSelected = isSelected,
                    isLocked = isLocked,
                    onClick = {
                        if (!isLocked) {
                            onSettingsChanged(settings.copy(selectedAppIcon = iconTheme))
                        }
                    }
                )
            }
        }

        HorizontalDivider(color = BorderSubtle, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

        // 7. Unlock Pro (Stubbed)
        SettingToggleRow(
            title = stringResource(R.string.setting_pro_status),
            subtitle = stringResource(R.string.setting_pro_desc),
            isChecked = settings.isProUnlocked,
            onCheckedChange = { onSettingsChanged(settings.copy(isProUnlocked = it)) }
        )

        HorizontalDivider(color = BorderSubtle, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

        // 8. About SpectreFlow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BgCard)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(20.dp).padding(top = 2.dp))
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.setting_about) + " v1.0.0",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.setting_about_desc),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String? = null,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(text = subtitle, color = TextSecondary, fontSize = 12.sp)
            }
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BgObsidian,
                checkedTrackColor = AccentEmerald,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = BgCard
            )
        )
    }
}

@Composable
private fun OptionChip(
    title: String,
    isSelected: Boolean,
    isLocked: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) AccentEmerald.copy(alpha = 0.2f) else BgCard)
            .border(
                1.dp,
                if (isSelected) AccentEmerald else BorderSubtle,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = when {
                isSelected -> AccentEmerald
                isLocked -> TextSecondary.copy(alpha = 0.5f)
                else -> TextSecondary
            },
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
