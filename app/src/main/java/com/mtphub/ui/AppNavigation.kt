package com.mtphub.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mtphub.ui.home.HomeScreen
import com.mtphub.ui.proxies.ProxiesScreen
import com.mtphub.ui.settings.SettingsScreen
import com.mtphub.ui.settings.HelpScreen
import com.mtphub.ui.settings.LogsScreen
import com.mtphub.ui.settings.ClientsScreen
import com.mtphub.ui.settings.TestConnectionScreen

@Composable
fun AppNavigation(
    viewModel: AppViewModel,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
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
