package com.mtphub

import android.app.Application
import com.mtphub.data.AppDatabase
import com.mtphub.data.SettingsRepository
import timber.log.Timber

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.mtphub.models.LogEntity

class DbLogTree(private val logDao: com.mtphub.data.LogDao) : Timber.Tree() {
    private val scope = CoroutineScope(Dispatchers.IO)
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        scope.launch {
            logDao.insertLog(LogEntity(level = priority, tag = tag ?: "App", message = message))
        }
    }
}

class MTProtoHub : Application() {
    
    lateinit var database: AppDatabase
        private set
        
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        
        database = AppDatabase.getDatabase(this)
        settingsRepository = SettingsRepository(this)
        
        Timber.plant(Timber.DebugTree())
        Timber.plant(DbLogTree(database.logDao()))
    }
}
