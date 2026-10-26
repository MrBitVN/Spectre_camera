package com.spectreflow.app.billing

import com.spectreflow.app.settings.AppSettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Clean interface for in-app billing.
 * Ready for drop-in Google Play Billing Library (v6+) integration.
 */
interface BillingService {
    val isProUnlocked: StateFlow<Boolean>
    suspend fun purchasePro(): Result<Boolean>
    suspend fun restorePurchases(): Result<Boolean>
    fun setProUnlocked(unlocked: Boolean)
}

/**
 * Stub implementation of BillingService for testing and demonstration.
 */
class StubBillingService(private val settingsManager: AppSettingsManager) : BillingService {
    private val _isProUnlocked = MutableStateFlow(settingsManager.loadSettings().isProUnlocked)
    override val isProUnlocked: StateFlow<Boolean> = _isProUnlocked.asStateFlow()

    override suspend fun purchasePro(): Result<Boolean> {
        setProUnlocked(true)
        return Result.success(true)
    }

    override suspend fun restorePurchases(): Result<Boolean> {
        val current = settingsManager.loadSettings().isProUnlocked
        _isProUnlocked.value = current
        return Result.success(current)
    }

    override fun setProUnlocked(unlocked: Boolean) {
        _isProUnlocked.value = unlocked
        val currentSettings = settingsManager.loadSettings()
        settingsManager.saveSettings(currentSettings.copy(isProUnlocked = unlocked))
    }
}
