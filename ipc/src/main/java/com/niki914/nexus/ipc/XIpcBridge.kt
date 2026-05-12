package com.niki914.nexus.ipc

import android.content.Context
import android.os.Bundle
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * `XIpcBridge` 是 `:ipc` 对外暴露的统一桥接入口，用来把“同一套能力在两个进程里运行”
 * 这件事压平到调用方几乎无感。
 *
 * 当前工程里，同一份业务代码会同时运行在两个 App / 进程中：
 * - Nexus 主 App 进程：真正拥有配置持久化能力和通知发送能力
 * - 宿主进程（例如被 Hook 的目标 App）：只能借助 `ContentProvider` 把请求转发回 Nexus
 *
 * 这里最容易让后人误解的一点是：宿主进程虽然也能拿到一个 `Context`，但这个 `Context`
 * 绝不能直接承担配置持久化和通知发送的最终执行职责。宿主 `Context` 在这里的唯一用途，
 * 是作为 IPC 发起端调用 `SettingsContentProvider`。真正的持久化和通知，最终都必须落到
 * Nexus 进程自己的 `Context` 上执行。
 *
 * 因此本类做的事情很单纯：
 * - 如果传入的是宿主 `Context`，就走 `ContentResolver.call()` 把请求转发给 Nexus
 * - 如果传入的是 Nexus `Context`，就直接调用本地 helper 完成持久化或通知
 *
 * 这样调用方只需要记住一条规则：始终把“当前手里的 `Context`”传进来即可，不需要自己关心
 * 现在是不是宿主进程、是否要走 provider、最终是谁在执行落盘或发通知。
 *
 * 额外约束：
 * - 本类只负责“路由”和少量 JSON 级别的读改写，不负责业务模型转换
 * - `XService` 之类的高层 facade 应该依赖本类，而不是直接碰 `ContentResolver.call()`
 * - `SettingsContentProvider` 仍然只是 IPC 边界；它负责分发，真正执行仍然下沉到 helper
 *
 * 设计意图是把“宿主多绕一跳、主 App 直接执行”统一成同一个 API 面，从而让跨进程和单进程
 * 的代码路径在语义上保持一致。
 */
object XIpcBridge {

    @Volatile
    private var cachedWebSettingsJson: String? = null
    private val webSettingsCacheMutex = Mutex()

    fun readWebSettingsJson(context: Context): String {
        return readJson(context, IpcContract.Store.WEB_SETTINGS)
    }

    fun writeWebSettingsJson(context: Context, json: String) {
        writeJson(context, IpcContract.Store.WEB_SETTINGS, json)
    }

    fun writeWebSetting(context: Context, key: String, value: Any?) {
        mutateSetting(context, IpcContract.Store.WEB_SETTINGS, key, value)
    }

    fun readLocalSettingsJson(context: Context): String {
        return readJson(context, IpcContract.Store.LOCAL_SETTINGS)
    }

    fun writeLocalSettingsJson(context: Context, json: String) {
        writeJson(context, IpcContract.Store.LOCAL_SETTINGS, json)
    }

    fun writeLocalSetting(context: Context, key: String, value: Any?) {
        mutateSetting(context, IpcContract.Store.LOCAL_SETTINGS, key, value)
    }

    fun readJson(
        context: Context,
        store: IpcContract.Store
    ): String {
        if (store == IpcContract.Store.WEB_SETTINGS) {
            cachedWebSettingsJson?.let { return it }
            return runBlocking {
                webSettingsCacheMutex.withLock {
                    cachedWebSettingsJson ?: readStoreJsonUncached(context, store).also {
                        cachedWebSettingsJson = it
                    }
                }
            }
        }
        return readStoreJsonUncached(context, store)
    }

    fun writeJson(
        context: Context,
        store: IpcContract.Store,
        json: String
    ) {
        if (isHostContext(context)) {
            writeJsonViaProvider(
                context = context,
                store = store,
                json = json
            )
        } else {
            runBlocking {
                XIpcStoreRepository.writeJson(context, store, json)
            }
        }
        updateWebCacheIfNeeded(store, json)
    }

