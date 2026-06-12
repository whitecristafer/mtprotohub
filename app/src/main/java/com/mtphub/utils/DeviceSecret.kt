package com.mtphub.utils

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

object DeviceSecret {
    @SuppressLint("HardwareIds")
    fun get(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "fallback_id"
        val bytes = MessageDigest.getInstance("MD5").digest(androidId.toByteArray())
        // MTProto 32 symb
        return bytes.joinToString("") { "%02x".format(it) }
    }
}