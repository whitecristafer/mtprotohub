package com.mtphub.service

import com.mtphub.models.ProxyEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class ClientConnection(
    val id: String = UUID.randomUUID().toString(),
    val connectedAt: Long = System.currentTimeMillis(),
    val clientIp: String
)

object LocalProxyState {
    val isRunning = MutableStateFlow(false)
    val isPaused = MutableStateFlow(false)
    val activeConnections = MutableStateFlow(0)
    private val _currentProxy = MutableStateFlow<ProxyEntity?>(null)
    val currentProxy: StateFlow<ProxyEntity?> = _currentProxy
    
    val connectedClients = MutableStateFlow<List<ClientConnection>>(emptyList())

    fun addClient(ip: String): ClientConnection {
        val client = ClientConnection(clientIp = ip)
        connectedClients.update { it + client }
        activeConnections.update { it + 1 }
        return client
    }

    fun removeClient(client: ClientConnection) {
        connectedClients.update { it.filter { c -> c.id != client.id } }
        activeConnections.update { maxOf(0, it - 1) }
    }


    internal fun setCurrentProxy(proxy: ProxyEntity?) { _currentProxy.value = proxy }
    internal fun setRunning(value: Boolean) { isRunning.value = value }
}
