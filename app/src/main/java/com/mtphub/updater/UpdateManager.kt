package com.mtphub.updater

import android.content.Context
import com.mtphub.data.VersionControl
import com.mtphub.service.UpdateService
import java.io.File

class UpdateManager(private val context: Context) {
    suspend fun checkForUpdates(
        onNewVersionFound: (latestVersion: String, downloadUrl: String, changelog: String) -> Unit,
        onNoUpdate: () -> Unit = {}
    ) {
        if (!UpdateService.UpdatePreferences.isAutoUpdateEnabled(context)) {
            onNoUpdate()
            return
        }

        val currentVersion = VersionControl.getCurrentVersion(context)
        val latestRelease = VersionControl.fetchLatestRelease() ?: run {
            onNoUpdate()
            return
        }

        val latestVersion = latestRelease.tag_name
        val asset = VersionControl.selectApkAsset(latestRelease)

        if (asset != null && isRemoteNewer(latestVersion, currentVersion)) {
            onNewVersionFound(
                latestVersion,
                asset.browser_download_url,
                latestRelease.body.orEmpty()
            )
        } else {
            onNoUpdate()
        }
    }

    fun startDownload(downloadUrl: String) {
        val destDir = File(context.getExternalFilesDir(null), VersionControl.DOWNLOAD_DIR)
        if (!destDir.exists()) destDir.mkdirs()

        val destFile = File(destDir, "mtprotohub_update_${System.currentTimeMillis()}.apk")
        UpdateService.startDownload(context, downloadUrl, destFile)
    }

    private fun isRemoteNewer(latest: String, current: String): Boolean {
        val newVersion = normalizeVersion(latest)
        val currentVersion = normalizeVersion(current)

        val newParts = newVersion.split('.').map { it.toIntOrNull() ?: 0 }
        val currentParts = currentVersion.split('.').map { it.toIntOrNull() ?: 0 }
        val maxSize = maxOf(newParts.size, currentParts.size)

        for (index in 0 until maxSize) {
            val a = newParts.getOrNull(index) ?: 0
            val b = currentParts.getOrNull(index) ?: 0
            if (a != b) return a > b
        }
        return false
    }

    private fun normalizeVersion(value: String): String {
        return value.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore(' ')
            .substringBefore('-')
            .ifBlank { "0.0.0" }
    }
}
