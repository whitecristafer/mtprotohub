package com.mtphub.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.mtphub.data.VersionControl
import com.mtphub.updater.UpdateProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import androidx.core.net.toUri
import androidx.core.content.edit



class UpdateService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var notificationManager: NotificationManager
    private var currentProgress = 0

    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "update_channel"

        fun startDownload(context: Context, downloadUrl: String, destFile: File) {
            val intent = Intent(context, UpdateService::class.java).apply {
                putExtra(EXTRA_URL, downloadUrl)
                putExtra(EXTRA_FILE, destFile.absolutePath)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        private const val EXTRA_URL = "url"
        private const val EXTRA_FILE = "file"
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Update Service",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
        val destPath = intent.getStringExtra(EXTRA_FILE) ?: return START_NOT_STICKY
        val destFile = File(destPath)

        startForeground(NOTIFICATION_ID, createNotification(0))

        serviceScope.launch {
            val success = VersionControl.downloadFile(url, destFile) { progress ->
                val percent = (progress * 100).toInt().coerceIn(0, 100)
                if (percent > currentProgress) {
                    currentProgress = percent
                    updateNotification(percent)
                    UpdateProgress.setProgress(percent)
                }
            }

            if (success) {
                UpdateProgress.setProgress(100)
                installApk(destFile)
            } else {
                showErrorNotification()
                UpdateProgress.reset()
            }

            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun createNotification(progress: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading update")
            .setContentText("Progress: $progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(progress: Int) {
        notificationManager.notify(NOTIFICATION_ID, createNotification(progress))
    }



    private fun savePendingApkPath(apkFile: File) {
        getSharedPreferences("update_prefs", MODE_PRIVATE)
            .edit {
                putString("pending_apk_path", apkFile.absolutePath)
            }
    }

    private fun installApk(apkFile: File) {
        if (!apkFile.exists()) {
            Toast.makeText(this, "APK file not found.", Toast.LENGTH_LONG).show()
            return
        }

        savePendingApkPath(apkFile)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !packageManager.canRequestPackageInstalls()
        ) {
            val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:$packageName".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(settingsIntent)
            return
        }

        openInstaller(apkFile)
    }

    private fun openInstaller(apkFile: File) {
        val apkUri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = apkUri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
        }

        startActivity(installIntent)
    }

    private fun showErrorNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Update failed")
            .setContentText("Could not download the update.")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .build()
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    object UpdatePreferences {
        private const val PREF_NAME = "update_prefs"
        private const val KEY_AUTO_UPDATE = "auto_update"

        fun isAutoUpdateEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_AUTO_UPDATE, true)
        }

        fun setAutoUpdateEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_AUTO_UPDATE, enabled).apply()
        }
    }
}
