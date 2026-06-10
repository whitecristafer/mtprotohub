package com.mtphub

import android.content.Intent
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
import com.mtphub.ui.AppNavigation
import com.mtphub.ui.AppViewModel
import com.mtphub.ui.AppViewModelFactory
import com.mtphub.ui.theme.MTProtoHubTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels { AppViewModelFactory(application as MTProtoHub) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initial fetch on open
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
                            startService(intent)
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
}
