package com.mtphub.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mtphub.ui.AppViewModel
import com.mtphub.service.LocalProxyState
import com.mtphub.data.TelegramApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigateToProxies: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToTest: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    val proxies by viewModel.proxies.collectAsStateWithLifecycle()
    val topWorking by viewModel.topWorkingProxies.collectAsStateWithLifecycle()
    val activeConnections by viewModel.activeConnections.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val isGatewayRunning by LocalProxyState.isRunning.collectAsStateWithLifecycle()
    val isPaused by LocalProxyState.isPaused.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MTProto Hub") },
                actions = {
                    IconButton(onClick = onNavigateToLogs) {
                        Icon(Icons.Default.DateRange, contentDescription = "Logs")
                    }
                    IconButton(onClick = onNavigateToProxies) {
                        Icon(Icons.Default.List, contentDescription = "Proxies")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = onNavigateToHelp) {
                        Icon(Icons.Default.Info, contentDescription = "Help")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val sdf = java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.getDefault())
            val lastUpdateStr = if (settings.lastRepoUpdate > 0) sdf.format(java.util.Date(settings.lastRepoUpdate)) else "Never"
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Last Update: $lastUpdateStr", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { viewModel.refreshList(force = true) }) {
                    Text("Fetch Now")
                }
            }
            
            // Status Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Gateway Status", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (isPaused) "Paused" else if (isGatewayRunning) "Working" else "Stopped",
                            color = if (isPaused) MaterialTheme.colorScheme.tertiary else if (isGatewayRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Connections: $activeConnections")
                        OutlinedButton(onClick = onNavigateToClients) {
                            Text("View Clients")
                        }
                    }
                }
            }

            // Connection Info
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Local Proxy Connection", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("IP: 127.0.0.1")
                            Text("Port: ${settings.localProxyPort}")
                        }
                        OutlinedButton(onClick = onNavigateToTest) {
                            Text("Test")
                        }
                    }
                }
            }

            // Current Proxy Info
            val current = topWorking.firstOrNull()
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Active MTProto Proxy", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (current != null) {
                        Text("IP: ${current.server}")
                        Text("Port: ${current.port}")
                        Text("Latency: ${if (current.latency >= 0) "${current.latency} ms" else "N/A"}")
                        Text("Score: ${current.score}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            val localUrl = "tg://proxy?server=127.0.0.1&port=${settings.localProxyPort}&secret=${current.secret}"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("MTProto", localUrl)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied local gateway link", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Export Local Link")
                        }
                    } else {
                        Text("No working proxies available.")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            val telegramInstalled = TelegramApp.isTelegramInstalled(context);
            if (current != null && telegramInstalled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            val localUrl = "tg://proxy?server=127.0.0.1&port=${settings.localProxyPort}&secret=${current.secret}"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(localUrl))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Не удалось открыть Telegram", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Connect proxy in Telegram", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                }
            } else {
                // NONE
            }
            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = {
                        if (isGatewayRunning) {
                            onStopService()
                        } else {
                            onStartService()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isGatewayRunning) "Stop" else "Start")
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = { viewModel.refreshList(force = false) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Check Pings")
                }
            }
        }
    }
}
