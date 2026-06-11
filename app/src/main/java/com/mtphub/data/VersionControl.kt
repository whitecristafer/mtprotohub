package com.mtphub.data

import android.content.Context
import android.content.pm.PackageManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

data class GitHubRelease(
    val tag_name: String,
    val name: String? = null,
    val body: String? = null,
    val assets: List<Asset> = emptyList(),
    val prerelease: Boolean = false
)

data class Asset(
    val name: String,
    val browser_download_url: String,
    val size: Long = 0L
)

class VersionControl {
    companion object {
        private const val GITHUB_API_RELEASES =
            "https://api.github.com/repos/whitecristafer/mtprotohub/releases/latest"
        const val DOWNLOAD_DIR = "MTProtoHubUpdates"

        private val client = OkHttpClient.Builder().build()
        private val gson = Gson()

        fun getCurrentVersion(context: Context): String {
            return try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.versionName?.trim().orEmpty().ifBlank { "0.0.0" }
            } catch (_: PackageManager.NameNotFoundException) {
                "0.0.0"
            }
        }

        suspend fun isUpdateAvailable(context: Context): Boolean {
            val currentVersion = getCurrentVersion(context)
            val latestRelease = fetchLatestRelease() ?: return false
            val latestVersion = normalizeVersion(latestRelease.tag_name)
            return compareVersions(latestVersion, currentVersion) > 0
        }

        private fun normalizeVersion(value: String): String {
            return value.trim()
                .removePrefix("v")
                .removePrefix("V")
                .substringBefore(' ')
                .substringBefore('-')
                .ifBlank { "0.0.0" }
        }

        private fun compareVersions(left: String, right: String): Int {
            val leftParts = normalizeVersion(left).split('.').map { it.toIntOrNull() ?: 0 }
            val rightParts = normalizeVersion(right).split('.').map { it.toIntOrNull() ?: 0 }
            val maxSize = maxOf(leftParts.size, rightParts.size)

            for (index in 0 until maxSize) {
                val a = leftParts.getOrNull(index) ?: 0
                val b = rightParts.getOrNull(index) ?: 0
                if (a != b) return a.compareTo(b)
            }
            return 0
        }

        suspend fun fetchLatestRelease(): GitHubRelease? = withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(GITHUB_API_RELEASES)
                    .header("User-Agent", "MTProtoHub-App")
                    .header("Accept", "application/vnd.github+json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body?.string() ?: return@withContext null
                    gson.fromJson(body, GitHubRelease::class.java)
                }
            } catch (_: Exception) {
                null
            }
        }

        fun selectApkAsset(release: GitHubRelease): Asset? {
            val apkAssets = release.assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
            if (apkAssets.isEmpty()) return null

            val preferredNames = listOf("release", "universal", "main", "stable")
            preferredNames.forEach { keyword ->
                apkAssets.firstOrNull { it.name.contains(keyword, ignoreCase = true) }?.let { return it }
            }

            return apkAssets.first()
        }

        suspend fun downloadFile(url: String, destFile: File, onProgress: (Float) -> Unit): Boolean {
            return withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "MTProtoHub-App")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@withContext false

                        val responseBody = response.body ?: return@withContext false
                        val totalBytes = responseBody.contentLength().takeIf { it > 0 } ?: -1L

                        responseBody.byteStream().use { input ->
                            FileOutputStream(destFile).use { output ->
                                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                var totalRead = 0L
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read <= 0) break
                                    output.write(buffer, 0, read)
                                    totalRead += read
                                    if (totalBytes > 0) {
                                        onProgress(totalRead.toFloat() / totalBytes.toFloat())
                                    }
                                }
                                output.flush()
                            }
                        }
                    }

                    onProgress(1f)
                    true
                } catch (_: Exception) {
                    false
                }
            }
        }
    }
}
