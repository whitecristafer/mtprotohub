package com.mtphub.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "proxies")
data class ProxyEntity(
    @PrimaryKey
    val url: String, // tg://proxy?server=...
    val server: String,
    val port: Int,
    val secret: String,
    val sourceUrl: String,
    val lastSeen: Long = System.currentTimeMillis(),
    val lastCheck: Long = 0,
    val latency: Int = -1, // in ms
    val successRate: Int = 0, // 0-100
    val uptimeScore: Int = 0,
    val speedScore: Int = 0,
    val totalChecks: Int = 0,
    val failedChecks: Int = 0,
    val status: ProxyStatus = ProxyStatus.UNCHECKED,
    val score: Int = 0,
    val alias: String? = null // For user custom ones
): Serializable

enum class ProxyStatus {
    UNCHECKED,
    WORKING,
    FAILED,
    BLACKLISTED
}
