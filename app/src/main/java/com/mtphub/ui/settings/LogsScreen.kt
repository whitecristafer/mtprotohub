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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mtphub.ui.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Application Logs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val allLogs = logs.joinToString("\n") { 
                            "[${sdf.format(Date(it.timestamp))}] ${it.tag}: ${it.message}"
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("MTProto Hub Logs", allLogs)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied ${logs.size} logs.", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Copy all")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(logs) { log ->
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    val time = sdf.format(Date(log.timestamp))
                    Text(
                        text = "[$time] ${log.tag}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = log.message,
                        style = MaterialTheme.typography.bodySmall
                    )
                    HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
