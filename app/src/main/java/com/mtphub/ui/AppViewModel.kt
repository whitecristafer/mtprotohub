package com.mtphub.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mtphub.MTProtoHub
import com.mtphub.data.AppDatabase
import com.mtphub.data.AppSettings
import com.mtphub.data.SettingsRepository
import com.mtphub.models.LogEntity
import com.mtphub.models.ProxyEntity
import com.mtphub.proxy.ProxyManager
import com.mtphub.service.LocalProxyState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AppViewModel(
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val proxyManager = ProxyManager(database, settingsRepository)

    val proxies: StateFlow<List<ProxyEntity>> = database.proxyDao().getAllProxies()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        
    val topWorkingProxies: StateFlow<List<ProxyEntity>> = database.proxyDao().getTopWorkingProxies(1)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val settings: StateFlow<AppSettings> = settingsRepository.settings
    
    val activeConnections: StateFlow<Int> = LocalProxyState.activeConnections
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)
        
    val connectedClients: StateFlow<List<com.mtphub.service.ClientConnection>> = LocalProxyState.connectedClients
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        
    val logs: StateFlow<List<LogEntity>> = database.logDao().getLogs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun refreshList(force: Boolean = false) {
        proxyManager.refreshProxies(force)
    }

    fun updateSettings(newSettings: AppSettings) {
        settingsRepository.updateSettings(newSettings)
    }
}

class AppViewModelFactory(private val application: MTProtoHub) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(application.database, application.settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
