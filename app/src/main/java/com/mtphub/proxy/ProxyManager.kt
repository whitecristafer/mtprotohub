package com.mtphub.proxy

import com.mtphub.data.AppDatabase
import com.mtphub.data.NetworkClient
import com.mtphub.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

class ProxyManager(
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val mutex = Mutex()
    private var isRefreshing = false

    fun refreshProxies(force: Boolean = false) {
        scope.launch {
            val settings = settingsRepository.settings.first()
            if (!force && !settings.autoScanEnabled) {
                Timber.d("Auto-scan is disabled, skipping refresh.")
                return@launch
            }

            mutex.withLock {
                if (isRefreshing) return@launch
                isRefreshing = true
            }

            try {
                Timber.d("Starting proxy fetch from sources")
                val rawText = NetworkClient.fetchProxiesText(settings.sourceUrl)
                
                if (rawText != null) {
                    val parsedProxies = ProxyParser.parseProxies(rawText, settings.sourceUrl)
                    Timber.d("Parsed ${parsedProxies.size} proxies")
                    
                    settingsRepository.updateSettings(settings.copy(lastRepoUpdate = System.currentTimeMillis()))

                    
                    // Insert unfamiliar ones as UNCHECKED
                    parsedProxies.forEach { newProxy ->
                        val existing = database.proxyDao().getProxyByUrl(newProxy.url)
                        if (existing == null) {
                            database.proxyDao().insertProxies(listOf(newProxy))
                        }
                    }
                }
                
                // Now trigger a check on unchecked or old proxies
                checkProxies()
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing proxies")
            } finally {
                mutex.withLock { isRefreshing = false }
            }
        }
    }

    private suspend fun checkProxies() {
        val settings = settingsRepository.settings.first()
        val allProxies = database.proxyDao().getAllProxies().first()
        
        // Prioritize unchecked and those haven't been checked recently
        val toCheck = allProxies.sortedBy { it.lastCheck }.take(200) // check max 200 at a time
        
        Timber.d("Checking ${toCheck.size} proxies")
        
        // Chunk by parallel limit
        val chunks = toCheck.chunked(settings.parallelChecks)
        
        for (chunk in chunks) {
            val results = chunk.map { proxy ->
                scope.async {
                    ProxyChecker.checkProxy(proxy, settings.maxPingMs)
                }
            }.awaitAll()
            
            database.proxyDao().insertProxies(results)
        }
        
        Timber.d("Finished checking proxies")
    }
}
