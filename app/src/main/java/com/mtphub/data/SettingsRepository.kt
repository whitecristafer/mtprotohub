package com.mtphub.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val updateIntervalHours: Int = 12,
    val poolSize: Int = 20,
    val parallelChecks: Int = 50,
    val maxPingMs: Int = 1000,
    val sourceUrl: String = "https://raw.githubusercontent.com/SoliSpirit/mtproto/master/all_proxies.txt",
    val autoSwitchProxies: Boolean = true,
    val selectedProxyUrl: String? = null,
    val autoScanEnabled: Boolean = true,
    val localProxyPort: Int = 1080,
    val language: String = "system",
    val lastRepoUpdate: Long = 0
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mtproto_settings", Context.MODE_PRIVATE)
    
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            updateIntervalHours = prefs.getInt("updateIntervalHours", 12),
            poolSize = prefs.getInt("poolSize", 20),
            parallelChecks = prefs.getInt("parallelChecks", 50),
            maxPingMs = prefs.getInt("maxPingMs", 1000),
            sourceUrl = prefs.getString("sourceUrl", "https://raw.githubusercontent.com/SoliSpirit/mtproto/master/all_proxies.txt") ?: "",
            autoSwitchProxies = prefs.getBoolean("autoSwitchProxies", true),
            selectedProxyUrl = prefs.getString("selectedProxyUrl", null),
            autoScanEnabled = prefs.getBoolean("autoScanEnabled", true),
            localProxyPort = prefs.getInt("localProxyPort", 1080),
            language = prefs.getString("language", "system") ?: "system",
            lastRepoUpdate = prefs.getLong("lastRepoUpdate", 0)
        )
    }

    fun updateSettings(newSettings: AppSettings) {
        prefs.edit().apply {
            putInt("updateIntervalHours", newSettings.updateIntervalHours)
            putInt("poolSize", newSettings.poolSize)
            putInt("parallelChecks", newSettings.parallelChecks)
            putInt("maxPingMs", newSettings.maxPingMs)
            putString("sourceUrl", newSettings.sourceUrl)
            putBoolean("autoSwitchProxies", newSettings.autoSwitchProxies)
            putString("selectedProxyUrl", newSettings.selectedProxyUrl)
            putBoolean("autoScanEnabled", newSettings.autoScanEnabled)
            putInt("localProxyPort", newSettings.localProxyPort)
            putString("language", newSettings.language)
            putLong("lastRepoUpdate", newSettings.lastRepoUpdate)
            apply()
        }
        _settings.value = newSettings
    }
}
