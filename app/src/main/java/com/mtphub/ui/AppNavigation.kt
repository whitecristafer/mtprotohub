package com.mtphub.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mtphub.data.VersionControl
import com.mtphub.ui.components.DownloadProgressDialog
import com.mtphub.ui.components.UpdateDialog
import com.mtphub.ui.home.HomeScreen
import com.mtphub.ui.proxies.ProxiesScreen
import com.mtphub.ui.settings.ClientsScreen
import com.mtphub.ui.settings.HelpScreen
import com.mtphub.ui.settings.LogsScreen
import com.mtphub.ui.settings.SettingsScreen
import com.mtphub.ui.settings.TestConnectionScreen
import com.mtphub.updater.UpdateManager
import com.mtphub.updater.UpdateProgress

@Composable
fun AppNavigation(
    viewModel: AppViewModel,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val downloadProgress by UpdateProgress.progress.collectAsStateWithLifecycle()
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showUpdateOffer by remember { mutableStateOf(false) }
    var latestVersion by remember { mutableStateOf("") }
    var downloadUrl by remember { mutableStateOf("") }
    var changelog by remember { mutableStateOf("") }
    val context = LocalContext.current
    val updateManager = remember { UpdateManager(context) }

    LaunchedEffect(downloadProgress) {
        when {
            downloadProgress in 1..99 -> showDownloadDialog = true
            downloadProgress == 100 -> {
                showDownloadDialog = false
                Toast.makeText(context, "Update downloaded", Toast.LENGTH_SHORT).show()
                UpdateProgress.reset()
            }
            downloadProgress == 0 -> showDownloadDialog = false
        }
    }

    LaunchedEffect(Unit) {
        updateManager.checkForUpdates(
            onNewVersionFound = { version, url, body ->
                latestVersion = version
                downloadUrl = url
                changelog = body
                showUpdateOffer = true
            }
        )
    }

    if (showUpdateOffer) {
        UpdateDialog(
            currentVersion = VersionControl.getCurrentVersion(context),
            latestVersion = latestVersion,
            changelog = changelog,
            onDownload = {
                showUpdateOffer = false
                updateManager.startDownload(downloadUrl)
            },
            onDismiss = { showUpdateOffer = false }
        )
    }

    if (showDownloadDialog) {
        DownloadProgressDialog(
            progress = downloadProgress,
            onBackground = {
                showDownloadDialog = false
                Toast.makeText(context, "Download continues in background", Toast.LENGTH_SHORT).show()
            }
        )
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToProxies = { navController.navigate("proxies") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToHelp = { navController.navigate("help") },
                onNavigateToLogs = { navController.navigate("logs") },
                onNavigateToClients = { navController.navigate("clients") },
                onNavigateToTest = { navController.navigate("testConn") },
                onStartService = onStartService,
                onStopService = onStopService
            )
        }
        composable("proxies") {
            ProxiesScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("help") {
            HelpScreen(onBack = { navController.popBackStack() })
        }
        composable("logs") {
            LogsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("clients") {
            ClientsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("testConn") {
            TestConnectionScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}
