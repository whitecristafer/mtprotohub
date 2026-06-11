package com.mtphub

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mtphub.service.LocalProxyService
import com.mtphub.service.UpdateService
import com.mtphub.ui.AppNavigation
import com.mtphub.ui.AppViewModel
import com.mtphub.ui.AppViewModelFactory
import com.mtphub.ui.theme.MTProtoHubTheme
import java.io.File
import androidx.core.content.edit
import com.mtphub.service.ApkInstaller

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels { AppViewModelFactory(application as MTProtoHub) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.refreshList()

        setContent {
            MTProtoHubTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        viewModel = viewModel,
                        onStartService = {
                            val intent = Intent(this, LocalProxyService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(intent)
                            } else {
                                startService(intent)
                            }
                        },
                        onStopService = {
                            val intent = Intent(this, LocalProxyService::class.java).apply {
                                action = "STOP"
                            }
                            startService(intent)
                        }
                    )
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()

        val prefs = getSharedPreferences("update_prefs", MODE_PRIVATE)
        val path = prefs.getString("pending_apk_path", null) ?: return

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || packageManager.canRequestPackageInstalls()) {
            prefs.edit().remove("pending_apk_path").apply()
            val file = File(path)
            if (file.exists()) {
                ApkInstaller.openInstaller(this, file)
            }
        }
    }
}
