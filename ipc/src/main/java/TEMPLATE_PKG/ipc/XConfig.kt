package ${BASE_PACKAGE}.ipc

import android.content.Context
import android.net.Uri

object XConfig {
    private const val AUTHORITY = "${BASE_PACKAGE}.ipc.provider"
    private val CONTENT_URI = Uri.parse("content://$AUTHORITY")

    @Volatile
    private var cachedJson: String? = null

    fun updateFromServerJson(context: Context, jsonString: String) {
        ConfigPersistence.writeConfig(context, jsonString)
        cachedJson = jsonString
    }

    fun get(context: Context): String {
        cachedJson?.let { return it }

        synchronized(this) {
            cachedJson?.let { return it }

            val bundle = context.contentResolver.call(
                CONTENT_URI,
                SettingsContentProvider.METHOD_GET_CONFIG,
                null,
                null
            )
            val json = bundle?.getString(SettingsContentProvider.KEY_CONFIG_JSON)

            val resolved = json ?: "{}"
            cachedJson = resolved
            return resolved
        }
    }
}
