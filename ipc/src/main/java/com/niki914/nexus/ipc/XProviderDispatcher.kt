package com.niki914.nexus.ipc

import android.content.Context
import android.os.Bundle

internal object XProviderDispatcher {

    fun dispatch(
        context: Context,
        method: String,
        extras: Bundle?
    ): Bundle? {
        return when (method) {
            XIpcContract.METHOD_GET_CONFIG,
            XIpcContract.METHOD_GET_WEB_SETTINGS -> bundleOf(
                XIpcContract.KEY_CONFIG_JSON to ConfigPersistence.readWebSettings(context),
                XIpcContract.KEY_WEB_SETTINGS_JSON to ConfigPersistence.readWebSettings(context)
            )

            XIpcContract.METHOD_PUT_WEB_SETTINGS -> {
                val json = extras?.getString(XIpcContract.KEY_WEB_SETTINGS_JSON)
                    ?: extras?.getString(XIpcContract.KEY_CONFIG_JSON)
                    ?: return null
                ConfigPersistence.writeWebSettings(context, json)
                bundleOf(
                    XIpcContract.KEY_SUCCESS to true,
                    XIpcContract.KEY_WEB_SETTINGS_JSON to json,
                    XIpcContract.KEY_CONFIG_JSON to json
                )
            }

            XIpcContract.METHOD_GET_LOCAL_SETTINGS -> bundleOf(
                XIpcContract.KEY_LOCAL_SETTINGS_JSON to ConfigPersistence.readLocalSettings(context)
            )

            XIpcContract.METHOD_PUT_LOCAL_SETTINGS -> {
                val json = extras?.getString(XIpcContract.KEY_LOCAL_SETTINGS_JSON)
                    ?: return null
                ConfigPersistence.writeLocalSettings(context, json)
                bundleOf(
                    XIpcContract.KEY_SUCCESS to true,
                    XIpcContract.KEY_LOCAL_SETTINGS_JSON to json
                )
            }

            XIpcContract.METHOD_POST_NOTIFICATION -> {
                val title = extras?.getString(XIpcContract.KEY_TITLE).orEmpty()
                val content = extras?.getString(XIpcContract.KEY_CONTENT).orEmpty()
                val uri = extras?.getString(XIpcContract.KEY_URI)
                XNotificationBridge.post(context, title, content, uri)
                bundleOf(XIpcContract.KEY_SUCCESS to true)
            }

            else -> null
        }
    }

    private fun bundleOf(vararg pairs: Pair<String, Any?>): Bundle {
        return Bundle().apply {
            pairs.forEach { (key, value) ->
                when (value) {
                    null -> putString(key, null)
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> error("Unsupported bundle type for key=$key")
                }
            }
        }
    }
}