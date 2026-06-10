package com.mtphub.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.mtphub.MTProtoHub
import com.mtphub.MainActivity
import com.mtphub.models.ProxyEntity
import com.mtphub.models.ProxyStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class LocalProxyService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    private var currentProxy: ProxyEntity? = null
    
    private var wakeLock: PowerManager.WakeLock? = null
    private var consecutiveFailures = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        
        when (action) {
            "STOP" -> {
                Timber.i("Stopping LocalProxyService")
                stopRelay()
                stopSelf()
                return START_NOT_STICKY
            }
            "PAUSE" -> {
                Timber.i("Pausing LocalProxyService")
                LocalProxyState.isPaused.value = true
                try { serverSocket?.close() } catch(e: Exception){}
                updateNotification("Service Paused")
                return START_STICKY
            }
            "RESUME" -> {
                Timber.i("Resuming LocalProxyService")
                LocalProxyState.isPaused.value = false
                startRelay()
                return START_STICKY
            }
            else -> {
                if (LocalProxyState.isPaused.value) {
                    LocalProxyState.isPaused.value = false
                }
                acquireWakeLock()
                startForeground(1, buildNotification("Starting..."))
                startRelay()
                return START_STICKY
            }
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MTProtoHub::ProxyWakeLock")
            wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes, but generally handled by foreground service
        }
    }

    private fun startRelay() {
        if (LocalProxyState.isRunning.value && serverSocket != null && !serverSocket!!.isClosed) return
        
        serviceScope.launch {
            LocalProxyState.isRunning.value = true
            consecutiveFailures = 0
            
            val app = application as MTProtoHub
            val settings = app.settingsRepository.settings.first()
            val proxyList = app.database.proxyDao().getTopWorkingProxies(50).first()
            
            if (settings.selectedProxyUrl != null && !settings.autoSwitchProxies) {
               currentProxy = app.database.proxyDao().getProxyByUrl(settings.selectedProxyUrl)
            } else {
               // Avoid picking the immediately failed one again if there are others
               val workingProxies = proxyList.filter { it.status == ProxyStatus.WORKING }
               currentProxy = workingProxies.firstOrNull { it.url != currentProxy?.url } ?: workingProxies.firstOrNull()
            }
            
            if (currentProxy == null || currentProxy!!.status != ProxyStatus.WORKING) {
                Timber.w("No working proxy found to start relay")
                updateNotification("No working proxy ready. Check list.")
                LocalProxyState.isRunning.value = false
                return@launch
            }
            
            updateNotification("Connected to: ${currentProxy?.server} (${currentProxy?.latency}ms)")
            Timber.i("Starting relay for proxy ${currentProxy?.server}:${currentProxy?.port}")
            
            try {
                var portToUse = settings.localProxyPort
                try {
                    serverSocket = ServerSocket(portToUse)
                } catch (e: Exception) {
                    portToUse = if (portToUse == 1080) 1081 else 1080
                    serverSocket = ServerSocket(portToUse)
                }
                
                Timber.i("Local Proxy listening on 127.0.0.1:$portToUse")
                
                while (isActive && !LocalProxyState.isPaused.value) {
                    val clientSocket = serverSocket?.accept() ?: break
                    serviceScope.launch(Dispatchers.IO) {
                        val ip = clientSocket.inetAddress?.hostAddress ?: "Unknown"
                        val clientConn = LocalProxyState.addClient(ip)
                        handleClient(clientSocket, currentProxy!!)
                        LocalProxyState.removeClient(clientConn)
                    }
                }
            } catch (e: Exception) {
                if (!LocalProxyState.isPaused.value) {
                    Timber.e(e, "Relay Server error in accept loop")
                    updateNotification("Error: ${e.message}")
                }
            } finally {
                if (!LocalProxyState.isPaused.value) {
                    LocalProxyState.isRunning.value = false
                }
            }
        }
    }
    
    private suspend fun handleClient(clientSocket: Socket, targetProxy: ProxyEntity) {
        var targetSocket: Socket? = null
        try {
            targetSocket = Socket()
            // Connect fast
            targetSocket.tcpNoDelay = true
            clientSocket.tcpNoDelay = true
            
            targetSocket.connect(InetSocketAddress(targetProxy.server, targetProxy.port), 6000)
            
            // Connection succeeded, reset failure count
            consecutiveFailures = 0
            
            val clientIn = clientSocket.getInputStream()
            val clientOut = clientSocket.getOutputStream()
            val targetIn = targetSocket.getInputStream()
            val targetOut = targetSocket.getOutputStream()
            
            coroutineScope {
                launch(Dispatchers.IO) {
                    try {
                        val buffer = ByteArray(65536)
                        var bytesRead: Int
                        while (clientIn.read(buffer).also { bytesRead = it } != -1) {
                            targetOut.write(buffer, 0, bytesRead)
                            targetOut.flush()
                        }
                    } catch (e: Exception) {
                        // Ignored, client closed
                    } finally {
                        try { targetSocket?.close() } catch (e: Exception) {}
                        try { clientSocket.close() } catch (e: Exception) {}
                    }
                }
                
                launch(Dispatchers.IO) {
                    try {
                        val buffer = ByteArray(65536)
                        var bytesRead: Int
                        while (targetIn.read(buffer).also { bytesRead = it } != -1) {
                            clientOut.write(buffer, 0, bytesRead)
                            clientOut.flush()
                        }
                    } catch (e: Exception) {
                        // Ignored, target closed
                    } finally {
                        try { targetSocket?.close() } catch (e: Exception) {}
                        try { clientSocket.close() } catch (e: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            if (e !is kotlinx.coroutines.CancellationException) {
                Timber.w("Failed to proxy connection: ${e.message}")
                onProxyFailure()
            }
        } finally {
            withContext(Dispatchers.IO) {
                try { targetSocket?.close() } catch(e: Exception){}
                try { clientSocket.close() } catch(e: Exception){}
            }
        }
    }

    private fun onProxyFailure() {
        consecutiveFailures++
        if (consecutiveFailures >= 3) {
            Timber.w("Proxy failed $consecutiveFailures times, forcing switch/restart...")
            consecutiveFailures = 0
            
            // Trigger restart to pick a new proxy
            serviceScope.launch {
                try { serverSocket?.close() } catch(e: Exception){}
                LocalProxyState.isRunning.value = false
                
                // Only switch if auto-switch is enabled
                val app = application as MTProtoHub
                val settings = app.settingsRepository.settings.first()
                if (settings.autoSwitchProxies) {
                    // Slight delay to allow cleanup
                    delay(1000)
                    startRelay()
                } else {
                    updateNotification("Selected proxy is offline.")
                }
            }
        }
    }

    private fun stopRelay() {
        LocalProxyState.isRunning.value = false
        LocalProxyState.isPaused.value = false
        try { serverSocket?.close() } catch(e: Exception) {}
        serviceScope.cancel()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRelay()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "proxy_service",
                "Proxy Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs the local proxy gateway"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val stopIntent = Intent(this, LocalProxyService::class.java).apply { action = "STOP" }
        val stopPending = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val pauseIntentString = if (LocalProxyState.isPaused.value) "RESUME" else "PAUSE"
        val pauseIntent = Intent(this, LocalProxyService::class.java).apply { action = pauseIntentString }
        val pausePending = PendingIntent.getService(this, 2, pauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val openAppIntent = Intent(this, MainActivity::class.java).apply { 
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPending = PendingIntent.getActivity(this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "proxy_service")
            .setContentTitle("MTProto Hub")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppPending)
            .addAction(android.R.drawable.ic_media_pause, if (LocalProxyState.isPaused.value) "Resume" else "Pause", pausePending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, buildNotification(text))
    }
}
