package com.niki914.nexus.ipc

import android.content.Context
import android.os.Bundle
import android.net.Uri
import androidx.core.net.toUri

enum class HostApp(val packageName: String) {
    Breeno("com.heytap.speechassist"),
    XiaoAi("com.miui.voiceassist");

    companion object {
        fun fromPackageName(packageName: String?): HostApp? {
            return entries.firstOrNull { it.packageName == packageName }
        }

        val packageNames: List<String>
            get() = entries.map(HostApp::packageName)
    }
}

object XValues {

    val myPackageName = "com.niki914.nexus.agentic"
    val appList: List<String>
        get() = HostApp.packageNames

    enum class AppType { Me, Host, Unknown }

    fun getAppTypeOf(context: Context): AppType {
        if (context.packageName in appList) return AppType.Host
        if (context.packageName == myPackageName) return AppType.Me
        return AppType.Unknown
    }
}

object IpcContract {
    const val AUTHORITY = "com.niki914.nexus.ipc.provider"
    val CONTENT_URI: Uri = "content://$AUTHORITY".toUri()

    enum class Method(val wireName: String) {
        GET_CONFIG("get_config"),
        GET_WEB_SETTINGS("get_web_settings"),
        PUT_WEB_SETTINGS("put_web_settings"),
        MUTATE_WEB_SETTINGS("mutate_web_settings"),
        GET_LOCAL_SETTINGS("get_local_settings"),
        PUT_LOCAL_SETTINGS("put_local_settings"),
        MUTATE_LOCAL_SETTINGS("mutate_local_settings"),
        POST_NOTIFICATION("post_notification");

        companion object {
            private val byWireName = entries.associateBy(Method::wireName)

            fun fromWire(wireName: String?): Method? {
                return wireName?.let { byWireName[it] }
            }
        }
    }

    enum class Field(val wireName: String) {
        CONFIG_JSON("config_json"),
        WEB_SETTINGS_JSON("web_settings_json"),
        LOCAL_SETTINGS_JSON("local_settings_json"),
        PATH("path"),
        VALUE_JSON("value_json"),
        TITLE("title"),
        CONTENT("content"),
        URI("uri"),
        SUCCESS("success")
    }

    enum class Store(
        val readMethod: Method,
        val writeMethod: Method,
        val mutateMethod: Method,
        val payloadField: Field,
        val legacyPayloadField: Field? = null
    ) {
        WEB_SETTINGS(
            readMethod = Method.GET_WEB_SETTINGS,
            writeMethod = Method.PUT_WEB_SETTINGS,
            mutateMethod = Method.MUTATE_WEB_SETTINGS,
            payloadField = Field.WEB_SETTINGS_JSON,
            legacyPayloadField = Field.CONFIG_JSON
        ),
        LOCAL_SETTINGS(
            readMethod = Method.GET_LOCAL_SETTINGS,
            writeMethod = Method.PUT_LOCAL_SETTINGS,
            mutateMethod = Method.MUTATE_LOCAL_SETTINGS,
            payloadField = Field.LOCAL_SETTINGS_JSON
        )
    }
}

internal fun Bundle.readString(field: IpcContract.Field): String? {
    return getString(field.wireName)
}

internal fun Bundle.readBoolean(field: IpcContract.Field): Boolean {
    return getBoolean(field.wireName)
}

internal fun Bundle.putValue(field: IpcContract.Field, value: Any?) {
    when (value) {
        null -> putString(field.wireName, null)
        is String -> putString(field.wireName, value)
        is Boolean -> putBoolean(field.wireName, value)
        else -> error("Unsupported bundle type for field=${field.name}")
    }
}

internal fun ipcBundleOf(vararg pairs: Pair<IpcContract.Field, Any?>): Bundle {
    return Bundle().apply {
        pairs.forEach { (field, value) ->
            putValue(field, value)
        }
    }
}