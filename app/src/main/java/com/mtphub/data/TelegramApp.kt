package com.mtphub.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import timber.log.Timber


class TelegramApp {
    companion object {
        fun isTelegramInstalled(context: Context): Boolean {
            return try {
                context.packageManager.getPackageInfo("org.telegram.messenger", 0)
                Timber.d("isTelegramInstalled = true")
                true
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.d("isTelegramInstalled = false")
                false
            }
        }
    }
}