    fun mutateSetting(
        context: Context,
        store: IpcContract.Store,
        key: String,
        value: Any?
    ): String {
        val updatedJson = if (isHostContext(context)) {
            mutateJsonViaProvider(
                context = context,
                store = store,
                key = key,
                valueJson = serializeValue(value)
            )
        } else {
            runBlocking {
                XIpcStoreRepository.mutateJson(
                    context = context,
                    store = store,
                    path = key,
                    valueJson = serializeValue(value)
                )
            }
        }
        updateWebCacheIfNeeded(store, updatedJson)
        return updatedJson
    }

    fun postNotification(
        context: Context,
        title: String,
        content: String,
        uri: String?
    ): Boolean {
        return if (isHostContext(context)) {
            val bundle = callProvider(
                context = context,
                method = IpcContract.Method.POST_NOTIFICATION,
                extras = ipcBundleOf(
                    IpcContract.Field.TITLE to title,
                    IpcContract.Field.CONTENT to content,
                    IpcContract.Field.URI to uri
                )
            )
            bundle?.readBoolean(IpcContract.Field.SUCCESS) == true
        } else {
            XNotificationBridge.post(context, title, content, uri)
            true
        }
    }

    private fun isHostContext(context: Context): Boolean {
        return context.packageName in XValues.appList
    }

    private fun readStoreJsonUncached(
        context: Context,
        store: IpcContract.Store
    ): String {
        return if (isHostContext(context)) {
            readJsonViaProvider(context, store)
        } else {
            runBlocking {
                XIpcStoreRepository.readJson(context, store)
            }
        }
    }

    private fun readJsonViaProvider(
        context: Context,
        store: IpcContract.Store
    ): String {
        return callProvider(
            context = context,
            method = store.readMethod
        )?.let { bundle ->
            bundle.readString(store.payloadField)
                ?: store.legacyPayloadField?.let { field ->
                    bundle.readString(field)
                }
        } ?: EMPTY_JSON
    }

    private fun writeJsonViaProvider(
        context: Context,
        store: IpcContract.Store,
        json: String
    ) {
        callProvider(
            context = context,
            method = store.writeMethod,
            extras = ipcBundleOf(store.payloadField to json)
        )
    }

    private fun mutateJsonViaProvider(
        context: Context,
        store: IpcContract.Store,
        key: String,
        valueJson: String
    ): String {
        return callProvider(
            context = context,
            method = store.mutateMethod,
            extras = ipcBundleOf(
                IpcContract.Field.PATH to key,
                IpcContract.Field.VALUE_JSON to valueJson
            )
        )?.let { bundle ->
            bundle.readString(store.payloadField)
                ?: store.legacyPayloadField?.let { field ->
                    bundle.readString(field)
                }
        } ?: readJsonViaProvider(context, store)
    }

    private fun callProvider(
        context: Context,
        method: IpcContract.Method,
        extras: Bundle? = null
    ): Bundle? {
        return context.contentResolver.call(
            IpcContract.CONTENT_URI,
            method.wireName,
            null,
            extras
        )
    }

    private fun updateWebCacheIfNeeded(
        store: IpcContract.Store,
        json: String
    ) {
        if (store == IpcContract.Store.WEB_SETTINGS) {
            cachedWebSettingsJson = json
        }
    }

    private fun serializeValue(value: Any?): String {
        return when (val jsonValue = toJsonValue(value)) {
            JSONObject.NULL -> "null"
            is String -> JSONObject.quote(jsonValue)
            else -> jsonValue.toString()
        }
    }

    private const val EMPTY_JSON = "{}"

    private fun toJsonValue(value: Any?): Any {
        return when (value) {
            null -> JSONObject.NULL
            is JSONObject -> value
            is JSONArray -> value
            is Boolean, is Number, is String -> value
            is Map<*, *> -> JSONObject().apply {
                value.forEach { (key, nestedValue) ->
                    if (key != null) {
                        put(key.toString(), toJsonValue(nestedValue))
                    }
                }
            }
            is Iterable<*> -> JSONArray().apply {
                value.forEach { put(toJsonValue(it)) }
            }
            is Array<*> -> JSONArray().apply {
                value.forEach { put(toJsonValue(it)) }
            }
            else -> value.toString()
        }
    }
}
