package com.mtphub.ui.proxies

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mtphub.models.ProxyEntity
import com.mtphub.models.ProxyStatus
import com.mtphub.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxiesScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val proxies by viewModel.proxies.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf("All") }
    val context = LocalContext.current

    val filteredList = proxies.filter {
        when (filter) {
            "Working" -> it.status == ProxyStatus.WORKING
            "Best" -> it.status == ProxyStatus.WORKING && it.score > 70
            "Failed" -> it.status == ProxyStatus.FAILED
            else -> true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Proxy List (${filteredList.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = listOf("All", "Working", "Best", "Failed").indexOf(filter),
                edgePadding = 16.dp
            ) {
                listOf("All", "Working", "Best", "Failed").forEach { tab ->
                    Tab(
                        selected = filter == tab,
                        onClick = { filter = tab },
                        text = { Text(tab) }
                    )
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredList) { proxy ->
                    ProxyItem(
                        proxy = proxy,
                        isSelected = proxy.url == settings.selectedProxyUrl,
                        localPort = settings.localProxyPort,
                        onSelect = {
                            viewModel.updateSettings(settings.copy(selectedProxyUrl = proxy.url, autoSwitchProxies = false))
                            Toast.makeText(context, "Selected proxy manually. Auto-switch disabled.", Toast.LENGTH_SHORT).show()
                        },
                        onExportOriginal = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("MTProto", proxy.url)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied original proxy link", Toast.LENGTH_SHORT).show()
                        },
                        onExportLocal = {
                            val localUrl = "tg://proxy?server=127.0.0.1&port=${settings.localProxyPort}&secret=${proxy.secret}"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("MTProto", localUrl)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied local link", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProxyItem(
    proxy: ProxyEntity,
    isSelected: Boolean,
    localPort: Int,
    onSelect: () -> Unit,
    onExportOriginal: () -> Unit,
    onExportLocal: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "IP: ${proxy.server}:${proxy.port}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Ping: ${if(proxy.latency >= 0) "${proxy.latency}ms" else "-"}")
                Text(text = "Score: ${proxy.score}")
                Text(text = proxy.status.name, color = if(proxy.status == ProxyStatus.WORKING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onSelect) {
                    Text("Select")
                }
                TextButton(onClick = onExportOriginal) {
                    Text("Export Remote")
                }
                TextButton(onClick = onExportLocal) {
                    Text("Export Local")
                }
            }
        }
    }
}
