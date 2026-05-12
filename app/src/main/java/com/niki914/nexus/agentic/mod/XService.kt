package com.niki914.nexus.agentic.mod

import android.content.Context
import com.niki914.nexus.h.util.ContextProvider
import com.niki914.nexus.h.util.xTry
import com.niki914.nexus.h.util.xlog
import com.niki914.nexus.ipc.XIpcBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object XService {

    suspend fun refreshWebSettings(context: Context, packageName: String, versionCode: Long) {
        withContext(Dispatchers.IO) {
            val remoteJson = fetchWebSettingsSync(packageName, versionCode)
            if (remoteJson != null) {
                writeWebSettingsJson(context, remoteJson)
                xlog("WebSettings refreshed for $packageName ($versionCode)")
            } else {
                xlog("WebSettings refresh failed for $packageName ($versionCode): remote JSON is null")
            }
        }
    }

    fun getWebSettings(context: Context): WebSettings {
        return (xTry("XService.getWebSettings") {
            readWebSettings(context)
        } ?: WebSettings()).also {
            xlog("WebSettings received: $it")
        }
    }

    fun getLocalSettings(context: Context): LocalSettings {
        return readLocalSettings(context).also {
            xlog("LocalSettings received: $it")
        }
    }

    fun putLocalSettings(context: Context, settings: LocalSettings) {
        writeLocalSettings(context, settings)
        xlog("LocalSettings updated: ${settings.props}")
    }

    suspend fun postNotification(
        title: String,
        content: String,
        uri: String?
    ) {
        val context = ContextProvider.await()
        XIpcBridge.postNotification(context, title, content, uri)
    }

    private fun readWebSettings(context: Context): WebSettings {
        return WebSettings(parseJsonObject(XIpcBridge.readWebSettingsJson(context)))
    }

    private fun writeWebSettingsJson(context: Context, json: String) {
        XIpcBridge.writeWebSettingsJson(context, json)
    }

    private fun readLocalSettings(context: Context): LocalSettings {
        return LocalSettings(parseJsonObject(XIpcBridge.readLocalSettingsJson(context)))
    }

    private fun writeLocalSettings(context: Context, settings: LocalSettings) {
        XIpcBridge.writeLocalSettingsJson(context, settings.props.toString())
    }
}

private fun fetchWebSettingsSync(packageName: String, versionCode: Long): String? {
    val url = buildWebSettingsUrl(packageName, versionCode)
    val request = Request.Builder()
        .url(url)
        .build()
    return xTry("fetchWebSettingsSync") {
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful || response.body == null) {
                xlog("fetchWebSettingsSync failed for $url: code=${response.code}")
                return@xTry null
            }
            response.body?.string()
        }
    }
}

private fun buildWebSettingsUrl(packageName: String, versionCode: Long): String {
    val host = "127.0.0.1:8788"
    return "http://$host/$packageName/$versionCode/config.json"
}

private val httpClient = OkHttpClient()
