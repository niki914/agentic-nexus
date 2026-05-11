package com.niki914.nexus.agentic.mod

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import com.niki914.nexus.h.util.xTry
import com.niki914.nexus.h.util.xlog
import com.niki914.nexus.ipc.XConfig
import com.niki914.nexus.ipc.XValues
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
                XConfig.updateFromServerJson(context, remoteJson)
                xlog("WebSettings refreshed for $packageName ($versionCode)")
            } else {
                xlog("WebSettings refresh failed for $packageName ($versionCode): remote JSON is null")
            }
        }
    }

    fun getWebSettings(context: Context): WebSettings {
        rememberContext(context)
        return (xTry("XService.getWebSettings") {
            WebSettings(parseJsonObject(XConfig.get(context)))
        } ?: WebSettings()).also {
            xlog("WebSettings received: $it")
        }
    }

    fun getLocalSettings(context: Context): LocalSettings {
        rememberContext(context)
        return if (shouldUseProviderForLocalSettings(context)) {
            LocalSettings(parseJsonObject(XConfig.getLocalSettings(context)))
        } else {
            readLocalSettingsDirect(context)
        }
    }

    fun putLocalSettings(context: Context, settings: LocalSettings) {
        rememberContext(context)
        if (shouldUseProviderForLocalSettings(context)) {
            XConfig.putLocalSettings(context, settings.props.toString())
            xlog("LocalSettings updated via provider: ${settings.props}")
        } else {
            writeLocalSettingsDirect(context, settings)
        }
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
        XConfig.postNotification(context, title, content, uri)
    }

    private fun rememberContext(context: Context) {
        cachedContext = context.applicationContext as Application
    }

    private fun shouldUseProviderForLocalSettings(context: Context): Boolean {
        return context.packageName in XValues.appList
    }

    private fun readLocalSettingsDirect(context: Context): LocalSettings {
        return LocalSettings(
            parseJsonObject(
                context
                    .getSharedPreferences(LOCAL_SETTINGS_PREFS, Context.MODE_PRIVATE)
                    .getString(LOCAL_SETTINGS_KEY, null)
            )
        ).also {
            xlog("LocalSettings received via direct prefs: $it")
        }
    }

    private fun writeLocalSettingsDirect(context: Context, settings: LocalSettings) {
        context
            .getSharedPreferences(LOCAL_SETTINGS_PREFS, Context.MODE_PRIVATE)
            .edit {
                putString(LOCAL_SETTINGS_KEY, settings.props.toString())
            }
        xlog("LocalSettings updated via direct prefs: ${settings.props}")
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
        val alias = if (packageName == "com.heytap.speechassist") "breeno" else packageName
        return "http://$host/$alias/$versionCode/config.json"
    }
}

private val httpClient = OkHttpClient()