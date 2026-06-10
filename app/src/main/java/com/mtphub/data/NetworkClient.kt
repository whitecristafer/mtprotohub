package com.mtphub.data

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import timber.log.Timber

object NetworkClient {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        engine {
            requestTimeout = 10000
        }
    }

    suspend fun fetchProxiesText(url: String): String? {
        return try {
            val response: HttpResponse = client.get(url)
            response.bodyAsText()
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch proxies from $url")
            null
        }
    }
}
