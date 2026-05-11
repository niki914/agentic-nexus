package com.niki914.nexus.ipc

import android.content.Context
import android.os.Bundle

object XConfig {

    @Volatile
    private var cachedWebSettingsJson: String? = null

    fun updateFromServerJson(context: Context, jsonString: String) {
        putWebSettings(context, jsonString)
    }

    fun get(context: Context): String {
        return getWebSettings(context)
    }

    fun getWebSettings(context: Context): String {
        cachedWebSettingsJson?.let { return it }
        synchronized(this) {
            cachedWebSettingsJson?.let { return it }
            val bundle = callProvider(context, XIpcContract.METHOD_GET_WEB_SETTINGS)
            val json = bundle?.getString(XIpcContract.KEY_WEB_SETTINGS_JSON)
                ?: bundle?.getString(XIpcContract.KEY_CONFIG_JSON)
            val resolved = json ?: "{}"
            cachedWebSettingsJson = resolved
            return resolved
        }
    }

    fun putWebSettings(context: Context, jsonString: String) {
        callProvider(
            context = context,
            method = XIpcContract.METHOD_PUT_WEB_SETTINGS,
            extras = Bundle().apply {
                putString(XIpcContract.KEY_WEB_SETTINGS_JSON, jsonString)
            }
        )
        cachedWebSettingsJson = jsonString
    }

    fun getLocalSettings(context: Context): String? {
        val bundle = callProvider(context, XIpcContract.METHOD_GET_LOCAL_SETTINGS)
        return bundle?.getString(XIpcContract.KEY_LOCAL_SETTINGS_JSON)
    }

    fun putLocalSettings(context: Context, jsonString: String) {
        callProvider(
            context = context,
            method = XIpcContract.METHOD_PUT_LOCAL_SETTINGS,
            extras = Bundle().apply {
                putString(XIpcContract.KEY_LOCAL_SETTINGS_JSON, jsonString)
            }
        )
    }

    fun postNotification(
        context: Context,
        title: String,
        content: String,
        uri: String?
    ): Boolean {
        val bundle = callProvider(
            context = context,
            method = XIpcContract.METHOD_POST_NOTIFICATION,
            extras = Bundle().apply {
                putString(XIpcContract.KEY_TITLE, title)
                putString(XIpcContract.KEY_CONTENT, content)
                putString(XIpcContract.KEY_URI, uri)
            }
        )
        return bundle?.getBoolean(XIpcContract.KEY_SUCCESS) == true
    }

    private fun callProvider(
        context: Context,
        method: String,
        extras: Bundle? = null
    ): Bundle? {
        return context.contentResolver.call(
            XIpcContract.CONTENT_URI,
            method,
            null,
            extras
        )
    }
}
