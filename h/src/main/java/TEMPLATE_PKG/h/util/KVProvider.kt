package ${BASE_PACKAGE}.h.util

import kotlinx.serialization.json.JsonElement

/**
 * Remote settings!
 */
object KVProvider : XProvider<Map<String, JsonElement>>() {
    suspend inline fun <reified T> get(key: String): T? {
        return await()[key] as? T
    }
}