package com.niki914.nexus.ipc

import android.net.Uri
import androidx.core.net.toUri

internal object XIpcContract {

    const val AUTHORITY = "com.niki914.nexus.ipc.provider"
    val CONTENT_URI: Uri = "content://$AUTHORITY".toUri()

    const val METHOD_GET_CONFIG = "get_config"
    const val METHOD_GET_WEB_SETTINGS = "get_web_settings"
    const val METHOD_PUT_WEB_SETTINGS = "put_web_settings"
    const val METHOD_GET_LOCAL_SETTINGS = "get_local_settings"
    const val METHOD_PUT_LOCAL_SETTINGS = "put_local_settings"
    const val METHOD_POST_NOTIFICATION = "post_notification"

    const val KEY_CONFIG_JSON = "config_json"
    const val KEY_WEB_SETTINGS_JSON = "web_settings_json"
    const val KEY_LOCAL_SETTINGS_JSON = "local_settings_json"
    const val KEY_TITLE = "title"
    const val KEY_CONTENT = "content"
    const val KEY_URI = "uri"
    const val KEY_SUCCESS = "success"
}