package com.mtphub.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import timber.log.Timber
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestConnectionScreen(viewModel: com.mtphub.ui.AppViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var logs by remember { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()
    var isTesting by remember { mutableStateOf(false) }

    fun addLog(msg: String) {
        logs = logs + msg
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local Proxy Test") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    if (isTesting) return@Button
                    isTesting = true
                    val portToTest = settings.localProxyPort
                    logs = listOf("Starting test against 127.0.0.1:$portToTest...")
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                val socket = Socket()
                                socket.connect(InetSocketAddress("127.0.0.1", portToTest), 3000)
                                addLog("✅ TCP Connection successful!")
                                
                                // Send dummy bytes
                                val out = socket.getOutputStream()
                                out.write("test_payload".toByteArray())
                                out.flush()
                                addLog("✅ Sent 12 bytes of test payload to proxy.")
                                
                                // Wait a bit to see if proxy drops us
                                kotlinx.coroutines.delay(1000)
                                if (socket.isClosed) {
                                    addLog("❌ Socket was closed by remote server.")
                                } else {
                                    addLog("✅ Connection remained open.")
                                }
                                socket.close()
                                addLog("Test completed, socket closed.")
                            }
                        } catch (e: Exception) {
                            addLog("❌ Failed: ${e.message}")
                            Timber.e(e, "Test Connection failed")
                        } finally {
                            isTesting = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTesting
            ) {
                Text(if (isTesting) "Testing..." else "Run Network Test")
            }

            Card(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Test Output", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    logs.forEach { log ->
                        Text(log)
                    }
                }
            }
        }
    }
}
