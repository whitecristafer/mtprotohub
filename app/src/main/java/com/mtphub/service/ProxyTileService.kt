package com.mtphub.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ProxyTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var listeningJob: Job? = null
    override fun onStartListening() {
        super.onStartListening()

        listeningJob = serviceScope.launch {
            LocalProxyState.isRunning.collect { isRunning ->
                updateTileState(isRunning)
            }
        }
    }

    override fun onStopListening() {
        listeningJob?.cancel()
        super.onStopListening()
    }

    private fun updateTileState(isRunning: Boolean) {
        val tile = qsTile ?: return

        if (isRunning) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "MTPHub: Running"
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "MTPHub: Stopped"
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        val intent = Intent(this, LocalProxyService::class.java)

        if (tile.state == Tile.STATE_INACTIVE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            intent.action = "STOP"
            startService(intent)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}