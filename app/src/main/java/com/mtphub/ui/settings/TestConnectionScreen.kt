package com.mtphub.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mtphub.models.ProxyStatus
import com.mtphub.service.LocalProxyState
import com.mtphub.ui.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.Socket

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestConnectionScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currentProxy by LocalProxyState.currentProxy.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var logs by remember { mutableStateOf(listOf<String>()) }
    var isTesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun addLog(msg: String) {
        logs = logs + "[${System.currentTimeMillis()}] $msg"
    }

    fun copyToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = logs.joinToString("\n")
        clipboard.setPrimaryClip(ClipData.newPlainText("Diagnostic Logs", text))
        Toast.makeText(context, "Logs copied", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { copyToClipboard() }, enabled = logs.isNotEmpty()) {
                        Icon(Icons.Default.ContentCopy, "Copy Logs")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        logs = emptyList()
                        isTesting = true
                        runFullDiagnostic(
                            proxy = currentProxy,
                            addLog = ::addLog
                        )
                        isTesting = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTesting && LocalProxyState.isRunning.value
            ) {
                Text(if (isTesting) "Running Diagnostics..." else "Start Full Diagnostic")
            }

            if (!LocalProxyState.isRunning.value) {
                Text(
                    text = "⚠️ Local proxy service is not running. Start it from Home screen first.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(logs) { log ->
                    Text(
                        log,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (log.contains("❌")) Color.Red else Color.Unspecified
                    )
                }
            }
        }
    }
}

private suspend fun runFullDiagnostic(proxy: com.mtphub.models.ProxyEntity?, addLog: (String) -> Unit) {
    withContext(Dispatchers.IO) {
        addLog("Starting full diagnostic...")

        if (proxy == null) {
            addLog("❌ No active proxy. Please start the proxy service and ensure at least one working proxy is available.")
            return@withContext
        }

        addLog("✅ Proxy loaded: ${proxy.server}:${proxy.port} (status: ${proxy.status})")

        if (proxy.status != ProxyStatus.WORKING) {
            addLog("⚠️ Proxy status is not WORKING (${proxy.status}). Test may fail.")
        }

        // Test TCP connection
        addLog("Testing TCP connection to ${proxy.server}:${proxy.port}...")
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(proxy.server, proxy.port), 5000)
            addLog("✅ TCP Connection established successfully.")

            // Test simple data send/receive (optional)
            addLog("Sending test packet...")
            socket.outputStream.write(ByteArray(64) { 0 })
            addLog("✅ Data sent successfully.")

            socket.close()
            addLog("✅ Connection closed cleanly.")
        } catch (e: Exception) {
            addLog("❌ Connection failed: ${e.message}")
            Timber.e(e, "Diagnostic failed")
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }
}