package com.mtphub.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
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
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Step 1: Start the Gateway", fontWeight = FontWeight.Bold)
                    Text("Go to the Home tab and click 'Start' under the Gateway Status. Ensure a proxy is active.")
                }
            }
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Step 2: Copy Local Link", fontWeight = FontWeight.Bold)
                    Text("Click the 'Export Local Link' button on the active proxy card. This copies the configuration for your local Telegram.")
                }
            }
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Step 3: Setup Telegram", fontWeight = FontWeight.Bold)
                    Text("Open Telegram and simply paste the link you copied into any chat (like 'Saved Messages') and tap on it. Telegram will prompt you to connect to the proxy.")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Manual Method", fontWeight = FontWeight.Bold)
                    Text("If the link doesn't work, configure it manually:\n" +
                            "1. Open Telegram Settings\n" +
                            "2. Go to 'Data and Storage'\n" +
                            "3. Scroll to the bottom and tap 'Proxy Settings'\n" +
                            "4. Add a new 'MTProto Proxy'\n" +
                            "5. Set Server IP: 127.0.0.1\n" +
                            "6. Set Port: 1080\n" +
                            "7. Paste the 'Secret' found in the copied link.")
                }
            }
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Will it switch automatically?", fontWeight = FontWeight.Bold)
                    Text("Yes. MTProto Hub automatically re-evaluates proxies in the background. If the current one dies, the gateway silently connects to the next healthy proxy without you needing to change the settings in Telegram.")
                }
            }
        }
    }
}
