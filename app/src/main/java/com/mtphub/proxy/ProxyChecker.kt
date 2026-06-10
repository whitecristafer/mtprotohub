package com.mtphub.proxy

import com.mtphub.models.ProxyEntity
import com.mtphub.models.ProxyStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.system.measureTimeMillis

object ProxyChecker {

    /**
     * Checks if a proxy is alive by attempting a TCP connection.
     * Evaluates latency and success rate.
     */
    suspend fun checkProxy(proxy: ProxyEntity, timeoutMs: Int = 3000): ProxyEntity = withContext(Dispatchers.IO) {
        var successCount = 0
        var totalLatency = 0L
        val maxChecks = 3
        
        for (i in 1..maxChecks) {
            val latency = measureTcpConnection(proxy.server, proxy.port, timeoutMs)
            if (latency != null) {
                successCount++
                totalLatency += latency
            }
        }
        
        val isWorking = successCount > 0
        val avgLatency = if (successCount > 0) (totalLatency / successCount).toInt() else -1
        
        // Calculate scores
        val currentTotalChecks = proxy.totalChecks + 1
        val currentFailedChecks = proxy.failedChecks + if (isWorking) 0 else 1
        
        val successRate = if (currentTotalChecks > 0) {
            ((currentTotalChecks - currentFailedChecks) * 100 / currentTotalChecks)
        } else {
            0
        }
        
        val score = calculateScore(avgLatency, successRate, isWorking)
        val status = if (currentFailedChecks > 10) ProxyStatus.BLACKLISTED else if (isWorking) ProxyStatus.WORKING else ProxyStatus.FAILED
        
        return@withContext proxy.copy(
            lastCheck = System.currentTimeMillis(),
            latency = avgLatency,
            successRate = successRate,
            totalChecks = currentTotalChecks,
            failedChecks = currentFailedChecks,
            status = status,
            score = score
        )
    }

    private suspend fun measureTcpConnection(host: String, port: Int, timeoutMs: Int): Long? {
        return withTimeoutOrNull(timeoutMs.toLong()) {
            try {
                var connectionTime = 0L
                val time = measureTimeMillis {
                    val socket = Socket()
                    // Configure socket
                    socket.tcpNoDelay = true
                    socket.soTimeout = timeoutMs
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                    socket.close()
                }
                time
            } catch (e: Exception) {
                null
            }
        }
    }
    
    private fun calculateScore(latency: Int, successRate: Int, isWorking: Boolean): Int {
        if (!isWorking) return 0
        
        // Formula: 40% latency, 30% success rate, 20% uptime, 10% speed
        // Simplification for MVP:
        // Latency Score: <100ms = 100, 1000ms = 0
        val latencyScore = maxOf(0, 100 - (latency / 10))
        val uptimeScore = 100 // Hardcoded for simplicity unless historical data is precise
        val speedScore = 100 // TCP connect doesn't measure bandwidth, assume 100
        
        return (latencyScore * 0.4 + successRate * 0.3 + uptimeScore * 0.2 + speedScore * 0.1).toInt()
    }
}
