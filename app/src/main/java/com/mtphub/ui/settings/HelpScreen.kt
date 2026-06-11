package com.mtphub.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mtphub.data.VersionControl
import com.mtphub.updater.UpdateManager
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val versionName = VersionControl.getCurrentVersion(context)
    val updateManager = remember { UpdateManager(context) }
    var updateStatus by remember { mutableStateOf("checking") }
    var latestVersion by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val release = VersionControl.fetchLatestRelease()
            val asset = release?.let { VersionControl.selectApkAsset(it) }
            val isNewer = release != null && asset != null && isRemoteNewer(release.tag_name, versionName)

            updateStatus = if (isNewer) "update_available" else "latest"
            latestVersion = release?.tag_name.orEmpty()
        } catch (e: Exception) {
            Timber.e(e, "Help screen update check failed")
            updateStatus = "latest"
        }
    }

    val versionSuffix = when (updateStatus) {
        "update_available" -> "(update available)"
        "latest" -> "(latest)"
        else -> "(checking...)"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & Instructions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("How to connect Telegram to MTProto Hub", style = MaterialTheme.typography.titleLarge)
            Text("Version: $versionName $versionSuffix", style = MaterialTheme.typography.bodySmall)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Step 1: Start the gateway", fontWeight = FontWeight.Bold)
                    Text("Open Home and tap Start under Gateway Status.")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Step 2: Copy the local link", fontWeight = FontWeight.Bold)
                    Text("Use Export Local Link on the active proxy card.")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Step 3: Open Telegram", fontWeight = FontWeight.Bold)
                    Text("Paste the copied link into Telegram and open it.")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Manual setup", fontWeight = FontWeight.Bold)
                    Text("Open Telegram Settings, go to Data and Storage, then Proxy Settings. Add an MTProto proxy with server 127.0.0.1 and the local port from the app.")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Auto switch", fontWeight = FontWeight.Bold)
                    Text("The app re-evaluates proxies in the background and switches to a healthy one when needed.")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("About MTProto Hub", fontWeight = FontWeight.Bold)
                    Text("Current version: $versionName")
                    Text("Latest version: ${latestVersion.ifBlank { "Unknown" }}")
                    Text("Developer: Cristafer White")
                    Text("GitHub: whitecristafer/mtprotohub")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val gitUrl = "https://github.com/whitecristafer/mtprotohub"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(gitUrl))
                            runCatching { context.startActivity(intent) }
                        }
                    ) {
                        Text("Open GitHub")
                    }
                    if (updateStatus == "update_available") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    val release = VersionControl.fetchLatestRelease()
                                    val asset = release?.let { VersionControl.selectApkAsset(it) }
                                    if (asset != null) {
                                        updateManager.startDownload(asset.browser_download_url)
                                    } else {
                                        Timber.e("No APK asset found for update")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Update now")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Open GitHub to view project details",
                        fontStyle = FontStyle.Italic,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun isRemoteNewer(latest: String, current: String): Boolean {
    val left = normalizeVersion(latest).split('.').map { it.toIntOrNull() ?: 0 }
    val right = normalizeVersion(current).split('.').map { it.toIntOrNull() ?: 0 }
    val maxSize = maxOf(left.size, right.size)

    for (index in 0 until maxSize) {
        val a = left.getOrNull(index) ?: 0
        val b = right.getOrNull(index) ?: 0
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
