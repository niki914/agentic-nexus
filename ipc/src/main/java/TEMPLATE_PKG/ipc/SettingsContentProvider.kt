package ${BASE_PACKAGE}.ipc

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle

class SettingsContentProvider : ContentProvider() {
    companion object {
        const val METHOD_GET_CONFIG = "get_config"
        const val KEY_CONFIG_JSON = "config_json"
    }

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method == METHOD_GET_CONFIG) {
            val json = ConfigPersistence.readConfig(context ?: return null)
            return Bundle().apply {
                putString(KEY_CONFIG_JSON, json)
            }
        }
        return super.call(method, arg, extras)
    }

    override fun query(uri: Uri, p1: Array<out String>?, p2: String?, p3: Array<out String>?, p4: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
