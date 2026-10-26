package com.spectreflow.app.settings

import android.content.Context
import android.content.SharedPreferences

enum class CaptureDuration(val seconds: Int, val isProOnly: Boolean) {
    SEC_3(3, false),
    SEC_5(5, false),
    SEC_9(9, false),
    SEC_15(15, true),
    SEC_30(30, true);

    companion object {
        fun fromSeconds(sec: Int): CaptureDuration =
            entries.find { it.seconds == sec } ?: SEC_3
    }
}

enum class OutputResolution {
    BALANCED, // 1080p
    HIGH      // Full sensor
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    VIETNAMESE("vi", "Tiếng Việt")
}

enum class AppIconTheme(val id: String, val displayName: String) {
    OBSIDIAN_EMERALD("emerald", "Obsidian Emerald (Default)"),
    CYBER_MINT("mint", "Cyber Mint (Pro)"),
    DEEP_GOLD("gold", "Deep Gold (Pro)")
}

data class AppSettingsState(
    val duration: CaptureDuration = CaptureDuration.SEC_3,
    val showGrid: Boolean = true,
    val hapticEnabled: Boolean = true,
    val saveCompanionVideo: Boolean = true,
    val resolution: OutputResolution = OutputResolution.HIGH,
    val isProUnlocked: Boolean = false,
    val selectedAppIcon: AppIconTheme = AppIconTheme.OBSIDIAN_EMERALD,
    val hasSeenOnboarding: Boolean = false,
    val language: AppLanguage = AppLanguage.ENGLISH
)

class AppSettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("spectreflow_prefs", Context.MODE_PRIVATE)

    fun loadSettings(): AppSettingsState {
        val durationSec = prefs.getInt("duration_sec", 3)
        val showGrid = prefs.getBoolean("show_grid", true)
        val haptics = prefs.getBoolean("haptics_enabled", true)
        val companionVideo = prefs.getBoolean("save_companion_video", true)
        val resName = prefs.getString("resolution", OutputResolution.HIGH.name) ?: OutputResolution.HIGH.name
        val isPro = prefs.getBoolean("is_pro", false)
        val iconId = prefs.getString("app_icon", AppIconTheme.OBSIDIAN_EMERALD.id) ?: AppIconTheme.OBSIDIAN_EMERALD.id
        val seenOnboarding = prefs.getBoolean("has_seen_onboarding", false)
        val langCode = prefs.getString("language", AppLanguage.ENGLISH.code) ?: AppLanguage.ENGLISH.code

        return AppSettingsState(
            duration = CaptureDuration.fromSeconds(durationSec),
            showGrid = showGrid,
            hapticEnabled = haptics,
            saveCompanionVideo = companionVideo,
            resolution = runCatching { OutputResolution.valueOf(resName) }.getOrDefault(OutputResolution.HIGH),
            isProUnlocked = isPro,
            selectedAppIcon = AppIconTheme.entries.find { it.id == iconId } ?: AppIconTheme.OBSIDIAN_EMERALD,
            hasSeenOnboarding = seenOnboarding,
            language = if (langCode == "vi") AppLanguage.VIETNAMESE else AppLanguage.ENGLISH
        )
    }

    fun saveSettings(settings: AppSettingsState) {
        prefs.edit()
            .putInt("duration_sec", settings.duration.seconds)
            .putBoolean("show_grid", settings.showGrid)
            .putBoolean("haptics_enabled", settings.hapticEnabled)
            .putBoolean("save_companion_video", settings.saveCompanionVideo)
            .putString("resolution", settings.resolution.name)
            .putBoolean("is_pro", settings.isProUnlocked)
            .putString("app_icon", settings.selectedAppIcon.id)
            .putBoolean("has_seen_onboarding", settings.hasSeenOnboarding)
            .putString("language", settings.language.code)
            .apply()
    }
}
