package com.mtphub

import android.app.Application
import android.os.Build
import android.util.Log
import com.mtphub.data.AppDatabase
import com.mtphub.data.SettingsRepository
import com.mtphub.models.LogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class DbLogTree(private val logDao: com.mtphub.data.LogDao) : Timber.Tree() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val finalMessage = buildString {
            append(message)
            if (t != null) {
                append('\n')
                append(Log.getStackTraceString(t))
            }
        }

        scope.launch {
            logDao.insertLog(
                LogEntity(
                    level = priority,
                    tag = tag ?: "App",
                    message = finalMessage
                )
            )
        }
    }
}

class MTProtoHub : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    private var previousCrashHandler: Thread.UncaughtExceptionHandler? = null

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getDatabase(this)
        settingsRepository = SettingsRepository(this)

        Timber.plant(Timber.DebugTree())
        Timber.plant(DbLogTree(database.logDao()))

        installCrashHandler()
        logStartupInfo()
    }

    private fun installCrashHandler() {
        previousCrashHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runBlocking(Dispatchers.IO) {
                database.logDao().insertLog(
                    LogEntity(
                        level = Log.ERROR,
                        tag = "Crash",
                        message = buildString {
                            append("Thread: ")
                            append(thread.name)
                            append('\n')
                            append(Log.getStackTraceString(throwable))
                        }
                    )
                )
            }
            previousCrashHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun logStartupInfo() {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val appVersion = packageInfo.versionName ?: BuildConfig.VERSION_NAME
        val installer = packageManager.getInstallerPackageName(packageName) ?: "unknown"

        Timber.i(
            buildString {
                appendLine("App started")
                appendLine("App: $packageName")
                appendLine("Version: $appVersion")
                appendLine("Installer: $installer")
                appendLine("Device: ${Build.DEVICE}")
                appendLine("Model: ${Build.MODEL}")
                appendLine("Manufacturer: ${Build.MANUFACTURER}")
                appendLine("Brand: ${Build.BRAND}")
                appendLine("Product: ${Build.PRODUCT}")
                appendLine("Board: ${Build.BOARD}")
                appendLine("Hardware: ${Build.HARDWARE}")
                appendLine("Host: ${Build.HOST}")
                appendLine("User: ${Build.USER}")
                appendLine("Type: ${Build.TYPE}")
                appendLine("Tags: ${Build.TAGS}")
                appendLine("Fingerprint: ${Build.FINGERPRINT}")
                appendLine("Android: ${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})")
                appendLine("Security patch: ${Build.VERSION.SECURITY_PATCH}")
                appendLine("Base OS: ${Build.VERSION.BASE_OS}")
                appendLine("Codename: ${Build.VERSION.CODENAME}")
                appendLine("Incremental: ${Build.VERSION.INCREMENTAL}")
                appendLine("Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
                appendLine("32-bit ABIs: ${Build.SUPPORTED_32_BIT_ABIS.joinToString()}")
                appendLine("64-bit ABIs: ${Build.SUPPORTED_64_BIT_ABIS.joinToString()}")
            }
        )
    }
}
