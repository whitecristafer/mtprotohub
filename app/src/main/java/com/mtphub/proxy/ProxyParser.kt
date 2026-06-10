package com.mtphub.proxy

import android.net.Uri
import com.mtphub.models.ProxyEntity
import timber.log.Timber

object ProxyParser {

    fun parseProxies(data: String, sourceUrl: String): List<ProxyEntity> {
        val lines = data.split("\n", "\r\n")
        val parsedList = mutableListOf<ProxyEntity>()
        
        for (line in lines) {
            val urlString = line.trim()
            if (urlString.isEmpty() || urlString.startsWith("#")) continue
            
            try {
                // Examples:
                // tg://proxy?server=1.2.3.4&port=443&secret=...
                // https://t.me/proxy?server=...
                
                val uri = Uri.parse(urlString)
                val server = uri.getQueryParameter("server")
                val portStr = uri.getQueryParameter("port")
                val secret = uri.getQueryParameter("secret")
                
                if (server != null && portStr != null && secret != null) {
                    val port = portStr.toIntOrNull()
                    if (port != null && port in 1..65535) {
                        parsedList.add(
                            ProxyEntity(
                                url = urlString,
                                server = server,
                                port = port,
                                secret = secret,
                                sourceUrl = sourceUrl
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.w("Failed to parse proxy line: $urlString. Error: ${e.message}")
            }
        }
        
        return parsedList
    }
}
