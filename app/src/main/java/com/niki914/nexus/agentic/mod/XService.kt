package com.niki914.nexus.agentic.mod

import android.app.Application
import android.content.Context
import com.niki914.nexus.h.util.xTry
import com.niki914.nexus.h.util.xlog
import com.niki914.nexus.ipc.XIpcBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object XService {

    @Volatile
    private var cachedContext: Application? = null

    suspend fun refreshWebSettings(context: Context, packageName: String, versionCode: Long) {
        rememberContext(context)
        withContext(Dispatchers.IO) {
            val remoteJson = fetchWebSettingsSync(packageName, versionCode)
            if (remoteJson != null) {
                XSettingsRepository.writeWebSettingsJson(context, remoteJson)
                xlog("WebSettings refreshed for $packageName ($versionCode)")
            } else {
                xlog("WebSettings refresh failed for $packageName ($versionCode): remote JSON is null")
            }
        }
    }

    fun getWebSettings(context: Context): WebSettings {
        rememberContext(context)
        return (xTry("XService.getWebSettings") {
            XSettingsRepository.readWebSettings(context)
        } ?: WebSettings()).also {
            xlog("WebSettings received: $it")
        }
    }

    fun getLocalSettings(context: Context): LocalSettings {
        rememberContext(context)
        return XSettingsRepository.readLocalSettings(context).also {
            xlog("LocalSettings received: $it")
        }
    }

    fun putLocalSettings(context: Context, settings: LocalSettings) {
        rememberContext(context)
        XSettingsRepository.writeLocalSettings(context, settings)
        xlog("LocalSettings updated: ${settings.props}")
    }

    fun postNotification(
        title: String,
        content: String,
        uri: String?
    ) {
        val context = cachedContext?.applicationContext
        if (context == null) {
            xlog("XService.postNotification skipped: no cached context")
            return
        }
        XIpcBridge.postNotification(context, title, content, uri)
    }

    private fun rememberContext(context: Context) {
        cachedContext = context.applicationContext as Application
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
}

private val httpClient = OkHttpClient()