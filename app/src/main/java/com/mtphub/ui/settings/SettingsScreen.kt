package com.mtphub.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mtphub.ui.AppViewModel
import com.mtphub.service.UpdateService.UpdatePreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var sourceUrl by remember(settings.sourceUrl) { mutableStateOf(settings.sourceUrl) }
    var maxPingMs by remember(settings.maxPingMs) { mutableStateOf(settings.maxPingMs.toString()) }
    var poolSize by remember(settings.poolSize) { mutableStateOf(settings.poolSize.toString()) }
    var parallelChecks by remember(settings.parallelChecks) { mutableStateOf(settings.parallelChecks.toString()) }
    var interval by remember(settings.updateIntervalHours) { mutableStateOf(settings.updateIntervalHours.toString()) }
    var localPort by remember(settings.localProxyPort) { mutableStateOf(settings.localProxyPort.toString()) }

    var autoSwitch by remember(settings.autoSwitchProxies) { mutableStateOf(settings.autoSwitchProxies) }
    var autoScan by remember(settings.autoScanEnabled) { mutableStateOf(settings.autoScanEnabled) }

    var autoUpdateEnabled by remember { mutableStateOf(UpdatePreferences.isAutoUpdateEnabled(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Column( modifier = Modifier
                .padding (horizontal = 16.dp, vertical = 8.dp)
            ) {
            Button(
                onClick = {
                    viewModel.updateSettings(
                        settings.copy(
                            sourceUrl = sourceUrl,
                            maxPingMs = maxPingMs.toIntOrNull() ?: 1000,
                            poolSize = poolSize.toIntOrNull() ?: 20,
                            parallelChecks = parallelChecks.toIntOrNull() ?: 50,
                            updateIntervalHours = interval.toIntOrNull() ?: 12,
                            localProxyPort = localPort.toIntOrNull() ?: 1080,
                            autoSwitchProxies = autoSwitch,
                            autoScanEnabled = autoScan
                        )
                    )
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Auto-switch Proxies")
                Switch(checked = autoSwitch, onCheckedChange = { autoSwitch = it })
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Auto-scan Proxies")
                Switch(checked = autoScan, onCheckedChange = { autoScan = it })
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Auto-check update")
                Switch(
                    checked = autoUpdateEnabled,
                    onCheckedChange = { isChecked ->
                        autoUpdateEnabled = isChecked
                        UpdatePreferences.setAutoUpdateEnabled(context, isChecked)
                    }
                )

            }

            OutlinedTextField(
                value = localPort,
                onValueChange = { localPort = it },
                label = { Text("Local Proxy Port") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = sourceUrl,
                onValueChange = { sourceUrl = it },
                label = { Text("Source URL") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = maxPingMs,
                onValueChange = { maxPingMs = it },
                label = { Text("Max Ping (ms)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = parallelChecks,
                onValueChange = { parallelChecks = it },
                label = { Text("Parallel Checks") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
